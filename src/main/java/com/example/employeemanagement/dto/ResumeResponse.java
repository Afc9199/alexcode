package com.example.employeemanagement.dto;

import java.time.Instant;
import java.time.LocalDate;

public record ResumeResponse(
		String id,
		String resumeId,
		String jobPostingId,
		String candidateName,
		String candidateEmail,
		String candidateContactNumber,
		String resumeFilename,
		String resumeOriginalName,
		LocalDate uploadDate,
		Instant createdAt,
		Instant updatedAt
) {
}

