package com.example.employeemanagement.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatMessageRequest(
		@NotBlank(message = "Message is required")
		String message) {
}

