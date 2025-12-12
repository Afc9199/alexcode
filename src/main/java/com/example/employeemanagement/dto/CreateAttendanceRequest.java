package com.example.employeemanagement.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAttendanceRequest(
		@NotBlank(message = "UserId is required") String userId,
		@NotNull @JsonFormat(pattern = "yyyy-MM-dd") LocalDate workDate,
		@NotNull @JsonFormat(pattern = "HH:mm") LocalTime checkIn,
		@JsonFormat(pattern = "HH:mm") LocalTime checkOut,
		@NotNull Double latitude,
		@NotNull Double longitude,
		@NotNull Double accuracyMeters,
		String notes) {
}

