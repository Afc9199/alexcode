package com.example.employeemanagement.dto;

import java.time.Instant;

public record BenefitCategoryResponse(
		String id,
		String benefitId,
		String name,
		String description,
		Double benefitAmount,
		Boolean active,
		Instant createdAt,
		Instant updatedAt) {
}

