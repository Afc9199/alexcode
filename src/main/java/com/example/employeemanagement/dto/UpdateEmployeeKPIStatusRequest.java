package com.example.employeemanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateEmployeeKPIStatusRequest(
		@NotBlank(message = "Status is required")
		String status,
		@Size(max = 500, message = "Evaluation note must not exceed 500 characters")
		String evaluationNote) {
}


