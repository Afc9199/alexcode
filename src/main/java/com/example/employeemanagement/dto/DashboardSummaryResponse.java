package com.example.employeemanagement.dto;

import java.util.List;

public record DashboardSummaryResponse(
		long newTickets,
		long ticketsResolved,
		long availableLeave,
		long projectsAssigned,
		double weeklyWorkingHours,
		double newTicketsChangePercent,
		double ticketsResolvedChangePercent,
		double availableLeaveChangePercent,
		double projectsAssignedChangePercent,
		List<Integer> newTicketsChartData,
		List<Integer> ticketsResolvedChartData,
		List<Integer> availableLeaveChartData,
		List<Integer> projectsAssignedChartData,
		Long totalEmployees,
		Long totalDepartments,
		// New fields for employee dashboard
		Long pendingLeaveRequests,
		Long monthlyAttendanceDays,
		Long lateArrivals,
		Double monthlyWorkingHours,
		List<Integer> pendingLeaveRequestsChartData,
		List<Integer> monthlyAttendanceDaysChartData,
		List<Integer> lateArrivalsChartData,
		List<Integer> monthlyWorkingHoursChartData,
		// Additional new fields
		Long pendingOvertimeRequests,
		Double monthlyOvertimeHours,
		Long upcomingLeaves,
		Double kpiCompletionRate,
		Long sickLeaveBalance,
		Long annualLeaveBalance) {
}
