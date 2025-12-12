package com.example.employeemanagement.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.example.employeemanagement.model.AnnouncementAudience;

public record AnnouncementResponse(
		String id,
		String title,
		String message,
		AnnouncementAudience audience,
		String createdBy,
		Instant createdAt,
		LocalDate expiresOn,
		boolean pinned) {
}

