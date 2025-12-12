package com.example.employeemanagement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PayrollResponse(
		String id,
		String userId,
		LocalDate periodStart,
		LocalDate periodEnd,
		BigDecimal baseSalary,
		BigDecimal bonus,
		BigDecimal deductions,
		BigDecimal netPay,
		String notes) {
}

