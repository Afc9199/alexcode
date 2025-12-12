package com.example.employeemanagement.dto;

import java.time.Instant;
import java.time.LocalDate;

public record JobPostingResponse(
		String id,
		String jobPostingId,
		String jobTitle,
		String jobDescription,
		LocalDate createdDate,
		String status,
		String createdBy,
		String createdByEmployeeId,
		String createdByName,
		Instant createdAt,
		Instant updatedAt
) {
}

