package com.example.employeemanagement.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignBenefitRequest(
		@NotBlank(message = "Employee ID is required")
		String employeeId,
		
		@NotBlank(message = "Benefit Category ID is required")
		String benefitCategoryId) {
}

