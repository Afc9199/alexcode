package com.example.employeemanagement.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record JobPostingRequest(
		@NotBlank(message = "Job title is required")
		@Size(max = 250, message = "Job title must not exceed 250 characters")
		String jobTitle,

		String jobDescription, // No limit

		@NotNull(message = "Created date is required")
		LocalDate createdDate,

		@NotBlank(message = "Status is required")
		String status // "Available" or "Full"
) {
}

