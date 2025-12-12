package com.example.employeemanagement.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "benefit_categories")
public class BenefitCategory {

	@Id
	private String id;

	@Indexed
	private String benefitId;

	@Indexed(unique = true)
	private String name;

	private String description;

	private Double benefitAmount;

	private Boolean active = true;

	private Instant createdAt = Instant.now();

	private Instant updatedAt = Instant.now();

	public BenefitCategory() {
	}

	public BenefitCategory(String name, String description, Double benefitAmount) {
		this.name = name;
		this.description = description;
		this.benefitAmount = benefitAmount;
		this.createdAt = Instant.now();
		this.updatedAt = Instant.now();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getBenefitId() {
		return benefitId;
	}

	public void setBenefitId(String benefitId) {
		this.benefitId = benefitId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
		this.updatedAt = Instant.now();
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
		this.updatedAt = Instant.now();
	}

	public Double getBenefitAmount() {
		return benefitAmount;
	}

	public void setBenefitAmount(Double benefitAmount) {
		this.benefitAmount = benefitAmount;
		this.updatedAt = Instant.now();
	}

	public Boolean getActive() {
		return active == null ? Boolean.TRUE : active;
	}

	public void setActive(Boolean active) {
		this.active = active;
		this.updatedAt = Instant.now();
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
}

