package com.example.employeemanagement.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "payroll_records")
public class PayrollRecord {

	@Id
	private String id;

	@Indexed
	private String userId;

	private String employeeId;

	private LocalDate periodStart;

	private LocalDate periodEnd;

	private YearMonth salaryMonth; // For monthly payroll

	// Legacy fields (kept for backward compatibility)
	private BigDecimal baseSalary = BigDecimal.ZERO;

	private BigDecimal bonus = BigDecimal.ZERO;

	private BigDecimal deductions = BigDecimal.ZERO;

	private BigDecimal netPay = BigDecimal.ZERO;

	// Detailed breakdown fields
	private BigDecimal basicSalary = BigDecimal.ZERO;
	private BigDecimal adjustedBasicSalary = BigDecimal.ZERO;
	private BigDecimal otPayNormal = BigDecimal.ZERO;
	private BigDecimal otPayOffDay = BigDecimal.ZERO;
	private BigDecimal otPayPublicHoliday = BigDecimal.ZERO;
	private BigDecimal totalOtPay = BigDecimal.ZERO;
	private BigDecimal kpiBonus = BigDecimal.ZERO;
	private BigDecimal benefitBonus = BigDecimal.ZERO;
	private BigDecimal grossPay = BigDecimal.ZERO;

	// Deductions
	private BigDecimal epfEmployee = BigDecimal.ZERO;
	private BigDecimal epfEmployer = BigDecimal.ZERO;
	private BigDecimal socsoEmployee = BigDecimal.ZERO;
	private BigDecimal socsoEmployer = BigDecimal.ZERO;
	private BigDecimal eisEmployee = BigDecimal.ZERO;
	private BigDecimal eisEmployer = BigDecimal.ZERO;
	private BigDecimal pcb = BigDecimal.ZERO;
	private BigDecimal totalEmployeeDeductions = BigDecimal.ZERO;
	private BigDecimal totalEmployerContributions = BigDecimal.ZERO;
	private BigDecimal employerCost = BigDecimal.ZERO;

	// Attendance summary
	private Integer totalWorkingDays;
	private Double otHoursNormal;
	private Double otHoursOffDay;
	private Double otHoursPublicHoliday;
	private Integer unpaidLeaveDays;
	private Integer absentDays;

	private String notes;

	private Instant createdAt = Instant.now();

	private Instant updatedAt = Instant.now();

	public PayrollRecord() {
	}

	public PayrollRecord(String id, String userId, LocalDate periodStart, LocalDate periodEnd, BigDecimal baseSalary,
			BigDecimal bonus, BigDecimal deductions, BigDecimal netPay, String notes, Instant createdAt,
			Instant updatedAt) {
		this.id = id;
		this.userId = userId;
		this.periodStart = periodStart;
		this.periodEnd = periodEnd;
		this.baseSalary = baseSalary;
		this.bonus = bonus;
		this.deductions = deductions;
		this.netPay = netPay;
		this.notes = notes;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public LocalDate getPeriodStart() {
		return periodStart;
	}

	public void setPeriodStart(LocalDate periodStart) {
		this.periodStart = periodStart;
	}

	public LocalDate getPeriodEnd() {
		return periodEnd;
	}

	public void setPeriodEnd(LocalDate periodEnd) {
		this.periodEnd = periodEnd;
	}

	public BigDecimal getBaseSalary() {
		return baseSalary;
	}

	public void setBaseSalary(BigDecimal baseSalary) {
		this.baseSalary = baseSalary;
	}

	public BigDecimal getBonus() {
		return bonus;
	}

	public void setBonus(BigDecimal bonus) {
		this.bonus = bonus;
	}

	public BigDecimal getDeductions() {
		return deductions;
	}

	public void setDeductions(BigDecimal deductions) {
		this.deductions = deductions;
	}

	public BigDecimal getNetPay() {
		return netPay;
	}

	public void setNetPay(BigDecimal netPay) {
		this.netPay = netPay;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}

	public String getEmployeeId() {
		return employeeId;
	}

	public void setEmployeeId(String employeeId) {
		this.employeeId = employeeId;
	}

	public YearMonth getSalaryMonth() {
		return salaryMonth;
	}

	public void setSalaryMonth(YearMonth salaryMonth) {
		this.salaryMonth = salaryMonth;
	}

	public BigDecimal getBasicSalary() {
		return basicSalary;
	}

	public void setBasicSalary(BigDecimal basicSalary) {
		this.basicSalary = basicSalary;
	}

	public BigDecimal getAdjustedBasicSalary() {
		return adjustedBasicSalary;
	}

	public void setAdjustedBasicSalary(BigDecimal adjustedBasicSalary) {
		this.adjustedBasicSalary = adjustedBasicSalary;
	}

	public BigDecimal getOtPayNormal() {
		return otPayNormal;
	}

	public void setOtPayNormal(BigDecimal otPayNormal) {
		this.otPayNormal = otPayNormal;
	}

	public BigDecimal getOtPayOffDay() {
		return otPayOffDay;
	}

	public void setOtPayOffDay(BigDecimal otPayOffDay) {
		this.otPayOffDay = otPayOffDay;
	}

	public BigDecimal getOtPayPublicHoliday() {
		return otPayPublicHoliday;
	}

	public void setOtPayPublicHoliday(BigDecimal otPayPublicHoliday) {
		this.otPayPublicHoliday = otPayPublicHoliday;
	}

	public BigDecimal getTotalOtPay() {
		return totalOtPay;
	}

	public void setTotalOtPay(BigDecimal totalOtPay) {
		this.totalOtPay = totalOtPay;
	}

	public BigDecimal getKpiBonus() {
		return kpiBonus;
	}

	public void setKpiBonus(BigDecimal kpiBonus) {
		this.kpiBonus = kpiBonus;
	}

	public BigDecimal getBenefitBonus() {
		return benefitBonus;
	}

	public void setBenefitBonus(BigDecimal benefitBonus) {
		this.benefitBonus = benefitBonus;
	}

	public BigDecimal getGrossPay() {
		return grossPay;
	}

	public void setGrossPay(BigDecimal grossPay) {
		this.grossPay = grossPay;
	}

	public BigDecimal getEpfEmployee() {
		return epfEmployee;
	}

	public void setEpfEmployee(BigDecimal epfEmployee) {
		this.epfEmployee = epfEmployee;
	}

	public BigDecimal getEpfEmployer() {
		return epfEmployer;
	}

	public void setEpfEmployer(BigDecimal epfEmployer) {
		this.epfEmployer = epfEmployer;
	}

	public BigDecimal getSocsoEmployee() {
		return socsoEmployee;
	}

	public void setSocsoEmployee(BigDecimal socsoEmployee) {
		this.socsoEmployee = socsoEmployee;
	}

	public BigDecimal getSocsoEmployer() {
		return socsoEmployer;
	}

	public void setSocsoEmployer(BigDecimal socsoEmployer) {
		this.socsoEmployer = socsoEmployer;
	}

	public BigDecimal getEisEmployee() {
		return eisEmployee;
	}

	public void setEisEmployee(BigDecimal eisEmployee) {
		this.eisEmployee = eisEmployee;
	}

	public BigDecimal getEisEmployer() {
		return eisEmployer;
	}

	public void setEisEmployer(BigDecimal eisEmployer) {
		this.eisEmployer = eisEmployer;
	}

	public BigDecimal getPcb() {
		return pcb;
	}

	public void setPcb(BigDecimal pcb) {
		this.pcb = pcb;
	}

	public BigDecimal getTotalEmployeeDeductions() {
		return totalEmployeeDeductions;
	}

	public void setTotalEmployeeDeductions(BigDecimal totalEmployeeDeductions) {
		this.totalEmployeeDeductions = totalEmployeeDeductions;
	}

	public BigDecimal getTotalEmployerContributions() {
		return totalEmployerContributions;
	}

	public void setTotalEmployerContributions(BigDecimal totalEmployerContributions) {
		this.totalEmployerContributions = totalEmployerContributions;
	}

	public BigDecimal getEmployerCost() {
		return employerCost;
	}

	public void setEmployerCost(BigDecimal employerCost) {
		this.employerCost = employerCost;
	}

	public Integer getTotalWorkingDays() {
		return totalWorkingDays;
	}

	public void setTotalWorkingDays(Integer totalWorkingDays) {
		this.totalWorkingDays = totalWorkingDays;
	}

	public Double getOtHoursNormal() {
		return otHoursNormal;
	}

	public void setOtHoursNormal(Double otHoursNormal) {
		this.otHoursNormal = otHoursNormal;
	}

	public Double getOtHoursOffDay() {
		return otHoursOffDay;
	}

	public void setOtHoursOffDay(Double otHoursOffDay) {
		this.otHoursOffDay = otHoursOffDay;
	}

	public Double getOtHoursPublicHoliday() {
		return otHoursPublicHoliday;
	}

	public void setOtHoursPublicHoliday(Double otHoursPublicHoliday) {
		this.otHoursPublicHoliday = otHoursPublicHoliday;
	}

	public Integer getUnpaidLeaveDays() {
		return unpaidLeaveDays;
	}

	public void setUnpaidLeaveDays(Integer unpaidLeaveDays) {
		this.unpaidLeaveDays = unpaidLeaveDays;
	}

	public Integer getAbsentDays() {
		return absentDays;
	}

	public void setAbsentDays(Integer absentDays) {
		this.absentDays = absentDays;
	}
}

