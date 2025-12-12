package com.example.employeemanagement.dto;

import java.util.List;

public record BenefitsReportResponse(
		String reportType,
		String benefitCategoryId,
		String department,
		String year,
		int totalRecords,
		List<BenefitsReportItem> items
) {
	public record BenefitsReportItem(
			String employeeId,
			String employeeName,
			String department,
			String benefitName,
			String benefitCategory,
			String status,
			String assignedDate
	) {}
}

