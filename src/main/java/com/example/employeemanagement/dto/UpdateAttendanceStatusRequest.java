package com.example.employeemanagement.dto;

import com.example.employeemanagement.model.AttendanceStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateAttendanceStatusRequest(
		@NotNull AttendanceStatus status,
		@NotBlank(message = "Notes are required") String notes) {
}

