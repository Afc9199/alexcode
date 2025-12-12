package com.example.employeemanagement.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.employeemanagement.dto.AttendanceResponse;
import com.example.employeemanagement.dto.CompanyLeaveSettingsResponse;
import com.example.employeemanagement.dto.CompanySettingsResponse;
import com.example.employeemanagement.dto.EmployeeBenefitResponse;
import com.example.employeemanagement.dto.EmployeeKPIResponse;
import com.example.employeemanagement.dto.LeaveResponse;
import com.example.employeemanagement.dto.OvertimeResponse;
import com.example.employeemanagement.dto.ProfileResponse;
import com.example.employeemanagement.model.AttendanceStatus;
import com.example.employeemanagement.model.LeaveStatus;
import com.example.employeemanagement.model.OvertimeStatus;
import com.example.employeemanagement.model.PayrollRecord;
import com.example.employeemanagement.util.SocsoTableGenerator;
import com.example.employeemanagement.util.TaxCalculator;

/**
 * Service for calculating monthly payroll with Malaysian statutory deductions
 */
@Service
public class PayrollCalculationService {

	private final UserProfileService userProfileService;
	private final CompanySettingsService companySettingsService;
	private final CompanyLeaveSettingsService companyLeaveSettingsService;
	private final AttendanceService attendanceService;
	private final LeaveService leaveService;
	private final OvertimeService overtimeService;
	private final EmployeeKPIService employeeKPIService;
	private final EmployeeBenefitService employeeBenefitService;

	public PayrollCalculationService(
			UserProfileService userProfileService,
			CompanySettingsService companySettingsService,
			CompanyLeaveSettingsService companyLeaveSettingsService,
			AttendanceService attendanceService,
			LeaveService leaveService,
			OvertimeService overtimeService,
			EmployeeKPIService employeeKPIService,
			EmployeeBenefitService employeeBenefitService) {
		this.userProfileService = userProfileService;
		this.companySettingsService = companySettingsService;
		this.companyLeaveSettingsService = companyLeaveSettingsService;
		this.attendanceService = attendanceService;
		this.leaveService = leaveService;
		this.overtimeService = overtimeService;
		this.employeeKPIService = employeeKPIService;
		this.employeeBenefitService = employeeBenefitService;
	}

	/**
	 * Calculate payroll for an employee for a specific month
	 */
	public PayrollRecord calculatePayroll(String userId, YearMonth month) {
		// Step 1: Get employee profile
		ProfileResponse profile = userProfileService.getProfile(userId);
		if (profile == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee profile not found");
		}

		// Step 2: Get company settings
		CompanySettingsResponse companySettings = companySettingsService.getSettings();
		CompanyLeaveSettingsResponse leaveSettings = companyLeaveSettingsService.getSettings();

		// Step 3: Get month boundaries
		LocalDate monthStart = month.atDay(1);
		LocalDate monthEnd = month.atEndOfMonth();

		// Step 4: Calculate theoretical working days (for reference)
		int theoreticalWorkingDays = calculateWorkingDays(month, companySettings.workingDays());

		// Step 4.5: Calculate actual present days (only days with attendance record and status PRESENT/LATE/REMOTE)
		int actualPresentDays = getActualPresentDays(userId, monthStart, monthEnd, companySettings.workingDays());

		// Step 5: Get unpaid leave days (for reference only, not used in salary calculation)
		int unpaidLeaveDays = getUnpaidLeaveDays(userId, monthStart, monthEnd);

		// Step 5.5: Get absent days (for reference only, not used in salary calculation)
		int absentDays = getAbsentDays(userId, monthStart, monthEnd);

		// Step 6: Calculate adjusted basic salary based on actual present days only
		// Only days with attendance record and status PRESENT/LATE/REMOTE count for salary
		// Formula: adjustedBasicSalary = basicSalary * (actualPresentDays / theoreticalWorkingDays)
		BigDecimal basicSalary = BigDecimal.valueOf(profile.basicSalary() != null ? profile.basicSalary() : 0.0);
		BigDecimal adjustedBasicSalary = calculateAdjustedBasicSalaryByPresentDays(basicSalary, actualPresentDays, theoreticalWorkingDays);

		// Step 7: Calculate overtime
		OvertimeCalculation otCalc = calculateOvertime(userId, monthStart, monthEnd, companySettings,
				leaveSettings.publicHolidays());

		// Step 8: Get KPI bonus (only completed KPIs)
		BigDecimal kpiBonus = getKpiBonus(userId, monthStart, monthEnd);

		// Step 9: Get benefit bonus
		BigDecimal benefitBonus = getBenefitBonus(userId);

		// Step 10: Calculate income classifications
		BigDecimal epfWages = adjustedBasicSalary.add(otCalc.totalOtPay()).add(kpiBonus).add(benefitBonus);
		BigDecimal socsoWages = adjustedBasicSalary.add(kpiBonus).add(benefitBonus); // Excludes OT
		if (socsoWages.compareTo(BigDecimal.valueOf(6000)) > 0) {
			socsoWages = BigDecimal.valueOf(6000); // Cap at RM 6,000
		}
		BigDecimal taxableIncome = adjustedBasicSalary.add(otCalc.totalOtPay()).add(kpiBonus).add(benefitBonus);

		// Step 11: Calculate statutory deductions
		StatutoryDeductions deductions = calculateStatutoryDeductions(
				basicSalary,
				epfWages.doubleValue(),
				socsoWages.doubleValue(),
				taxableIncome.doubleValue(),
				profile);

		// Step 12: Calculate gross pay and net pay
		BigDecimal grossPay = adjustedBasicSalary
				.add(otCalc.totalOtPay())
				.add(kpiBonus)
				.add(benefitBonus);

		BigDecimal totalEmployeeDeductions = deductions.epfEmployee()
				.add(deductions.socsoEmployee())
				.add(deductions.eisEmployee())
				.add(deductions.pcb());

		BigDecimal netPay = grossPay.subtract(totalEmployeeDeductions);

		// Step 13: Calculate employer contributions
		BigDecimal totalEmployerContributions = deductions.epfEmployer()
				.add(deductions.socsoEmployer())
				.add(deductions.eisEmployer());

		BigDecimal employerCost = grossPay.add(totalEmployerContributions);

		// Step 14: Create and populate PayrollRecord
		PayrollRecord record = new PayrollRecord();
		record.setUserId(userId);
		record.setEmployeeId(profile.employeeId());
		record.setSalaryMonth(month);
		record.setPeriodStart(monthStart);
		record.setPeriodEnd(monthEnd);

		// Earnings
		record.setBasicSalary(basicSalary);
		record.setAdjustedBasicSalary(adjustedBasicSalary);
		record.setOtPayNormal(otCalc.otPayNormal());
		record.setOtPayOffDay(otCalc.otPayOffDay());
		record.setOtPayPublicHoliday(otCalc.otPayPublicHoliday());
		record.setTotalOtPay(otCalc.totalOtPay());
		record.setKpiBonus(kpiBonus);
		record.setBenefitBonus(benefitBonus);
		record.setGrossPay(grossPay);

		// Deductions
		record.setEpfEmployee(deductions.epfEmployee());
		record.setEpfEmployer(deductions.epfEmployer());
		record.setSocsoEmployee(deductions.socsoEmployee());
		record.setSocsoEmployer(deductions.socsoEmployer());
		record.setEisEmployee(deductions.eisEmployee());
		record.setEisEmployer(deductions.eisEmployer());
		record.setPcb(deductions.pcb());
		record.setTotalEmployeeDeductions(totalEmployeeDeductions);
		record.setTotalEmployerContributions(totalEmployerContributions);

		// Net pay and employer cost
		record.setNetPay(netPay);
		record.setEmployerCost(employerCost);

		// Attendance summary
		record.setTotalWorkingDays(actualPresentDays); // Store actual present days instead of theoretical working days
		record.setOtHoursNormal(otCalc.otHoursNormal());
		record.setOtHoursOffDay(otCalc.otHoursOffDay());
		record.setOtHoursPublicHoliday(otCalc.otHoursPublicHoliday());
		record.setUnpaidLeaveDays(unpaidLeaveDays);
		record.setAbsentDays(absentDays);

		// Legacy fields (for backward compatibility)
		record.setBaseSalary(basicSalary);
		record.setBonus(kpiBonus.add(benefitBonus));
		record.setDeductions(totalEmployeeDeductions);

		return record;
	}

	/**
	 * Calculate total working days in a month based on operational days
	 * (Kept for reference, but not used in salary calculation anymore)
	 */
	private int calculateWorkingDays(YearMonth month, List<String> workingDays) {
		Set<DayOfWeek> workingDaySet = workingDays.stream()
				.map(day -> DayOfWeek.valueOf(day.toUpperCase()))
				.collect(Collectors.toSet());

		LocalDate start = month.atDay(1);
		LocalDate end = month.atEndOfMonth();

		int count = 0;
		LocalDate current = start;
		while (!current.isAfter(end)) {
			if (workingDaySet.contains(current.getDayOfWeek())) {
				count++;
			}
			current = current.plusDays(1);
		}
		return count;
	}

	/**
	 * Get actual present days - only days with attendance record and status PRESENT/LATE/REMOTE
	 * Only these days count for salary calculation
	 */
	private int getActualPresentDays(String userId, LocalDate monthStart, LocalDate monthEnd, List<String> workingDays) {
		Set<DayOfWeek> workingDaySet = workingDays.stream()
				.map(day -> DayOfWeek.valueOf(day.toUpperCase()))
				.collect(Collectors.toSet());

		// Get all attendance records for the month
		List<AttendanceResponse> attendanceRecords = attendanceService.findForUser(userId);
		
		// Count only days with attendance record and status PRESENT, LATE, or REMOTE
		// ABSENT days and days without attendance record are not counted
		return attendanceRecords.stream()
				.filter(att -> att.workDate() != null)
				.filter(att -> !att.workDate().isBefore(monthStart) && !att.workDate().isAfter(monthEnd))
				.filter(att -> workingDaySet.contains(att.workDate().getDayOfWeek())) // Only count working days
				.filter(att -> att.status() != null && (
					att.status() == AttendanceStatus.PRESENT ||
					att.status() == AttendanceStatus.LATE ||
					att.status() == AttendanceStatus.REMOTE
				)) // Only count PRESENT, LATE, or REMOTE status
				.mapToInt(att -> 1)
				.sum();
	}

	/**
	 * Get count of absent days for the month (only working days)
	 * Absent days are days marked as ABSENT in attendance records
	 */
	private int getAbsentDays(String userId, LocalDate monthStart, LocalDate monthEnd) {
		// Get company settings for working days
		CompanySettingsResponse companySettings = companySettingsService.getSettings();
		Set<DayOfWeek> workingDaySet = companySettings.workingDays().stream()
				.map(day -> DayOfWeek.valueOf(day.toUpperCase()))
				.collect(Collectors.toSet());

		// Get attendance records for the month
		List<AttendanceResponse> attendanceRecords = attendanceService.findForUser(userId);
		
		return attendanceRecords.stream()
				.filter(att -> att.workDate() != null)
				.filter(att -> !att.workDate().isBefore(monthStart) && !att.workDate().isAfter(monthEnd))
				.filter(att -> att.status() == AttendanceStatus.ABSENT)
				.filter(att -> workingDaySet.contains(att.workDate().getDayOfWeek())) // Only count working days
				.mapToInt(att -> 1)
				.sum();
	}

	/**
	 * Get count of unpaid leave days for the month (only working days)
	 */
	private int getUnpaidLeaveDays(String userId, LocalDate monthStart, LocalDate monthEnd) {
		// Get company settings for working days
		CompanySettingsResponse companySettings = companySettingsService.getSettings();
		Set<DayOfWeek> workingDaySet = companySettings.workingDays().stream()
				.map(day -> DayOfWeek.valueOf(day.toUpperCase()))
				.collect(Collectors.toSet());

		List<LeaveResponse> leaves = leaveService.findForUser(userId);
		return leaves.stream()
				.filter(leave -> leave.status() == LeaveStatus.APPROVED)
				.filter(leave -> "Unpaid Leave".equals(leave.leaveType()))
				.mapToInt(leave -> {
					LocalDate leaveStart = leave.startDate();
					LocalDate leaveEnd = leave.endDate();
					
					// Calculate overlap with the month
					LocalDate overlapStart = leaveStart.isBefore(monthStart) ? monthStart : leaveStart;
					LocalDate overlapEnd = leaveEnd.isAfter(monthEnd) ? monthEnd : leaveEnd;
					
					if (overlapStart.isAfter(overlapEnd)) {
						return 0;
					}
					
					// Count only working days in the overlap
					int count = 0;
					LocalDate current = overlapStart;
					while (!current.isAfter(overlapEnd)) {
						if (workingDaySet.contains(current.getDayOfWeek())) {
							count++;
						}
						current = current.plusDays(1);
					}
					return count;
				})
				.sum();
	}

	/**
	 * Calculate adjusted basic salary after deduction for unpaid leave and absent days
	 * 
	 * Algorithm:
	 * 1. Calculate daily rate = basicSalary / totalWorkingDays
	 * 2. Calculate total deduction = dailyRate × totalDeductionDays (unpaid leave + absent)
	 * 3. Adjusted salary = basicSalary - totalDeduction (minimum 0)
	 */
	private BigDecimal calculateAdjustedBasicSalary(BigDecimal basicSalary, int totalWorkingDays, int totalDeductionDays) {
		if (totalWorkingDays == 0) {
			return BigDecimal.ZERO;
		}
		// Step 1: Calculate daily rate (rounded to 2 decimal places)
		BigDecimal dailyRate = basicSalary.divide(BigDecimal.valueOf(totalWorkingDays), 2, RoundingMode.HALF_UP);
		
		// Step 2: Calculate total deduction amount
		BigDecimal deduction = dailyRate.multiply(BigDecimal.valueOf(totalDeductionDays));
		
		// Step 3: Subtract deduction from basic salary (ensure it doesn't go below 0)
		return basicSalary.subtract(deduction).max(BigDecimal.ZERO);
	}

	/**
	 * Calculate adjusted basic salary based on actual present days only
	 * Only days with attendance record and status PRESENT/LATE/REMOTE count for salary
	 * 
	 * Algorithm:
	 * 1. If theoreticalWorkingDays is 0, return 0
	 * 2. If actualPresentDays is 0, return 0 (no attendance = no salary)
	 * 3. Calculate adjusted salary = basicSalary * (actualPresentDays / theoreticalWorkingDays)
	 */
	private BigDecimal calculateAdjustedBasicSalaryByPresentDays(BigDecimal basicSalary, int actualPresentDays, int theoreticalWorkingDays) {
		if (theoreticalWorkingDays == 0) {
			return BigDecimal.ZERO;
		}
		if (actualPresentDays == 0) {
			return BigDecimal.ZERO; // No attendance record with PRESENT/LATE/REMOTE = no salary
		}
		// Calculate proportional salary based on actual present days
		BigDecimal ratio = BigDecimal.valueOf(actualPresentDays)
				.divide(BigDecimal.valueOf(theoreticalWorkingDays), 4, RoundingMode.HALF_UP);
		return basicSalary.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
	}

	/**
	 * Calculate overtime pay - only from approved overtime requests
	 */
	private OvertimeCalculation calculateOvertime(String userId, LocalDate monthStart, LocalDate monthEnd,
			CompanySettingsResponse companySettings, List<CompanyLeaveSettingsResponse.PublicHolidayDto> publicHolidays) {
		
		Set<LocalDate> publicHolidayDates = publicHolidays.stream()
				.map(CompanyLeaveSettingsResponse.PublicHolidayDto::date)
				.collect(Collectors.toSet());

		// Get approved overtime requests for the month
		List<OvertimeResponse> approvedOvertimeRequests = overtimeService
				.findByUserIdAndStatusAndWorkDateBetween(userId, OvertimeStatus.APPROVED, monthStart, monthEnd);
		
		// Debug logging
		System.out.println("=== Overtime Calculation Debug ===");
		System.out.println("UserId: " + userId);
		System.out.println("Date Range: " + monthStart + " to " + monthEnd);
		System.out.println("Approved Overtime Requests Found: " + approvedOvertimeRequests.size());
		for (OvertimeResponse ot : approvedOvertimeRequests) {
			System.out.println("  - OT ID: " + ot.overtimeId() + ", Date: " + ot.workDate() + 
					", Hours: " + ot.hours() + ", Start: " + ot.startTime() + ", End: " + ot.endTime());
		}

		// Get employee profile for basic salary
		ProfileResponse profile = userProfileService.getProfile(userId);
		BigDecimal basicSalary = BigDecimal.valueOf(profile.basicSalary() != null ? profile.basicSalary() : 0.0);

		// Calculate hourly rate using fixed divisor of 26
		BigDecimal hourlyRate = basicSalary
				.divide(BigDecimal.valueOf(26), 4, RoundingMode.HALF_UP)
				.divide(BigDecimal.valueOf(8), 4, RoundingMode.HALF_UP);

		Set<DayOfWeek> workingDays = companySettings.workingDays().stream()
				.map(day -> DayOfWeek.valueOf(day.toUpperCase()))
				.collect(Collectors.toSet());

		double otHoursNormal = 0.0;
		double otHoursOffDay = 0.0;
		double otHoursPublicHoliday = 0.0;

		// Calculate overtime from approved requests only
		for (OvertimeResponse otRequest : approvedOvertimeRequests) {
			LocalDate workDate = otRequest.workDate();
			LocalTime startTime = otRequest.startTime();
			LocalTime endTime = otRequest.endTime();
			
			// Calculate hours from startTime and endTime if hours field is null or invalid
			double otHours = 0.0;
			if (otRequest.hours() != null && otRequest.hours() > 0) {
				otHours = otRequest.hours();
				System.out.println("  Using hours field: " + otHours);
			} else if (startTime != null && endTime != null) {
				// Calculate hours from time difference
				Duration duration = Duration.between(startTime, endTime);
				if (duration.isNegative()) {
					// Handle overnight overtime (e.g., 22:00 to 02:00)
					duration = duration.plusDays(1);
				}
				otHours = duration.toMinutes() / 60.0;
				System.out.println("  Calculated hours from time: " + otHours + " (from " + startTime + " to " + endTime + ")");
			} else {
				System.out.println("  WARNING: No valid hours or time data for OT ID: " + otRequest.overtimeId());
			}
			
			if (otHours <= 0) {
				System.out.println("  Skipping OT ID: " + otRequest.overtimeId() + " (hours <= 0)");
				continue; // Skip if no valid hours
			}
			
			if (publicHolidayDates.contains(workDate)) {
				// Public holiday: 3.0x multiplier
				otHoursPublicHoliday += otHours;
				System.out.println("  Added to Public Holiday OT: " + otHours + " hours");
			} else if (!workingDays.contains(workDate.getDayOfWeek())) {
				// Off day: 2.0x multiplier
				otHoursOffDay += otHours;
				System.out.println("  Added to Off Day OT: " + otHours + " hours");
			} else {
				// Normal work day: 1.5x multiplier
				otHoursNormal += otHours;
				System.out.println("  Added to Normal Day OT: " + otHours + " hours");
			}
		}
		
		System.out.println("Total OT Hours - Normal: " + otHoursNormal + ", Off Day: " + otHoursOffDay + ", Public Holiday: " + otHoursPublicHoliday);

		BigDecimal otPayNormal = hourlyRate
				.multiply(BigDecimal.valueOf(otHoursNormal))
				.multiply(BigDecimal.valueOf(1.5))
				.setScale(2, RoundingMode.HALF_UP);

		BigDecimal otPayOffDay = hourlyRate
				.multiply(BigDecimal.valueOf(otHoursOffDay))
				.multiply(BigDecimal.valueOf(2.0))
				.setScale(2, RoundingMode.HALF_UP);

		BigDecimal otPayPublicHoliday = hourlyRate
				.multiply(BigDecimal.valueOf(otHoursPublicHoliday))
				.multiply(BigDecimal.valueOf(3.0))
				.setScale(2, RoundingMode.HALF_UP);

		BigDecimal totalOtPay = otPayNormal.add(otPayOffDay).add(otPayPublicHoliday);
		
		System.out.println("OT Pay - Normal: " + otPayNormal + ", Off Day: " + otPayOffDay + ", Public Holiday: " + otPayPublicHoliday);
		System.out.println("Total OT Pay: " + totalOtPay);
		System.out.println("=== End Overtime Calculation Debug ===");

		return new OvertimeCalculation(otHoursNormal, otHoursOffDay, otHoursPublicHoliday,
				otPayNormal, otPayOffDay, otPayPublicHoliday, totalOtPay);
	}

	/**
	 * Get KPI bonus for completed KPIs in the month
	 */
	private BigDecimal getKpiBonus(String userId, LocalDate monthStart, LocalDate monthEnd) {
		List<EmployeeKPIResponse> kpis = employeeKPIService.getKPIsByUserId(userId);
		return kpis.stream()
				.filter(kpi -> "COMPLETED".equalsIgnoreCase(kpi.status()))
				.filter(kpi -> kpi.dueDate() != null)
				.filter(kpi -> !kpi.dueDate().isBefore(monthStart) && !kpi.dueDate().isAfter(monthEnd))
				.map(kpi -> BigDecimal.valueOf(kpi.bonusAmount() != null ? kpi.bonusAmount() : 0.0))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	/**
	 * Get benefit bonus (all assigned benefits)
	 */
	private BigDecimal getBenefitBonus(String userId) {
		List<EmployeeBenefitResponse> benefits = employeeBenefitService.getBenefitsByUserId(userId);
		return benefits.stream()
				.map(benefit -> BigDecimal.valueOf(benefit.benefitAmount() != null ? benefit.benefitAmount() : 0.0))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	/**
	 * Calculate all statutory deductions
	 */
	private StatutoryDeductions calculateStatutoryDeductions(
			BigDecimal basicSalary,
			double epfWages,
			double socsoWages, // MUST EXCLUDE Overtime
			double taxableIncome,
			ProfileResponse profile) {

		// EPF Calculation
		double epfEmployeeRate = 0.11; // 11% default
		double epfEmployerRate = basicSalary.compareTo(BigDecimal.valueOf(5000)) <= 0 ? 0.13 : 0.12;

		BigDecimal epfEmployee = BigDecimal.valueOf(epfWages * epfEmployeeRate)
				.setScale(2, RoundingMode.HALF_UP);
		BigDecimal epfEmployer = BigDecimal.valueOf(epfWages * epfEmployerRate)
				.setScale(2, RoundingMode.HALF_UP);

		// Round up to nearest Ringgit
		epfEmployee = BigDecimal.valueOf(TaxCalculator.roundUpToRinggit(epfEmployee.doubleValue()));
		epfEmployer = BigDecimal.valueOf(TaxCalculator.roundUpToRinggit(epfEmployer.doubleValue()));

		// SOCSO and EIS Calculation
		int age = profile.age() != null ? profile.age() : 0;
		SocsoTableGenerator.SocsoEisResult socsoEis = SocsoTableGenerator.calculateSocsoAndEis(socsoWages, age);

		BigDecimal socsoEmployee = BigDecimal.valueOf(socsoEis.getSocsoEmployee())
				.setScale(2, RoundingMode.HALF_UP);
		BigDecimal socsoEmployer = BigDecimal.valueOf(socsoEis.getSocsoEmployer())
				.setScale(2, RoundingMode.HALF_UP);
		BigDecimal eisEmployee = BigDecimal.valueOf(socsoEis.getEisEmployee())
				.setScale(2, RoundingMode.HALF_UP);
		BigDecimal eisEmployer = BigDecimal.valueOf(socsoEis.getEisEmployer())
				.setScale(2, RoundingMode.HALF_UP);

		// PCB Calculation
		boolean isResident = "Resident".equalsIgnoreCase(profile.residentStatus());
		int numberOfChildren = profile.numberOfChildren() != null ? profile.numberOfChildren() : 0;
		boolean spouseWorking = "Yes".equalsIgnoreCase(profile.spouseWorking());
		double annualEpfContribution = epfEmployee.doubleValue() * 12;

		double totalReliefs = TaxCalculator.calculateTotalReliefs(numberOfChildren, spouseWorking, annualEpfContribution);
		double pcbAmount = TaxCalculator.calculateMonthlyPCB(taxableIncome, isResident, totalReliefs);
		BigDecimal pcb = BigDecimal.valueOf(pcbAmount).setScale(2, RoundingMode.HALF_UP);

		return new StatutoryDeductions(epfEmployee, epfEmployer, socsoEmployee, socsoEmployer,
				eisEmployee, eisEmployer, pcb);
	}

	// Helper record classes
	private record OvertimeCalculation(
			double otHoursNormal,
			double otHoursOffDay,
			double otHoursPublicHoliday,
			BigDecimal otPayNormal,
			BigDecimal otPayOffDay,
			BigDecimal otPayPublicHoliday,
			BigDecimal totalOtPay) {
	}

	private record StatutoryDeductions(
			BigDecimal epfEmployee,
			BigDecimal epfEmployer,
			BigDecimal socsoEmployee,
			BigDecimal socsoEmployer,
			BigDecimal eisEmployee,
			BigDecimal eisEmployer,
			BigDecimal pcb) {
	}
}

