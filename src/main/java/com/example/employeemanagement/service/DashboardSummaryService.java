package com.example.employeemanagement.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.employeemanagement.dto.AttendanceResponse;
import com.example.employeemanagement.dto.DashboardSummaryResponse;
import com.example.employeemanagement.dto.EmployeeKPIResponse;
import com.example.employeemanagement.dto.LeaveBalanceResponse;
import com.example.employeemanagement.dto.LeaveResponse;
import com.example.employeemanagement.dto.OvertimeResponse;
import com.example.employeemanagement.model.AttendanceStatus;
import com.example.employeemanagement.model.LeaveStatus;
import com.example.employeemanagement.model.OvertimeStatus;
import com.example.employeemanagement.model.Role;
import com.example.employeemanagement.model.UserAccount;
import com.example.employeemanagement.repository.UserAccountRepository;
import com.example.employeemanagement.repository.DepartmentRepository;

@Service
public class DashboardSummaryService {

	private final LeaveService leaveService;
	private final AttendanceService attendanceService;
	private final LeaveBalanceService leaveBalanceService;
	private final EmployeeKPIService employeeKPIService;
	private final OvertimeService overtimeService;
	private final UserAccountRepository userAccountRepository;
	private final DepartmentRepository departmentRepository;

	public DashboardSummaryService(LeaveService leaveService, AttendanceService attendanceService,
			LeaveBalanceService leaveBalanceService, EmployeeKPIService employeeKPIService,
			OvertimeService overtimeService,
			UserAccountRepository userAccountRepository, DepartmentRepository departmentRepository) {
		this.leaveService = leaveService;
		this.attendanceService = attendanceService;
		this.leaveBalanceService = leaveBalanceService;
		this.employeeKPIService = employeeKPIService;
		this.overtimeService = overtimeService;
		this.userAccountRepository = userAccountRepository;
		this.departmentRepository = departmentRepository;
	}

	public DashboardSummaryResponse getAdminSummary() {
		LocalDate now = LocalDate.now();
		LocalDate firstDayOfMonth = now.withDayOfMonth(1);
		LocalDate firstDayOfLastMonth = firstDayOfMonth.minusMonths(1);

		// Get all leave requests
		List<LeaveResponse> allLeaves = leaveService.findAll();
		Instant monthStart = firstDayOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant();
		Instant lastMonthStart = firstDayOfLastMonth.atStartOfDay(ZoneId.systemDefault()).toInstant();

		// New tickets (pending leave requests this month)
		long newTickets = allLeaves.stream()
				.filter(leave -> leave.status() == LeaveStatus.PENDING
						&& leave.createdAt() != null
						&& leave.createdAt().isAfter(monthStart))
				.count();

		// Tickets resolved (approved/rejected this month)
		long ticketsResolved = allLeaves.stream()
				.filter(leave -> (leave.status() == LeaveStatus.APPROVED || leave.status() == LeaveStatus.REJECTED)
						&& leave.decidedAt() != null
						&& leave.decidedAt().isAfter(monthStart))
				.count();

		// Available leave (total remaining leave days across all employees)
		// This is a simplified calculation - in reality, we'd sum from LeaveBalance
		long availableLeave = allLeaves.stream()
				.filter(leave -> leave.status() == LeaveStatus.APPROVED)
				.mapToLong(LeaveResponse::totalDays)
				.sum();

		// Projects assigned (all KPIs, excluding COMPLETED)
		// Count all assigned KPIs that are not completed (PENDING, INCOMPLETE, or any other active status)
		long projectsAssigned = employeeKPIService.getAllAssignments().stream()
				.filter(kpi -> kpi.status() == null || !kpi.status().equalsIgnoreCase("COMPLETED"))
				.count();

		// Calculate weekly working hours (average from attendance this week)
		LocalDate weekStart = now.minusDays(now.getDayOfWeek().getValue() - 1);
		List<AttendanceResponse> thisWeekAttendance = attendanceService.findAll().stream()
				.filter(att -> {
					if (att.workDate() == null)
						return false;
					LocalDate workDate = att.workDate();
					return !workDate.isBefore(weekStart) && !workDate.isAfter(now);
				})
				.collect(Collectors.toList());

		double weeklyWorkingHours = calculateWeeklyHours(thisWeekAttendance);

		// Calculate changes from last month
		long lastMonthNewTickets = allLeaves.stream()
				.filter(leave -> leave.status() == LeaveStatus.PENDING
						&& leave.createdAt() != null
						&& leave.createdAt().isAfter(lastMonthStart)
						&& leave.createdAt().isBefore(monthStart))
				.count();

		long lastMonthTicketsResolved = allLeaves.stream()
				.filter(leave -> (leave.status() == LeaveStatus.APPROVED || leave.status() == LeaveStatus.REJECTED)
						&& leave.decidedAt() != null
						&& leave.decidedAt().isAfter(lastMonthStart)
						&& leave.decidedAt().isBefore(monthStart))
				.count();

		long lastMonthAvailableLeave = allLeaves.stream()
				.filter(leave -> leave.status() == LeaveStatus.APPROVED
						&& leave.createdAt() != null
						&& leave.createdAt().isAfter(lastMonthStart)
						&& leave.createdAt().isBefore(monthStart))
				.mapToLong(LeaveResponse::totalDays)
				.sum();

		long lastMonthProjectsAssigned = employeeKPIService.getAllAssignments().stream()
				.filter(kpi -> kpi.status() == null || !kpi.status().equalsIgnoreCase("COMPLETED"))
				.count(); // Simplified - in real scenario, track historical data

		double newTicketsChangePercent = calculateChangePercent(lastMonthNewTickets, newTickets);
		double ticketsResolvedChangePercent = calculateChangePercent(lastMonthTicketsResolved, ticketsResolved);
		double availableLeaveChangePercent = calculateChangePercent(lastMonthAvailableLeave, availableLeave);
		double projectsAssignedChangePercent = calculateChangePercent(lastMonthProjectsAssigned, projectsAssigned);

		// Generate chart data (last 7 days)
		List<Integer> newTicketsChartData = generateChartData(allLeaves, 7, leave -> 
			leave.status() == LeaveStatus.PENDING && leave.createdAt() != null, true);
		List<Integer> ticketsResolvedChartData = generateChartData(allLeaves, 7, leave -> 
			(leave.status() == LeaveStatus.APPROVED || leave.status() == LeaveStatus.REJECTED) 
			&& leave.decidedAt() != null, false);
		List<Integer> availableLeaveChartData = generateChartData(allLeaves, 7, leave -> 
			leave.status() == LeaveStatus.APPROVED, true);
		List<Integer> projectsAssignedChartData = generateDailyCount(7);

		// Calculate total employees (only EMPLOYEE role)
		List<UserAccount> allUsers = userAccountRepository.findAll();
		long totalEmployees = allUsers.stream()
				.filter(user -> user.getRole() != null && Role.EMPLOYEE.equals(user.getRole()))
				.count();

		// Calculate total departments
		long totalDepartments = departmentRepository.count();
		
		// Debug logging (can be removed after verification)
		System.out.println("DEBUG DashboardSummary: Total users in database: " + allUsers.size());
		System.out.println("DEBUG DashboardSummary: Total employees (EMPLOYEE role): " + totalEmployees);
		System.out.println("DEBUG DashboardSummary: Total departments: " + totalDepartments);
		for (UserAccount user : allUsers) {
			System.out.println("DEBUG DashboardSummary: User: " + user.getUsername() + ", Role: " + user.getRole());
		}

		return new DashboardSummaryResponse(
				newTickets, ticketsResolved, availableLeave, projectsAssigned, weeklyWorkingHours,
				newTicketsChangePercent, ticketsResolvedChangePercent, availableLeaveChangePercent,
				projectsAssignedChangePercent, newTicketsChartData, ticketsResolvedChartData,
				availableLeaveChartData, projectsAssignedChartData,
				totalEmployees, totalDepartments,
				null, null, null, null, null, null, null, null, // Admin summary doesn't need employee-specific metrics
				null, null, null, null, null, null); // Additional fields for admin (not used)
	}

	public DashboardSummaryResponse getEmployeeSummary(String userId) {
		LocalDate now = LocalDate.now();
		LocalDate firstDayOfMonth = now.withDayOfMonth(1);
		LocalDate firstDayOfLastMonth = firstDayOfMonth.minusMonths(1);

		Instant monthStart = firstDayOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant();
		Instant lastMonthStart = firstDayOfLastMonth.atStartOfDay(ZoneId.systemDefault()).toInstant();

		// Get employee-specific data
		List<LeaveResponse> myLeaves = leaveService.findForUser(userId);
		List<AttendanceResponse> myAttendance = attendanceService.findForUser(userId);
		LeaveBalanceResponse leaveBalance = leaveBalanceService.getLeaveBalances(userId);
		List<EmployeeKPIResponse> myKPIs = employeeKPIService.getKPIsByUserId(userId);
		List<OvertimeResponse> myOvertimeRequests = overtimeService.findForUser(userId);
		
		// Debug: Log all attendance records
		System.out.println("=== Employee Dashboard Summary Debug ===");
		System.out.println("UserId: " + userId);
		System.out.println("Current Date: " + LocalDate.now());
		System.out.println("Total attendance records for user: " + myAttendance.size());
		if (myAttendance.isEmpty()) {
			System.out.println("WARNING: No attendance records found for user!");
		} else {
			for (AttendanceResponse att : myAttendance) {
				System.out.println("  - Date: " + att.workDate() + ", Status: " + att.status() + 
						" (type: " + (att.status() != null ? att.status().getClass().getName() : "null") + ")");
			}
		}

		// New tickets (pending leave requests this month)
		long newTickets = myLeaves.stream()
				.filter(leave -> leave.status() == LeaveStatus.PENDING
						&& leave.createdAt() != null
						&& leave.createdAt().isAfter(monthStart))
				.count();

		// Tickets resolved (approved/rejected this month)
		long ticketsResolved = myLeaves.stream()
				.filter(leave -> (leave.status() == LeaveStatus.APPROVED || leave.status() == LeaveStatus.REJECTED)
						&& leave.decidedAt() != null
						&& leave.decidedAt().isAfter(monthStart))
				.count();

		// Available leave (total remaining days)
		long availableLeave = leaveBalance.balances().stream()
				.mapToLong(LeaveBalanceResponse.LeaveBalanceDto::remainingDays)
				.sum();

		// Projects assigned (all KPIs, excluding COMPLETED)
		// Count all assigned KPIs that are not completed (PENDING, INCOMPLETE, or any other active status)
		long projectsAssigned = myKPIs.stream()
				.filter(kpi -> kpi.status() == null || !kpi.status().equalsIgnoreCase("COMPLETED"))
				.count();

		// Calculate weekly working hours
		LocalDate weekStart = now.minusDays(now.getDayOfWeek().getValue() - 1);
		List<AttendanceResponse> thisWeekAttendance = myAttendance.stream()
				.filter(att -> {
					if (att.workDate() == null)
						return false;
					LocalDate workDate = att.workDate();
					return !workDate.isBefore(weekStart) && !workDate.isAfter(now);
				})
				.collect(Collectors.toList());

		double weeklyWorkingHours = calculateWeeklyHours(thisWeekAttendance);

		// Calculate changes from last month
		long lastMonthNewTickets = myLeaves.stream()
				.filter(leave -> leave.status() == LeaveStatus.PENDING
						&& leave.createdAt() != null
						&& leave.createdAt().isAfter(lastMonthStart)
						&& leave.createdAt().isBefore(monthStart))
				.count();

		long lastMonthTicketsResolved = myLeaves.stream()
				.filter(leave -> (leave.status() == LeaveStatus.APPROVED || leave.status() == LeaveStatus.REJECTED)
						&& leave.decidedAt() != null
						&& leave.decidedAt().isAfter(lastMonthStart)
						&& leave.decidedAt().isBefore(monthStart))
				.count();

		long lastMonthAvailableLeave = leaveBalance.balances().stream()
				.mapToLong(LeaveBalanceResponse.LeaveBalanceDto::remainingDays)
				.sum(); // Simplified - would need historical tracking

		long lastMonthProjectsAssigned = myKPIs.stream()
				.filter(kpi -> kpi.status() == null || !kpi.status().equalsIgnoreCase("COMPLETED"))
				.count();

		double newTicketsChangePercent = calculateChangePercent(lastMonthNewTickets, newTickets);
		double ticketsResolvedChangePercent = calculateChangePercent(lastMonthTicketsResolved, ticketsResolved);
		double availableLeaveChangePercent = calculateChangePercent(lastMonthAvailableLeave, availableLeave);
		double projectsAssignedChangePercent = calculateChangePercent(lastMonthProjectsAssigned, projectsAssigned);

		// Generate chart data (last 7 days)
		List<Integer> newTicketsChartData = generateChartData(myLeaves, 7, leave -> 
			leave.status() == LeaveStatus.PENDING && leave.createdAt() != null, true);
		List<Integer> ticketsResolvedChartData = generateChartData(myLeaves, 7, leave -> 
			(leave.status() == LeaveStatus.APPROVED || leave.status() == LeaveStatus.REJECTED) 
			&& leave.decidedAt() != null, false);
		List<Integer> availableLeaveChartData = generateDailyCount(7);
		List<Integer> projectsAssignedChartData = generateDailyCount(7);

		// Calculate new metrics for employee dashboard
		// Pending leave requests (all pending, not just this month)
		long pendingLeaveRequests = myLeaves.stream()
				.filter(leave -> leave.status() == LeaveStatus.PENDING)
				.count();

		// Monthly attendance days (PRESENT, LATE, or REMOTE status this month)
		// Filter by current month and calculate counts
		// Use end of month instead of today to include all records in the month
		LocalDate monthStartDate = firstDayOfMonth;
		LocalDate monthEndDate = now.withDayOfMonth(now.lengthOfMonth()); // End of current month
		
		System.out.println("=== Employee Dashboard Summary Calculation ===");
		System.out.println("UserId: " + userId);
		System.out.println("Current Date: " + LocalDate.now());
		System.out.println("Month Range: " + monthStartDate + " to " + monthEndDate);
		System.out.println("Total attendance records for user: " + myAttendance.size());
		
		// Filter attendance records for current month
		List<AttendanceResponse> thisMonthAttendance = myAttendance.stream()
				.filter(att -> {
					if (att.workDate() == null) {
						return false;
					}
					LocalDate workDate = att.workDate();
					return !workDate.isBefore(monthStartDate) && !workDate.isAfter(monthEndDate);
				})
				.collect(Collectors.toList());
		
		// Count monthly attendance days (PRESENT, LATE, REMOTE) and late arrivals (LATE only)
		long monthlyAttendanceDays = 0;
		long lateArrivals = 0;
		
		for (AttendanceResponse att : thisMonthAttendance) {
			LocalDate workDate = att.workDate();
			AttendanceStatus status = att.status();
			
			System.out.println("  Record: Date=" + workDate + ", Status=" + status);
			
			if (status != null) {
				// Count as attendance if PRESENT, LATE, or REMOTE
				if (status == AttendanceStatus.PRESENT || 
					status == AttendanceStatus.LATE || 
					status == AttendanceStatus.REMOTE) {
					monthlyAttendanceDays++;
					System.out.println("    ✓ Counted as attendance (Status: " + status + ")");
				}
				
				// Count as late arrival if LATE
				if (status == AttendanceStatus.LATE) {
					lateArrivals++;
					System.out.println("    ✓ Counted as late arrival");
				}
			}
		}
		
		System.out.println("=== RESULT ===");
		System.out.println("Records in current month: " + thisMonthAttendance.size());
		System.out.println("Monthly Attendance Days: " + monthlyAttendanceDays);
		System.out.println("Late Arrivals: " + lateArrivals);
		System.out.println("=== End Calculation ===");

		// Monthly working hours
		double monthlyWorkingHours = calculateWeeklyHours(thisMonthAttendance);

		// Calculate new metrics
		// 1. Pending Overtime Requests
		long pendingOvertimeRequests = myOvertimeRequests.stream()
				.filter(ot -> ot.status() == OvertimeStatus.PENDING)
				.count();

		// 2. Monthly Overtime Hours (approved overtime this month)
		// Use the same monthStartDate and monthEndDate defined above (line 279-280)
		List<OvertimeResponse> approvedOvertimeThisMonth = myOvertimeRequests.stream()
				.filter(ot -> ot.status() == OvertimeStatus.APPROVED
						&& ot.workDate() != null
						&& !ot.workDate().isBefore(monthStartDate)
						&& !ot.workDate().isAfter(monthEndDate))
				.collect(Collectors.toList());
		
		double monthlyOvertimeHours = approvedOvertimeThisMonth.stream()
				.mapToDouble(ot -> ot.hours() != null ? ot.hours() : 0.0)
				.sum();

		// 3. Upcoming Leaves (approved leaves with start date in the future)
		LocalDate today = LocalDate.now();
		long upcomingLeaves = myLeaves.stream()
				.filter(leave -> leave.status() == LeaveStatus.APPROVED
						&& leave.startDate() != null
						&& leave.startDate().isAfter(today))
				.count();

		// 4. KPI Completion Rate
		long totalKPIs = myKPIs.size();
		long completedKPIs = myKPIs.stream()
				.filter(kpi -> kpi.status() != null && "COMPLETED".equals(kpi.status()))
				.count();
		double kpiCompletionRate = totalKPIs > 0 ? (completedKPIs * 100.0 / totalKPIs) : 0.0;

		// 5. Sick Leave Balance & Annual Leave Balance
		long sickLeaveBalance = leaveBalance.balances().stream()
				.filter(balance -> balance.leaveType() != null && balance.leaveType().toUpperCase().contains("SICK"))
				.mapToLong(LeaveBalanceResponse.LeaveBalanceDto::remainingDays)
				.sum();
		
		long annualLeaveBalance = leaveBalance.balances().stream()
				.filter(balance -> balance.leaveType() != null && 
						(balance.leaveType().toUpperCase().contains("ANNUAL") || 
						 balance.leaveType().toUpperCase().contains("YEARLY")))
				.mapToLong(LeaveBalanceResponse.LeaveBalanceDto::remainingDays)
				.sum();

		// Generate chart data for new metrics
		List<Integer> pendingLeaveRequestsChartData = generateChartData(myLeaves, 7, leave -> 
			leave.status() == LeaveStatus.PENDING && leave.createdAt() != null, true);
		List<Integer> monthlyAttendanceDaysChartData = generateAttendanceChartData(thisMonthAttendance, 7);
		List<Integer> lateArrivalsChartData = generateAttendanceChartData(thisMonthAttendance.stream()
				.filter(att -> att.status() != null && att.status() == AttendanceStatus.LATE)
				.collect(Collectors.toList()), 7);
		List<Integer> monthlyWorkingHoursChartData = generateDailyCount(7);

		// Employee summary doesn't need total employees/departments
		return new DashboardSummaryResponse(
				newTickets, ticketsResolved, availableLeave, projectsAssigned, weeklyWorkingHours,
				newTicketsChangePercent, ticketsResolvedChangePercent, availableLeaveChangePercent,
				projectsAssignedChangePercent, newTicketsChartData, ticketsResolvedChartData,
				availableLeaveChartData, projectsAssignedChartData,
				null, null,
				pendingLeaveRequests, monthlyAttendanceDays, lateArrivals, monthlyWorkingHours,
				pendingLeaveRequestsChartData, monthlyAttendanceDaysChartData, lateArrivalsChartData, monthlyWorkingHoursChartData,
				pendingOvertimeRequests, monthlyOvertimeHours, upcomingLeaves, kpiCompletionRate,
				sickLeaveBalance, annualLeaveBalance);
	}

	private double calculateWeeklyHours(List<AttendanceResponse> attendance) {
		return attendance.stream()
				.mapToDouble(att -> {
					if (att.checkIn() != null && att.checkOut() != null) {
						long minutes = java.time.Duration.between(att.checkIn(), att.checkOut()).toMinutes();
						return minutes / 60.0;
					}
					return 8.0; // Default 8 hours if no check-in/out
				})
				.sum();
	}

	private double calculateChangePercent(long lastMonth, long thisMonth) {
		if (lastMonth == 0) {
			return thisMonth > 0 ? 100.0 : 0.0;
		}
		return ((thisMonth - lastMonth) / (double) lastMonth) * 100.0;
	}

	private List<Integer> generateChartData(List<LeaveResponse> leaves, int days,
			java.util.function.Predicate<LeaveResponse> filter, boolean useCreatedAt) {
		List<Integer> data = new ArrayList<>();
		LocalDate now = LocalDate.now();
		for (int i = days - 1; i >= 0; i--) {
			LocalDate date = now.minusDays(i);
			Instant dayStart = date.atStartOfDay(ZoneId.systemDefault()).toInstant();
			Instant dayEnd = date.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();

			long count = leaves.stream()
					.filter(filter)
					.filter(leave -> {
						Instant timestamp = useCreatedAt 
							? (leave.createdAt() != null ? leave.createdAt() : null)
							: (leave.decidedAt() != null ? leave.decidedAt() : null);
						return timestamp != null && !timestamp.isBefore(dayStart) && !timestamp.isAfter(dayEnd);
					})
					.count();
			data.add((int) count);
		}
		return data;
	}

	private List<Integer> generateDailyCount(int days) {
		List<Integer> data = new ArrayList<>();
		for (int i = 0; i < days; i++) {
			data.add((int) (Math.random() * 5) + 1); // Placeholder data
		}
		return data;
	}

	private List<Integer> generateAttendanceChartData(List<AttendanceResponse> attendance, int days) {
		List<Integer> data = new ArrayList<>();
		LocalDate now = LocalDate.now();
		for (int i = days - 1; i >= 0; i--) {
			LocalDate date = now.minusDays(i);
			long count = attendance.stream()
					.filter(att -> att.workDate() != null && att.workDate().equals(date))
					.count();
			data.add((int) count);
		}
		return data;
	}
}

