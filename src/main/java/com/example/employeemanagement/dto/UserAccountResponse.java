package com.example.employeemanagement.dto;

import java.time.Instant;
import java.time.LocalDate;

public record UserAccountResponse(
		String id,
		String username,
		String employeeId,
		String role,
		boolean active,
		String fullName,
		String email,
		String department,
		String jobTitle,
		Double basicSalary,
		String contactNumber,
		String gender,
		Integer age,
		String race,
		String religion,
		String address,
		String maritalStatus,
		String bankName,
		String bankAccountNumber,
		String epfNumber,
		String icNumber,
		String taxNumber,
		Integer numberOfChildren,
		Instant createdAt,
		String nationality,
		String residentStatus,
		String spouseWorking,
		String passportNumber,
		// Compliance & Key Identity Information
		LocalDate dateOfBirth,
		String socsoNumber,
		// Position & Employment Information
		LocalDate dateOfHire,
		Integer probationPeriodLength,
		String employmentType,
		String reportingManagerId,
		String location,
		// Emergency Contact
		String emergencyContactName,
		String emergencyContactRelationship,
		String emergencyContactNumber) {
}

