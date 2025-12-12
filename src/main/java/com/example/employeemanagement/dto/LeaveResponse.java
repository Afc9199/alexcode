package com.example.employeemanagement.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.example.employeemanagement.model.LeaveStatus;

public record LeaveResponse(
		String id,
		String leaveId,
		String userId,
		String employeeId,
		String employeeName,
		String leaveType,
		LocalDate startDate,
		LocalDate endDate,
		String reason,
		String supportingDocumentFilename,
		LeaveStatus status,
		String managerComment,
		Instant createdAt,
		Instant decidedAt) {
	
	public long totalDays() {
		if (startDate == null || endDate == null) {
			return 0;
		}
		return java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
	}
}

