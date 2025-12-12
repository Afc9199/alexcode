package com.example.employeemanagement.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignKPIRequest(
		@NotBlank(message = "KPI Category ID is required")
		String kpiCategoryId,
		
		String employeeId, // Optional for individual assignment
		
		String departmentName) { // Optional for bulk assignment by department
}

