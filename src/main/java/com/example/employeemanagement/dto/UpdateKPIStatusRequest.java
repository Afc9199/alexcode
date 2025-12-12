package com.example.employeemanagement.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateKPIStatusRequest(
		@NotNull(message = "Status flag is required")
		Boolean active
) {
}

