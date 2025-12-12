package com.example.employeemanagement.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BenefitCategoryRequest(
		@NotBlank(message = "Benefit category name is required")
		@Size(max = 100, message = "Benefit category name must not exceed 100 characters")
		String name,
		
		@Size(max = 500, message = "Description must not exceed 500 characters")
		String description,
		
		@NotNull(message = "Benefit amount is required")
		@DecimalMin(value = "0.01", message = "Benefit amount must be greater than 0")
		Double benefitAmount) {
}

