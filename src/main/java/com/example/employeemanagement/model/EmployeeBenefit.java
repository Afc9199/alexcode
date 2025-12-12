package com.example.employeemanagement.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "employee_benefits")
public class EmployeeBenefit {

	@Id
	private String id;

	@Indexed
	private String userId;

	@Indexed
	private String employeeId;

	@Indexed
	private String benefitCategoryId;

	private String benefitId; // Human-readable Benefit ID

	private String benefitName;

	private Double benefitAmount;

	private Instant assignedAt = Instant.now();

	private Instant updatedAt = Instant.now();

	public EmployeeBenefit() {
	}

	public EmployeeBenefit(String userId, String employeeId, String benefitCategoryId, String benefitId,
			String benefitName, Double benefitAmount) {
		this.userId = userId;
		this.employeeId = employeeId;
		this.benefitCategoryId = benefitCategoryId;
		this.benefitId = benefitId;
		this.benefitName = benefitName;
		this.benefitAmount = benefitAmount;
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

	public String getBenefitCategoryId() {
		return benefitCategoryId;
	}

	public void setBenefitCategoryId(String benefitCategoryId) {
		this.benefitCategoryId = benefitCategoryId;
	}

	public String getBenefitId() {
		return benefitId;
	}

	public void setBenefitId(String benefitId) {
		this.benefitId = benefitId;
	}

	public String getBenefitName() {
		return benefitName;
	}

	public void setBenefitName(String benefitName) {
		this.benefitName = benefitName;
	}

	public Double getBenefitAmount() {
		return benefitAmount;
	}

	public void setBenefitAmount(Double benefitAmount) {
		this.benefitAmount = benefitAmount;
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

