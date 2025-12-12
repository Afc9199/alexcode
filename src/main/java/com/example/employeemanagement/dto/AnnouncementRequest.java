package com.example.employeemanagement.dto;

import java.time.LocalDate;

import com.example.employeemanagement.model.AnnouncementAudience;
import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AnnouncementRequest(
		String announcementId,
		@NotBlank(message = "Title is required") String title,
		@NotBlank(message = "Message is required") String message,
		@NotNull AnnouncementAudience audience,
		@NotBlank(message = "CreatedBy is required") String createdBy,
		@JsonFormat(pattern = "yyyy-MM-dd") LocalDate expiresOn,
		boolean pinned) {
}

