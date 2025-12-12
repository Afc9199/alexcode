package com.example.employeemanagement.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.example.employeemanagement.model.AttendanceStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminAttendanceRequest(
		@NotBlank(message = "Employee ID is required") String employeeId,
		@NotNull @JsonFormat(pattern = "yyyy-MM-dd") LocalDate workDate,
		@JsonFormat(pattern = "[HH:mm:ss][HH:mm]") LocalTime checkIn,
		@JsonFormat(pattern = "[HH:mm:ss][HH:mm]") LocalTime checkOut,
		@NotNull AttendanceStatus status,
		String notes) {
}

