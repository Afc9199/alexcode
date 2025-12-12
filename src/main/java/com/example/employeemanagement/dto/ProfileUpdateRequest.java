package com.example.employeemanagement.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ProfileUpdateRequest(
		@NotBlank(message = "Full name is required") String fullName,
		@Email(message = "Email must be valid") String email,
		@NotBlank(message = "Department is required") String department,
		@NotBlank(message = "Job title is required") String jobTitle,
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
		String nationality,
		String residentStatus,
		String spouseWorking,
		LocalDate dateOfBirth,
		String socsoNumber,
		LocalDate dateOfHire,
		String emergencyContactName,
		String emergencyContactRelationship,
		String emergencyContactNumber) {
}

