package com.example.employeemanagement.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.employeemanagement.dto.AttendanceReportResponse;
import com.example.employeemanagement.dto.BenefitsReportResponse;
import com.example.employeemanagement.dto.CompanySettingsResponse;
import com.example.employeemanagement.dto.KPIPerformanceReportResponse;
import com.example.employeemanagement.dto.PayrollReportResponse;
import com.example.employeemanagement.model.AttendanceRecord;
import com.example.employeemanagement.model.EmployeeBenefit;
import com.example.employeemanagement.model.EmployeeKPI;
import com.example.employeemanagement.model.PayrollRecord;
import com.example.employeemanagement.model.UserAccount;
import com.example.employeemanagement.repository.AttendanceRepository;
import com.example.employeemanagement.repository.EmployeeBenefitRepository;
import com.example.employeemanagement.repository.EmployeeKPIRepository;
import com.example.employeemanagement.repository.PayrollRepository;
import com.example.employeemanagement.repository.UserAccountRepository;
import com.example.employeemanagement.service.CompanySettingsService;

@Service
public class ReportService {

	private final AttendanceRepository attendanceRepository;
	private final UserAccountRepository userAccountRepository;
	private final PayrollRepository payrollRepository;
	private final EmployeeBenefitRepository employeeBenefitRepository;
	private final EmployeeKPIRepository employeeKPIRepository;
	private final CompanySettingsService companySettingsService;

	public ReportService(
			AttendanceRepository attendanceRepository,
			UserAccountRepository userAccountRepository,
			PayrollRepository payrollRepository,
			EmployeeBenefitRepository employeeBenefitRepository,
			EmployeeKPIRepository employeeKPIRepository,
			CompanySettingsService companySettingsService) {
		this.attendanceRepository = attendanceRepository;
		this.userAccountRepository = userAccountRepository;
		this.payrollRepository = payrollRepository;
		this.employeeBenefitRepository = employeeBenefitRepository;
		this.employeeKPIRepository = employeeKPIRepository;
		this.companySettingsService = companySettingsService;
	}

	public AttendanceReportResponse generateAttendanceReport(String reportType, LocalDate startDate, LocalDate endDate, String department) {
		// Get company settings for work hours calculation
		CompanySettingsResponse companySettings = companySettingsService.getSettings();
		LocalTime workEndTime = companySettings.workEndTime() != null ? companySettings.workEndTime() : LocalTime.of(17, 0);
		
		// Get all attendance records in the date range
		List<AttendanceRecord> allRecords = attendanceRepository.findAll().stream()
				.filter(record -> {
					LocalDate workDate = record.getWorkDate();
					return workDate != null && !workDate.isBefore(startDate) && !workDate.isAfter(endDate);
				})
				.collect(Collectors.toList());

		// Filter by department if specified
		if (department != null && !department.isEmpty() && !department.equals("All Departments")) {
			allRecords = allRecords.stream()
					.filter(record -> {
						UserAccount user = userAccountRepository.findById(record.getUserId()).orElse(null);
						return user != null && department.equals(user.getDepartment());
					})
					.collect(Collectors.toList());
		}

		// Group by report type
		List<AttendanceReportResponse.AttendanceReportItem> items = new ArrayList<>();
		double totalWorkHours = 0.0;
		double totalOvertimeHours = 0.0;
		
		for (AttendanceRecord record : allRecords) {
			UserAccount user = userAccountRepository.findById(record.getUserId()).orElse(null);
			if (user == null) continue;

			// Calculate work hours
			Double workHours = null;
			Double overtimeHours = null;
			
			if (record.getCheckIn() != null && record.getCheckOut() != null) {
				LocalTime checkIn = record.getCheckIn();
				LocalTime checkOut = record.getCheckOut();
				
				// Calculate total hours worked
				Duration totalDuration = Duration.between(checkIn, checkOut);
				if (totalDuration.isNegative()) {
					// Handle overnight shifts
					totalDuration = totalDuration.plusDays(1);
				}
				workHours = totalDuration.toMinutes() / 60.0;
				
				// Calculate overtime hours (time after workEndTime)
				if (checkOut.isAfter(workEndTime)) {
					Duration otDuration = Duration.between(workEndTime, checkOut);
					overtimeHours = otDuration.toMinutes() / 60.0;
				} else {
					overtimeHours = 0.0;
				}
				
				// Subtract overtime from work hours to get regular work hours
				if (overtimeHours > 0) {
					workHours = workHours - overtimeHours;
				}
				
				totalWorkHours += workHours;
				totalOvertimeHours += overtimeHours;
			} else if (record.getStatus() != null && 
					(record.getStatus().name().equals("PRESENT") || 
					 record.getStatus().name().equals("LATE") || 
					 record.getStatus().name().equals("REMOTE"))) {
				// Default to 8 hours if no check-in/out but status indicates present
				workHours = 8.0;
				overtimeHours = 0.0;
				totalWorkHours += workHours;
			}

			items.add(new AttendanceReportResponse.AttendanceReportItem(
					user.getEmployeeId() != null ? user.getEmployeeId() : "",
					user.getFullName() != null ? user.getFullName() : "",
					record.getWorkDate() != null ? record.getWorkDate().toString() : "",
					record.getCheckIn() != null ? record.getCheckIn().toString() : "",
					record.getCheckOut() != null ? record.getCheckOut().toString() : "",
					record.getStatus() != null ? record.getStatus().name() : "",
					record.getNotes() != null ? record.getNotes() : "",
					workHours,
					overtimeHours,
					user.getDepartment() != null ? user.getDepartment() : "",
					user.getJobTitle() != null ? user.getJobTitle() : ""
			));
		}

		return new AttendanceReportResponse(
				reportType,
				startDate.toString(),
				endDate.toString(),
				department != null ? department : "All Departments",
				LocalDate.now().toString(), // Date Generated
				items.size(),
				totalWorkHours,
				totalOvertimeHours,
				items
		);
	}

	public PayrollReportResponse generatePayrollReport(String reportType, YearMonth month, String employeeId, String department, boolean includeStatutory) {
		LocalDate monthStart = month.atDay(1);
		LocalDate monthEnd = month.atEndOfMonth();

		// Get payroll records for the month
		List<PayrollRecord> payrollRecords = payrollRepository.findAll().stream()
				.filter(record -> {
					LocalDate periodStart = record.getPeriodStart();
					LocalDate periodEnd = record.getPeriodEnd();
					return periodStart != null && periodEnd != null
							&& !periodStart.isAfter(monthEnd) && !periodEnd.isBefore(monthStart);
				})
				.collect(Collectors.toList());

		// Filter by employee or department
		if (reportType.equals("employee") && employeeId != null && !employeeId.isEmpty()) {
			UserAccount user = userAccountRepository.findById(employeeId).orElse(null);
			if (user != null) {
				payrollRecords = payrollRecords.stream()
						.filter(record -> record.getUserId().equals(user.getId()))
						.collect(Collectors.toList());
			}
		} else if (reportType.equals("department") && department != null && !department.isEmpty() && !department.equals("All Departments")) {
			payrollRecords = payrollRecords.stream()
					.filter(record -> {
						UserAccount user = userAccountRepository.findById(record.getUserId()).orElse(null);
						return user != null && department.equals(user.getDepartment());
					})
					.collect(Collectors.toList());
		}

		List<PayrollReportResponse.PayrollReportItem> items = new ArrayList<>();
		BigDecimal totalEpfEmployee = BigDecimal.ZERO;
		BigDecimal totalEpfEmployer = BigDecimal.ZERO;
		BigDecimal totalSocsoEmployee = BigDecimal.ZERO;
		BigDecimal totalSocsoEmployer = BigDecimal.ZERO;
		BigDecimal totalEisEmployee = BigDecimal.ZERO;
		BigDecimal totalEisEmployer = BigDecimal.ZERO;
		BigDecimal totalPcb = BigDecimal.ZERO;

		for (PayrollRecord record : payrollRecords) {
			UserAccount user = userAccountRepository.findById(record.getUserId()).orElse(null);
			if (user == null) continue;

			items.add(new PayrollReportResponse.PayrollReportItem(
					user.getEmployeeId() != null ? user.getEmployeeId() : "",
					user.getFullName() != null ? user.getFullName() : "",
					user.getDepartment() != null ? user.getDepartment() : "",
					record.getBasicSalary() != null ? record.getBasicSalary() : BigDecimal.ZERO,
					record.getAdjustedBasicSalary() != null ? record.getAdjustedBasicSalary() : BigDecimal.ZERO,
					record.getTotalOtPay() != null ? record.getTotalOtPay() : BigDecimal.ZERO,
					record.getKpiBonus() != null ? record.getKpiBonus() : BigDecimal.ZERO,
					record.getBenefitBonus() != null ? record.getBenefitBonus() : BigDecimal.ZERO,
					record.getGrossPay() != null ? record.getGrossPay() : BigDecimal.ZERO,
					record.getEpfEmployee() != null ? record.getEpfEmployee() : BigDecimal.ZERO,
					record.getEpfEmployer() != null ? record.getEpfEmployer() : BigDecimal.ZERO,
					record.getSocsoEmployee() != null ? record.getSocsoEmployee() : BigDecimal.ZERO,
					record.getSocsoEmployer() != null ? record.getSocsoEmployer() : BigDecimal.ZERO,
					record.getEisEmployee() != null ? record.getEisEmployee() : BigDecimal.ZERO,
					record.getEisEmployer() != null ? record.getEisEmployer() : BigDecimal.ZERO,
					record.getPcb() != null ? record.getPcb() : BigDecimal.ZERO,
					record.getTotalEmployeeDeductions() != null ? record.getTotalEmployeeDeductions() : BigDecimal.ZERO,
					record.getNetPay() != null ? record.getNetPay() : BigDecimal.ZERO
			));

			if (includeStatutory) {
				if (record.getEpfEmployee() != null) totalEpfEmployee = totalEpfEmployee.add(record.getEpfEmployee());
				if (record.getEpfEmployer() != null) totalEpfEmployer = totalEpfEmployer.add(record.getEpfEmployer());
				if (record.getSocsoEmployee() != null) totalSocsoEmployee = totalSocsoEmployee.add(record.getSocsoEmployee());
				if (record.getSocsoEmployer() != null) totalSocsoEmployer = totalSocsoEmployer.add(record.getSocsoEmployer());
				if (record.getEisEmployee() != null) totalEisEmployee = totalEisEmployee.add(record.getEisEmployee());
				if (record.getEisEmployer() != null) totalEisEmployer = totalEisEmployer.add(record.getEisEmployer());
				if (record.getPcb() != null) totalPcb = totalPcb.add(record.getPcb());
			}
		}

		PayrollReportResponse.StatutorySummary statutorySummary = includeStatutory
				? new PayrollReportResponse.StatutorySummary(
						totalEpfEmployee,
						totalEpfEmployer,
						totalSocsoEmployee,
						totalSocsoEmployer,
						totalEisEmployee,
						totalEisEmployer,
						totalPcb)
				: null;

		return new PayrollReportResponse(
				reportType,
				month.toString(),
				employeeId != null ? employeeId : "",
				department != null ? department : "All Departments",
				includeStatutory,
				items.size(),
				items,
				statutorySummary
		);
	}

	public BenefitsReportResponse generateBenefitsReport(String benefitCategoryId, String department) {
		List<EmployeeBenefit> allBenefits = employeeBenefitRepository.findAll();

		// Filter by benefit category if specified
		if (benefitCategoryId != null && !benefitCategoryId.isEmpty() && !benefitCategoryId.equals("All Benefits")) {
			allBenefits = allBenefits.stream()
					.filter(benefit -> benefitCategoryId.equals(benefit.getBenefitCategoryId()))
					.collect(Collectors.toList());
		}

		// Filter by department if specified
		if (department != null && !department.isEmpty() && !department.equals("All Departments")) {
			allBenefits = allBenefits.stream()
					.filter(benefit -> {
						UserAccount user = userAccountRepository.findById(benefit.getUserId()).orElse(null);
						return user != null && department.equals(user.getDepartment());
					})
					.collect(Collectors.toList());
		}

		List<BenefitsReportResponse.BenefitsReportItem> items = new ArrayList<>();
		for (EmployeeBenefit benefit : allBenefits) {
			UserAccount user = userAccountRepository.findById(benefit.getUserId()).orElse(null);
			if (user == null) continue;

			// Get benefit category name (would need BenefitCategoryService, but for now use ID)
			String benefitCategoryName = benefit.getBenefitCategoryId();

			items.add(new BenefitsReportResponse.BenefitsReportItem(
					user.getEmployeeId() != null ? user.getEmployeeId() : "",
					user.getFullName() != null ? user.getFullName() : "",
					user.getDepartment() != null ? user.getDepartment() : "",
					benefit.getBenefitName() != null ? benefit.getBenefitName() : (benefitCategoryName != null ? benefitCategoryName : ""),
					benefitCategoryName != null ? benefitCategoryName : "",
					"ASSIGNED", // EmployeeBenefit doesn't have status field, default to ASSIGNED
					benefit.getAssignedAt() != null ? benefit.getAssignedAt().toString() : ""
			));
		}

		return new BenefitsReportResponse(
				"benefits",
				benefitCategoryId != null ? benefitCategoryId : "",
				department != null ? department : "All Departments",
				null,
				items.size(),
				items
		);
	}

	public KPIPerformanceReportResponse generateKPIPerformanceReport(String department, int year) {
		// Get all employee KPIs
		List<EmployeeKPI> allKPIs = employeeKPIRepository.findAll();

		// Filter by department if specified
		if (department != null && !department.isEmpty() && !department.equals("All Departments")) {
			allKPIs = allKPIs.stream()
					.filter(kpi -> {
						UserAccount user = userAccountRepository.findById(kpi.getUserId()).orElse(null);
						return user != null && department.equals(user.getDepartment());
					})
					.collect(Collectors.toList());
		}

		// Filter by year if specified (based on due date or completion date)
		// For now, we'll include all KPIs

		List<KPIPerformanceReportResponse.KPIPerformanceReportItem> items = new ArrayList<>();
		for (EmployeeKPI employeeKPI : allKPIs) {
			UserAccount user = userAccountRepository.findById(employeeKPI.getUserId()).orElse(null);
			if (user == null) continue;

			// Get KPI details
			String kpiName = employeeKPI.getKpiName() != null ? employeeKPI.getKpiName() : employeeKPI.getKpiId();

			// Parse measurable value (target) and current progress value (actual)
			BigDecimal targetValue = BigDecimal.ZERO;
			try {
				if (employeeKPI.getMeasurableValue() != null && !employeeKPI.getMeasurableValue().isEmpty()) {
					targetValue = new BigDecimal(employeeKPI.getMeasurableValue());
				}
			} catch (NumberFormatException e) {
				// If not a number, use 0
			}
			
			BigDecimal actualValue = BigDecimal.ZERO;
			if (employeeKPI.getCurrentProgressValue() != null) {
				actualValue = BigDecimal.valueOf(employeeKPI.getCurrentProgressValue());
			}
			
			BigDecimal achievementPercentage = BigDecimal.ZERO;
			if (targetValue.compareTo(BigDecimal.ZERO) > 0) {
				achievementPercentage = actualValue.divide(targetValue, 4, java.math.RoundingMode.HALF_UP)
						.multiply(BigDecimal.valueOf(100));
			}

			items.add(new KPIPerformanceReportResponse.KPIPerformanceReportItem(
					user.getEmployeeId() != null ? user.getEmployeeId() : "",
					user.getFullName() != null ? user.getFullName() : "",
					user.getDepartment() != null ? user.getDepartment() : "",
					employeeKPI.getKpiId() != null ? employeeKPI.getKpiId() : "",
					kpiName != null ? kpiName : "",
					targetValue,
					actualValue,
					achievementPercentage,
					employeeKPI.getStatus() != null ? employeeKPI.getStatus() : "",
					employeeKPI.getBonusAmount() != null ? BigDecimal.valueOf(employeeKPI.getBonusAmount()) : BigDecimal.ZERO
			));
		}

		return new KPIPerformanceReportResponse(
				"performance",
				department != null ? department : "All Departments",
				String.valueOf(year),
				items.size(),
				items
		);
	}

	// Employee-specific report methods (only returns data for the specified user)
	public AttendanceReportResponse generateEmployeeAttendanceReport(String userId, String reportType, LocalDate startDate, LocalDate endDate) {
		// Get company settings for work hours calculation
		CompanySettingsResponse companySettings = companySettingsService.getSettings();
		LocalTime workEndTime = companySettings.workEndTime() != null ? companySettings.workEndTime() : LocalTime.of(17, 0);
		
		// Get all attendance records for this user in the date range
		List<AttendanceRecord> allRecords = attendanceRepository.findByUserIdOrderByWorkDateDesc(userId).stream()
				.filter(record -> {
					LocalDate workDate = record.getWorkDate();
					return workDate != null && !workDate.isBefore(startDate) && !workDate.isAfter(endDate);
				})
				.collect(Collectors.toList());

		// Group by report type
		List<AttendanceReportResponse.AttendanceReportItem> items = new ArrayList<>();
		double totalWorkHours = 0.0;
		double totalOvertimeHours = 0.0;
		
		for (AttendanceRecord record : allRecords) {
			UserAccount user = userAccountRepository.findById(record.getUserId()).orElse(null);
			if (user == null) continue;

			// Calculate work hours
			Double workHours = null;
			Double overtimeHours = null;
			
			if (record.getCheckIn() != null && record.getCheckOut() != null) {
				LocalTime checkIn = record.getCheckIn();
				LocalTime checkOut = record.getCheckOut();
				
				// Calculate total hours worked
				Duration totalDuration = Duration.between(checkIn, checkOut);
				if (totalDuration.isNegative()) {
					// Handle overnight shifts
					totalDuration = totalDuration.plusDays(1);
				}
				workHours = totalDuration.toMinutes() / 60.0;
				
				// Calculate overtime (hours beyond work end time)
				if (checkOut.isAfter(workEndTime)) {
					Duration overtimeDuration = Duration.between(workEndTime, checkOut);
					overtimeHours = overtimeDuration.toMinutes() / 60.0;
				} else {
					overtimeHours = 0.0;
				}
			}

			totalWorkHours += workHours != null ? workHours : 0.0;
			totalOvertimeHours += overtimeHours != null ? overtimeHours : 0.0;

			items.add(new AttendanceReportResponse.AttendanceReportItem(
					user.getEmployeeId() != null ? user.getEmployeeId() : "",
					user.getFullName() != null ? user.getFullName() : "",
					record.getWorkDate() != null ? record.getWorkDate().toString() : "",
					record.getCheckIn() != null ? record.getCheckIn().toString() : "",
					record.getCheckOut() != null ? record.getCheckOut().toString() : "",
					record.getStatus() != null ? record.getStatus().name() : "",
					record.getNotes() != null ? record.getNotes() : "",
					workHours,
					overtimeHours,
					user.getDepartment() != null ? user.getDepartment() : "",
					user.getJobTitle() != null ? user.getJobTitle() : ""
			));
		}

		return new AttendanceReportResponse(
				reportType,
				startDate.toString(),
				endDate.toString(),
				"", // department - not applicable for employee reports
				java.time.LocalDate.now().toString(),
				items.size(),
				totalWorkHours,
				totalOvertimeHours,
				items
		);
	}

	public KPIPerformanceReportResponse generateEmployeeKPIPerformanceReport(String userId, int year) {
		// Get all employee KPIs for this user
		List<EmployeeKPI> allKPIs = employeeKPIRepository.findByUserId(userId);

		List<KPIPerformanceReportResponse.KPIPerformanceReportItem> items = new ArrayList<>();
		for (EmployeeKPI employeeKPI : allKPIs) {
			UserAccount user = userAccountRepository.findById(employeeKPI.getUserId()).orElse(null);
			if (user == null) continue;

			// Get KPI details
			String kpiName = employeeKPI.getKpiName() != null ? employeeKPI.getKpiName() : employeeKPI.getKpiId();

			// Parse measurable value (target) and current progress value (actual)
			BigDecimal targetValue = BigDecimal.ZERO;
			try {
				if (employeeKPI.getMeasurableValue() != null && !employeeKPI.getMeasurableValue().isEmpty()) {
					targetValue = new BigDecimal(employeeKPI.getMeasurableValue());
				}
			} catch (NumberFormatException e) {
				// If not a number, use 0
			}
			
			BigDecimal actualValue = BigDecimal.ZERO;
			if (employeeKPI.getCurrentProgressValue() != null) {
				actualValue = BigDecimal.valueOf(employeeKPI.getCurrentProgressValue());
			}
			
			BigDecimal achievementPercentage = BigDecimal.ZERO;
			if (targetValue.compareTo(BigDecimal.ZERO) > 0) {
				achievementPercentage = actualValue.divide(targetValue, 4, java.math.RoundingMode.HALF_UP)
						.multiply(BigDecimal.valueOf(100));
			}

			items.add(new KPIPerformanceReportResponse.KPIPerformanceReportItem(
					user.getEmployeeId() != null ? user.getEmployeeId() : "",
					user.getFullName() != null ? user.getFullName() : "",
					user.getDepartment() != null ? user.getDepartment() : "",
					employeeKPI.getKpiId() != null ? employeeKPI.getKpiId() : "",
					kpiName != null ? kpiName : "",
					targetValue,
					actualValue,
					achievementPercentage,
					employeeKPI.getStatus() != null ? employeeKPI.getStatus() : "",
					employeeKPI.getBonusAmount() != null ? BigDecimal.valueOf(employeeKPI.getBonusAmount()) : BigDecimal.ZERO
			));
		}

		return new KPIPerformanceReportResponse(
				"performance",
				"My Performance",
				String.valueOf(year),
				items.size(),
				items
		);
	}

	public PayrollReportResponse generateEmployeePayrollReport(String userId, YearMonth month, boolean includeStatutory) {
		// Get payroll record for this user in the specified month
		Optional<PayrollRecord> recordOpt = payrollRepository.findByUserIdAndSalaryMonth(userId, month);
		List<PayrollRecord> records = recordOpt.map(List::of).orElse(List.of());

		List<PayrollReportResponse.PayrollReportItem> items = new ArrayList<>();
		BigDecimal totalEpfEmployee = BigDecimal.ZERO;
		BigDecimal totalEpfEmployer = BigDecimal.ZERO;
		BigDecimal totalSocsoEmployee = BigDecimal.ZERO;
		BigDecimal totalSocsoEmployer = BigDecimal.ZERO;
		BigDecimal totalEisEmployee = BigDecimal.ZERO;
		BigDecimal totalEisEmployer = BigDecimal.ZERO;
		BigDecimal totalPcb = BigDecimal.ZERO;

		for (PayrollRecord record : records) {
			UserAccount user = userAccountRepository.findById(record.getUserId()).orElse(null);
			if (user == null) continue;

			items.add(new PayrollReportResponse.PayrollReportItem(
					user.getEmployeeId() != null ? user.getEmployeeId() : "",
					user.getFullName() != null ? user.getFullName() : "",
					user.getDepartment() != null ? user.getDepartment() : "",
					record.getBasicSalary() != null ? record.getBasicSalary() : BigDecimal.ZERO,
					record.getAdjustedBasicSalary() != null ? record.getAdjustedBasicSalary() : BigDecimal.ZERO,
					record.getTotalOtPay() != null ? record.getTotalOtPay() : BigDecimal.ZERO,
					record.getKpiBonus() != null ? record.getKpiBonus() : BigDecimal.ZERO,
					record.getBenefitBonus() != null ? record.getBenefitBonus() : BigDecimal.ZERO,
					record.getGrossPay() != null ? record.getGrossPay() : BigDecimal.ZERO,
					record.getEpfEmployee() != null ? record.getEpfEmployee() : BigDecimal.ZERO,
					record.getEpfEmployer() != null ? record.getEpfEmployer() : BigDecimal.ZERO,
					record.getSocsoEmployee() != null ? record.getSocsoEmployee() : BigDecimal.ZERO,
					record.getSocsoEmployer() != null ? record.getSocsoEmployer() : BigDecimal.ZERO,
					record.getEisEmployee() != null ? record.getEisEmployee() : BigDecimal.ZERO,
					record.getEisEmployer() != null ? record.getEisEmployer() : BigDecimal.ZERO,
					record.getPcb() != null ? record.getPcb() : BigDecimal.ZERO,
					record.getTotalEmployeeDeductions() != null ? record.getTotalEmployeeDeductions() : BigDecimal.ZERO,
					record.getNetPay() != null ? record.getNetPay() : BigDecimal.ZERO
			));

			if (includeStatutory) {
				if (record.getEpfEmployee() != null) totalEpfEmployee = totalEpfEmployee.add(record.getEpfEmployee());
				if (record.getEpfEmployer() != null) totalEpfEmployer = totalEpfEmployer.add(record.getEpfEmployer());
				if (record.getSocsoEmployee() != null) totalSocsoEmployee = totalSocsoEmployee.add(record.getSocsoEmployee());
				if (record.getSocsoEmployer() != null) totalSocsoEmployer = totalSocsoEmployer.add(record.getSocsoEmployer());
				if (record.getEisEmployee() != null) totalEisEmployee = totalEisEmployee.add(record.getEisEmployee());
				if (record.getEisEmployer() != null) totalEisEmployer = totalEisEmployer.add(record.getEisEmployer());
				if (record.getPcb() != null) totalPcb = totalPcb.add(record.getPcb());
			}
		}

		PayrollReportResponse.StatutorySummary statutorySummary = includeStatutory
				? new PayrollReportResponse.StatutorySummary(
						totalEpfEmployee,
						totalEpfEmployer,
						totalSocsoEmployee,
						totalSocsoEmployer,
						totalEisEmployee,
						totalEisEmployer,
						totalPcb)
				: null;

		return new PayrollReportResponse(
				"employee",
				month.toString(),
				"",
				"",
				includeStatutory,
				items.size(),
				items,
				statutorySummary
		);
	}

	public PayrollReportResponse generateEmployeeAnnualPayrollReport(String userId, int year, boolean includeStatutory) {
		// Get all payroll records for this user in the specified year
		LocalDate yearStart = LocalDate.of(year, 1, 1);
		LocalDate yearEnd = LocalDate.of(year, 12, 31);
		
		List<PayrollRecord> records = payrollRepository.findByUserIdOrderByPeriodEndDesc(userId).stream()
				.filter(record -> {
					YearMonth salaryMonth = record.getSalaryMonth();
					if (salaryMonth != null) {
						return salaryMonth.getYear() == year;
					}
					// Fallback to periodEnd if salaryMonth is null
					LocalDate periodEnd = record.getPeriodEnd();
					return periodEnd != null && !periodEnd.isBefore(yearStart) && !periodEnd.isAfter(yearEnd);
				})
				.collect(Collectors.toList());

		List<PayrollReportResponse.PayrollReportItem> items = new ArrayList<>();
		BigDecimal totalEpfEmployee = BigDecimal.ZERO;
		BigDecimal totalEpfEmployer = BigDecimal.ZERO;
		BigDecimal totalSocsoEmployee = BigDecimal.ZERO;
		BigDecimal totalSocsoEmployer = BigDecimal.ZERO;
		BigDecimal totalEisEmployee = BigDecimal.ZERO;
		BigDecimal totalEisEmployer = BigDecimal.ZERO;
		BigDecimal totalPcb = BigDecimal.ZERO;

		for (PayrollRecord record : records) {
			UserAccount user = userAccountRepository.findById(record.getUserId()).orElse(null);
			if (user == null) continue;

			items.add(new PayrollReportResponse.PayrollReportItem(
					user.getEmployeeId() != null ? user.getEmployeeId() : "",
					user.getFullName() != null ? user.getFullName() : "",
					user.getDepartment() != null ? user.getDepartment() : "",
					record.getBasicSalary() != null ? record.getBasicSalary() : BigDecimal.ZERO,
					record.getAdjustedBasicSalary() != null ? record.getAdjustedBasicSalary() : BigDecimal.ZERO,
					record.getTotalOtPay() != null ? record.getTotalOtPay() : BigDecimal.ZERO,
					record.getKpiBonus() != null ? record.getKpiBonus() : BigDecimal.ZERO,
					record.getBenefitBonus() != null ? record.getBenefitBonus() : BigDecimal.ZERO,
					record.getGrossPay() != null ? record.getGrossPay() : BigDecimal.ZERO,
					record.getEpfEmployee() != null ? record.getEpfEmployee() : BigDecimal.ZERO,
					record.getEpfEmployer() != null ? record.getEpfEmployer() : BigDecimal.ZERO,
					record.getSocsoEmployee() != null ? record.getSocsoEmployee() : BigDecimal.ZERO,
					record.getSocsoEmployer() != null ? record.getSocsoEmployer() : BigDecimal.ZERO,
					record.getEisEmployee() != null ? record.getEisEmployee() : BigDecimal.ZERO,
					record.getEisEmployer() != null ? record.getEisEmployer() : BigDecimal.ZERO,
					record.getPcb() != null ? record.getPcb() : BigDecimal.ZERO,
					record.getTotalEmployeeDeductions() != null ? record.getTotalEmployeeDeductions() : BigDecimal.ZERO,
					record.getNetPay() != null ? record.getNetPay() : BigDecimal.ZERO
			));

			if (includeStatutory) {
				if (record.getEpfEmployee() != null) totalEpfEmployee = totalEpfEmployee.add(record.getEpfEmployee());
				if (record.getEpfEmployer() != null) totalEpfEmployer = totalEpfEmployer.add(record.getEpfEmployer());
				if (record.getSocsoEmployee() != null) totalSocsoEmployee = totalSocsoEmployee.add(record.getSocsoEmployee());
				if (record.getSocsoEmployer() != null) totalSocsoEmployer = totalSocsoEmployer.add(record.getSocsoEmployer());
				if (record.getEisEmployee() != null) totalEisEmployee = totalEisEmployee.add(record.getEisEmployee());
				if (record.getEisEmployer() != null) totalEisEmployer = totalEisEmployer.add(record.getEisEmployer());
				if (record.getPcb() != null) totalPcb = totalPcb.add(record.getPcb());
			}
		}

		PayrollReportResponse.StatutorySummary statutorySummary = includeStatutory
				? new PayrollReportResponse.StatutorySummary(
						totalEpfEmployee,
						totalEpfEmployer,
						totalSocsoEmployee,
						totalSocsoEmployer,
						totalEisEmployee,
						totalEisEmployer,
						totalPcb)
				: null;

		return new PayrollReportResponse(
				"annual",
				String.valueOf(year),
				"",
				"",
				includeStatutory,
				items.size(),
				items,
				statutorySummary
		);
	}
}

