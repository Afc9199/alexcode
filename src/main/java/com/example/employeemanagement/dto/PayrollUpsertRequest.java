package com.example.employeemanagement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PayrollUpsertRequest(
		String payrollId,
		@NotBlank(message = "UserId is required") String userId,
		@NotNull @JsonFormat(pattern = "yyyy-MM-dd") LocalDate periodStart,
		@NotNull @JsonFormat(pattern = "yyyy-MM-dd") LocalDate periodEnd,
		@NotNull @DecimalMin(value = "0.0") BigDecimal baseSalary,
		@NotNull @DecimalMin(value = "0.0") BigDecimal bonus,
		@NotNull @DecimalMin(value = "0.0") BigDecimal deductions,
		String notes) {
}

