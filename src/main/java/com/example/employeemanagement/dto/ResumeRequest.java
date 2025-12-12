package com.example.employeemanagement.dto;

import jakarta.validation.constraints.NotBlank;

public record ResumeRequest(
		@NotBlank(message = "Job posting ID is required")
		String jobPostingId,

		@NotBlank(message = "Candidate name is required")
		String candidateName,

		@NotBlank(message = "Candidate email is required")
		String candidateEmail,

		@NotBlank(message = "Candidate contact number is required")
		String candidateContactNumber
) {
}

