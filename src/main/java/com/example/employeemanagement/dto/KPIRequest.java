package com.example.employeemanagement.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record KPIRequest(
		@NotBlank(message = "KPI name is required")
		@Size(max = 100, message = "KPI name must not exceed 100 characters")
		String name,

		@Size(max = 500, message = "Description must not exceed 500 characters")
		String description,

		@NotBlank(message = "Measurable value is required")
		@Size(max = 100, message = "Measurable value must not exceed 100 characters")
		String measurableValue,

		@NotNull(message = "Due date is required")
		LocalDate dueDate,

		@NotNull(message = "Bonus amount is required")
		@DecimalMin(value = "0.01", message = "Bonus amount must be greater than 0")
		Double bonusAmount
) {
}

