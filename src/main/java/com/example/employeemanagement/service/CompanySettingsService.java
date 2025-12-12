package com.example.employeemanagement.service;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.employeemanagement.dto.CompanySettingsRequest;
import com.example.employeemanagement.dto.CompanySettingsResponse;
import com.example.employeemanagement.model.CompanySettings;
import com.example.employeemanagement.repository.CompanySettingsRepository;

@Service
public class CompanySettingsService {

	private final CompanySettingsRepository companySettingsRepository;

	public CompanySettingsService(CompanySettingsRepository companySettingsRepository) {
		this.companySettingsRepository = companySettingsRepository;
	}

	public CompanySettingsResponse getSettings() {
		CompanySettings settings = getOrCreateSettings();
		return toResponse(settings);
	}

	public CompanySettingsResponse updateSettings(CompanySettingsRequest request) {
		CompanySettings settings = getOrCreateSettings();
		settings.setWorkStartTime(request.workStartTime());
		settings.setWorkEndTime(request.workEndTime());
		settings.setLateThresholdMinutes(request.lateThresholdMinutes() != null ? request.lateThresholdMinutes() : 0);
		settings.setWorkingDays(request.workingDays());
		// Update company information (allow empty strings to clear the fields)
		settings.setCompanyName(request.companyName() != null ? request.companyName() : "");
		settings.setCompanyAddress(request.companyAddress() != null ? request.companyAddress() : "");
		settings.setUpdatedAt(Instant.now());
		CompanySettings saved = companySettingsRepository.save(settings);
		return toResponse(saved);
	}

	public CompanySettings getOrCreateSettings() {
		List<CompanySettings> allSettings = companySettingsRepository.findAll();
		if (allSettings.isEmpty()) {
			CompanySettings defaultSettings = new CompanySettings();
			defaultSettings.setWorkStartTime(LocalTime.of(9, 0));
			defaultSettings.setWorkEndTime(LocalTime.of(17, 0));
			defaultSettings.setLateThresholdMinutes(0);
			defaultSettings.setWorkingDays(List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"));
			return companySettingsRepository.save(defaultSettings);
		}
		CompanySettings settings = allSettings.get(0);
		// Ensure working days are always populated
		settings.setWorkingDays(settings.getWorkingDays());
		return settings;
	}

	private CompanySettingsResponse toResponse(CompanySettings settings) {
		return new CompanySettingsResponse(
				settings.getId(),
				settings.getWorkStartTime(),
				settings.getWorkEndTime(),
				settings.getLateThresholdMinutes(),
				settings.getWorkingDays(),
				settings.getCompanyName(),
				settings.getCompanyAddress());
	}
}

