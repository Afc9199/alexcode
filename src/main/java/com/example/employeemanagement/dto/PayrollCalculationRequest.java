package com.example.employeemanagement.dto;

import java.time.YearMonth;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PayrollCalculationRequest(
		@NotBlank(message = "User ID is required")
		String userId,
		@NotNull(message = "Month is required")
		@JsonDeserialize(using = YearMonthDeserializer.class)
		YearMonth month) {
}

