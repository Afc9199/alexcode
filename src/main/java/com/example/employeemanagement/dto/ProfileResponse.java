package com.example.employeemanagement.dto;

import java.time.LocalDate;

public record ProfileResponse(
		String id,
		String username,
		String employeeId,
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
		String passportNumber,
		String taxNumber,
		Integer numberOfChildren,
		String role,
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

