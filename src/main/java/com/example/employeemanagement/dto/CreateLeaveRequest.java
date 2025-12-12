package com.example.employeemanagement.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateLeaveRequest(
		@NotBlank(message = "UserId is required") String userId,
		@NotBlank(message = "Leave type is required") String leaveType,
		@NotNull @FutureOrPresent @JsonFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
		@NotNull @FutureOrPresent @JsonFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
		@NotBlank(message = "Reason is required") String reason) {
}

