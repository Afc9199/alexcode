package com.example.employeemanagement.model;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "employee_kpis")
public class EmployeeKPI {

	@Id
	private String id;

	@Indexed
	private String userId;

	@Indexed
	private String employeeId;

	@Indexed
	private String kpiId; // Human-readable KPI ID (e.g., K001)

	@Indexed
	private String kpiCategoryId; // MongoDB document ID

	private String kpiName;

	private String description;

	private String measurableValue;

	private LocalDate dueDate;

	private Double bonusAmount;

	private Double currentProgressValue = 0.0;

	private Double progressPercentage = 0.0;

	private String evidenceFilename;

	private String evidenceOriginalName;

	private Instant evidenceUploadedAt;

	private String status = "PENDING";

	private String evaluationNote;

	private Instant evaluatedAt;

	private Instant assignedAt = Instant.now();

	private Instant updatedAt = Instant.now();

	public EmployeeKPI() {
	}

	public EmployeeKPI(String userId, String employeeId, String kpiCategoryId, String kpiId,
			String kpiName, String description, String measurableValue, LocalDate dueDate, Double bonusAmount) {
		this.userId = userId;
		this.employeeId = employeeId;
		this.kpiCategoryId = kpiCategoryId;
		this.kpiId = kpiId;
		this.kpiName = kpiName;
		this.description = description;
		this.measurableValue = measurableValue;
		this.dueDate = dueDate;
		this.bonusAmount = bonusAmount;
		this.assignedAt = Instant.now();
		this.updatedAt = Instant.now();
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

	public String getEmployeeId() {
		return employeeId;
	}

	public void setEmployeeId(String employeeId) {
		this.employeeId = employeeId;
	}

	public String getKpiId() {
		return kpiId;
	}

	public void setKpiId(String kpiId) {
		this.kpiId = kpiId;
	}

	public String getKpiCategoryId() {
		return kpiCategoryId;
	}

	public void setKpiCategoryId(String kpiCategoryId) {
		this.kpiCategoryId = kpiCategoryId;
	}

	public String getKpiName() {
		return kpiName;
	}

	public void setKpiName(String kpiName) {
		this.kpiName = kpiName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getMeasurableValue() {
		return measurableValue;
	}

	public void setMeasurableValue(String measurableValue) {
		this.measurableValue = measurableValue;
	}

	public LocalDate getDueDate() {
		return dueDate;
	}

	public void setDueDate(LocalDate dueDate) {
		this.dueDate = dueDate;
	}

	public Double getBonusAmount() {
		return bonusAmount;
	}

	public void setBonusAmount(Double bonusAmount) {
		this.bonusAmount = bonusAmount;
	}

	public Double getCurrentProgressValue() {
		return currentProgressValue;
	}

	public void setCurrentProgressValue(Double currentProgressValue) {
		this.currentProgressValue = currentProgressValue;
		this.updatedAt = Instant.now();
	}

	public Double getProgressPercentage() {
		return progressPercentage;
	}

	public void setProgressPercentage(Double progressPercentage) {
		this.progressPercentage = progressPercentage;
		this.updatedAt = Instant.now();
	}

	public String getEvidenceFilename() {
		return evidenceFilename;
	}

	public void setEvidenceFilename(String evidenceFilename) {
		this.evidenceFilename = evidenceFilename;
	}

	public String getEvidenceOriginalName() {
		return evidenceOriginalName;
	}

	public void setEvidenceOriginalName(String evidenceOriginalName) {
		this.evidenceOriginalName = evidenceOriginalName;
	}

	public Instant getEvidenceUploadedAt() {
		return evidenceUploadedAt;
	}

	public void setEvidenceUploadedAt(Instant evidenceUploadedAt) {
		this.evidenceUploadedAt = evidenceUploadedAt;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
		this.updatedAt = Instant.now();
	}

	public String getEvaluationNote() {
		return evaluationNote;
	}

	public void setEvaluationNote(String evaluationNote) {
		this.evaluationNote = evaluationNote;
		this.updatedAt = Instant.now();
	}

	public Instant getEvaluatedAt() {
		return evaluatedAt;
	}

	public void setEvaluatedAt(Instant evaluatedAt) {
		this.evaluatedAt = evaluatedAt;
		this.updatedAt = Instant.now();
	}

	public Instant getAssignedAt() {
		return assignedAt;
	}

	public void setAssignedAt(Instant assignedAt) {
		this.assignedAt = assignedAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}
}

