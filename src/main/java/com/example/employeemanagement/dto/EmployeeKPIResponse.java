package com.example.employeemanagement.dto;

import java.time.Instant;
import java.time.LocalDate;

public record EmployeeKPIResponse(
		String id,
		String userId,
		String employeeId,
		String employeeName,
		String kpiCategoryId,
		String kpiId,
		String kpiName,
		String description,
		String measurableValue,
		LocalDate dueDate,
		Double bonusAmount,
		Double currentProgressValue,
		Double progressPercentage,
		String evidenceFilename,
		String evidenceOriginalName,
		Instant evidenceUploadedAt,
		Instant assignedAt,
		Instant updatedAt,
		String status,
		String evaluationNote,
		Instant evaluatedAt) {
}

