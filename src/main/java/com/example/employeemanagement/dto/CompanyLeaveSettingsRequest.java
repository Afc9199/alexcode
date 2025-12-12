package com.example.employeemanagement.dto;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotNull;

public record CompanyLeaveSettingsRequest(
		@NotNull List<LeaveTypeDto> availableLeaveTypes,
		List<PublicHolidayDto> publicHolidays) {

	public record LeaveTypeDto(
			@NotNull String name,
			int daysAllowed) {
	}

	public record PublicHolidayDto(
			@NotNull @JsonFormat(pattern = "yyyy-MM-dd") LocalDate date,
			@NotNull String name) {
	}
}

