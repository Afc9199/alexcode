package com.example.employeemanagement.dto;

import java.math.BigDecimal;
import java.util.List;

public record PayrollReportResponse(
		String reportType,
		String month,
		String employeeId,
		String department,
		boolean includeStatutory,
		int totalEmployees,
		List<PayrollReportItem> items,
		StatutorySummary statutorySummary
) {
	public record PayrollReportItem(
			String employeeId,
			String employeeName,
			String department,
			BigDecimal basicSalary,
			BigDecimal adjustedBasicSalary,
			BigDecimal totalOtPay,
			BigDecimal kpiBonus,
			BigDecimal benefitBonus,
			BigDecimal grossPay,
			BigDecimal epfEmployee,
			BigDecimal epfEmployer,
			BigDecimal socsoEmployee,
			BigDecimal socsoEmployer,
			BigDecimal eisEmployee,
			BigDecimal eisEmployer,
			BigDecimal pcb,
			BigDecimal totalEmployeeDeductions,
			BigDecimal netPay
	) {}
	
	public record StatutorySummary(
			BigDecimal totalEpfEmployee,
			BigDecimal totalEpfEmployer,
			BigDecimal totalSocsoEmployee,
			BigDecimal totalSocsoEmployer,
			BigDecimal totalEisEmployee,
			BigDecimal totalEisEmployer,
			BigDecimal totalPcb
	) {}
}

