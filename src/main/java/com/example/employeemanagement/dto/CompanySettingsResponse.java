package com.example.employeemanagement.dto;

import java.time.LocalTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

public record CompanySettingsResponse(
		String id,
		@JsonFormat(pattern = "HH:mm") LocalTime workStartTime,
		@JsonFormat(pattern = "HH:mm") LocalTime workEndTime,
		Integer lateThresholdMinutes,
		List<String> workingDays,
		String companyName,
		String companyAddress) {
}

