package com.example.employeemanagement.dto;

import java.math.BigDecimal;
import java.util.List;

public record KPIPerformanceReportResponse(
		String reportType,
		String department,
		String year,
		int totalRecords,
		List<KPIPerformanceReportItem> items
) {
	public record KPIPerformanceReportItem(
			String employeeId,
			String employeeName,
			String department,
			String kpiId,
			String kpiName,
			BigDecimal targetValue,
			BigDecimal actualValue,
			BigDecimal achievementPercentage,
			String status,
			BigDecimal bonusAmount
	) {}
}

