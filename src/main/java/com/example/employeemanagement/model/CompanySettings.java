package com.example.employeemanagement.model;

import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "company_settings")
public class CompanySettings {

	@Id
	private String id;

	private LocalTime workStartTime = LocalTime.of(9, 0); // 9:00 AM

	private LocalTime workEndTime = LocalTime.of(17, 0); // 5:00 PM

	private Integer lateThresholdMinutes = 0; // Minutes after start time to consider late

	private List<String> workingDays = new ArrayList<>(DEFAULT_WORKING_DAYS);

	private String companyName = "";

	private String companyAddress = "";

	private Instant createdAt = Instant.now();

	private Instant updatedAt = Instant.now();

	private static final List<String> DEFAULT_WORKING_DAYS = Arrays.asList(
			"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY");

	public CompanySettings() {
	}

	public CompanySettings(String id, LocalTime workStartTime, LocalTime workEndTime, Integer lateThresholdMinutes,
			Instant createdAt, Instant updatedAt) {
		this.id = id;
		this.workStartTime = workStartTime;
		this.workEndTime = workEndTime;
		this.lateThresholdMinutes = lateThresholdMinutes;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public LocalTime getWorkStartTime() {
		return workStartTime;
	}

	public void setWorkStartTime(LocalTime workStartTime) {
		this.workStartTime = workStartTime;
	}

	public LocalTime getWorkEndTime() {
		return workEndTime;
	}

	public void setWorkEndTime(LocalTime workEndTime) {
		this.workEndTime = workEndTime;
	}

	public Integer getLateThresholdMinutes() {
		return lateThresholdMinutes;
	}

	public void setLateThresholdMinutes(Integer lateThresholdMinutes) {
		this.lateThresholdMinutes = lateThresholdMinutes;
	}

	public List<String> getWorkingDays() {
		if (workingDays == null || workingDays.isEmpty()) {
			workingDays = new ArrayList<>(DEFAULT_WORKING_DAYS);
		}
		return workingDays;
	}

	public void setWorkingDays(List<String> workingDays) {
		if (workingDays == null || workingDays.isEmpty()) {
			this.workingDays = new ArrayList<>(DEFAULT_WORKING_DAYS);
			return;
		}
		this.workingDays = new ArrayList<>(workingDays);
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}

	public String getCompanyName() {
		return companyName;
	}

	public void setCompanyName(String companyName) {
		this.companyName = companyName != null ? companyName : "";
	}

	public String getCompanyAddress() {
		return companyAddress;
	}

	public void setCompanyAddress(String companyAddress) {
		this.companyAddress = companyAddress != null ? companyAddress : "";
	}
}

