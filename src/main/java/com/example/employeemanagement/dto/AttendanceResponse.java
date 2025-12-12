package com.example.employeemanagement.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.example.employeemanagement.model.AttendanceStatus;

public record AttendanceResponse(
		String id,
		String attendanceId,
		String userId,
		String employeeId,
		String employeeName,
		LocalDate workDate,
		LocalTime checkIn,
		LocalTime checkOut,
		AttendanceStatus status,
		String notes,
		Double latitude,
		Double longitude,
		Double accuracyMeters,
		String sourceNetwork) {
}

