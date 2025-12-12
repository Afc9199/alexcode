package com.example.employeemanagement.dto;

import java.time.Instant;
import java.time.LocalDate;

public record KPIResponse(
		String id,
		String kpiId,
		String name,
		String description,
		String measurableValue,
		LocalDate dueDate,
		Double bonusAmount,
		Boolean active,
		Instant createdAt,
		Instant updatedAt
) {
}

