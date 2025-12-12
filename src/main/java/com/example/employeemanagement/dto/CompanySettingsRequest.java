package com.example.employeemanagement.dto;

import java.time.LocalTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CompanySettingsRequest(
		@NotNull @JsonFormat(pattern = "HH:mm") LocalTime workStartTime,
		@NotNull @JsonFormat(pattern = "HH:mm") LocalTime workEndTime,
		Integer lateThresholdMinutes,
		@NotEmpty List<String> workingDays,
		@Size(max = 30, message = "Company name cannot exceed 30 characters") String companyName,
		@Size(max = 250, message = "Company address cannot exceed 250 characters") String companyAddress) {
}

