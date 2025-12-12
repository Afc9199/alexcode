package com.example.employeemanagement.dto;

import java.time.LocalDate;
import java.util.List;

public record CompanyLeaveSettingsResponse(
		List<LeaveTypeDto> availableLeaveTypes,
		List<PublicHolidayDto> publicHolidays) {

	public record LeaveTypeDto(String name, int daysAllowed) {
	}

	public record PublicHolidayDto(LocalDate date, String name) {
	}
}

