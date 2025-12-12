package com.example.employeemanagement.dto;

import java.math.BigDecimal;
import java.time.YearMonth;

public record PayslipResponse(
		String payrollId,
		String userId,
		String employeeId,
		String employeeName,
		String icNumber,
		String epfNumber,
		String socsoNumber,
		String taxNumber,
		YearMonth salaryMonth,
		
		// Company Information
		String companyName,
		String companyAddress,
		
		// Attendance Summary
		int totalWorkingDays,
		double otHoursNormal,
		double otHoursOffDay,
		double otHoursPublicHoliday,
		int unpaidLeaveDays,
		int absentDays,
		
		// Earnings Breakdown
		BigDecimal basicSalary,
		BigDecimal adjustedBasicSalary,
		BigDecimal otPayNormal,
		BigDecimal otPayOffDay,
		BigDecimal otPayPublicHoliday,
		BigDecimal totalOtPay,
		BigDecimal kpiBonus,
		BigDecimal benefitBonus,
		BigDecimal grossPay,
		
		// Deduction Breakdown
		BigDecimal epfEmployee,
		BigDecimal epfEmployer,
		BigDecimal socsoEmployee,
		BigDecimal socsoEmployer,
		BigDecimal eisEmployee,
		BigDecimal eisEmployer,
		BigDecimal pcb,
		BigDecimal totalEmployeeDeductions,
		
		// Employer Contributions
		BigDecimal totalEmployerContributions,
		
		// Net Pay
		BigDecimal netPay,
		BigDecimal employerCost) {
}

