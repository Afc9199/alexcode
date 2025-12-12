package com.example.employeemanagement.dto;

import java.time.Instant;

public record EmployeeBenefitResponse(
		String id,
		String userId,
		String employeeId,
		String employeeName,
		String benefitCategoryId,
		String benefitId,
		String benefitName,
		Double benefitAmount,
		Instant assignedAt,
		Instant updatedAt) {
}

