package com.example.employeemanagement.dto;

import java.util.List;

public record AttendanceReportResponse(
		String reportType,
		String startDate,
		String endDate,
		String department,
		String dateGenerated,
		int totalRecords,
		double totalWorkHours,
		double totalOvertimeHours,
		List<AttendanceReportItem> items
) {
	public record AttendanceReportItem(
			String employeeId,
			String employeeName,
			String workDate,
			String checkIn,
			String checkOut,
			String status,
			String notes,
			Double workHours,
			Double overtimeHours,
			String department,
			String jobTitle
	) {}
}

