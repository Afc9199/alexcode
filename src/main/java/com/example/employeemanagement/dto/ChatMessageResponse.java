package com.example.employeemanagement.dto;

import java.util.List;

public record ChatMessageResponse(
		String answer,
		List<ChatContextItem> context,
		String model,
		long latencyMillis) {
}

