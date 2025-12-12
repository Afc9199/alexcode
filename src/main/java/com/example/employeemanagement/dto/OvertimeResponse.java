package com.example.employeemanagement.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import com.example.employeemanagement.model.OvertimeStatus;

public record OvertimeResponse(
		String id,
		String overtimeId,
		String userId,
		String employeeId,
		String employeeName,
		LocalDate workDate,
		LocalTime startTime,
		LocalTime endTime,
		Double hours,
		String reason,
		OvertimeStatus status,
		String managerComment,
		Instant createdAt,
		Instant decidedAt) {
}

