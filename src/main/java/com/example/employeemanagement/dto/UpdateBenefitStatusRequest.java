package com.example.employeemanagement.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateBenefitStatusRequest(
		@NotNull(message = "Active status is required")
		Boolean active) {
}


