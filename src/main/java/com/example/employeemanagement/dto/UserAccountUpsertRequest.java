package com.example.employeemanagement.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserAccountUpsertRequest(
		@NotBlank(message = "username is required") String username,
		String password,
		@NotBlank(message = "role is required") String role,
		String fullName,
		@Email(message = "email must be valid") String email,
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
		String passportNumber,
		String taxNumber,
		Integer numberOfChildren,
		Boolean active,
		String nationality,
		String residentStatus,
		String spouseWorking,
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

