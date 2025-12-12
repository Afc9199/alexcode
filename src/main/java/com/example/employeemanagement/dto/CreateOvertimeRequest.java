package com.example.employeemanagement.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateOvertimeRequest(
		@NotBlank(message = "UserId is required") String userId,
		@NotNull @JsonFormat(pattern = "yyyy-MM-dd") LocalDate workDate,
		@NotNull @JsonFormat(pattern = "HH:mm") LocalTime startTime,
		@NotNull @JsonFormat(pattern = "HH:mm") LocalTime endTime,
		@NotBlank(message = "Reason is required") String reason) {
}

