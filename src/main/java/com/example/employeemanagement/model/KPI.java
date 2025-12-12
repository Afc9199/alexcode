package com.example.employeemanagement.model;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "kpis")
public class KPI {

	@Id
	private String id;

	@Indexed
	private String kpiId;

	@Indexed(unique = true)
	private String name;

	private String description;

	private String measurableValue;

	private LocalDate dueDate;

	private Double bonusAmount;

	private Boolean active = true;

	private Instant createdAt = Instant.now();

	private Instant updatedAt = Instant.now();

	public KPI() {
	}

	public KPI(String name, String description) {
		this.name = name;
		this.description = description;
		this.createdAt = Instant.now();
		this.updatedAt = Instant.now();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getKpiId() {
		return kpiId;
	}

	public void setKpiId(String kpiId) {
		this.kpiId = kpiId;
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

	public String getMeasurableValue() {
		return measurableValue;
	}

	public void setMeasurableValue(String measurableValue) {
		this.measurableValue = measurableValue;
		this.updatedAt = Instant.now();
	}

	public LocalDate getDueDate() {
		return dueDate;
	}

	public void setDueDate(LocalDate dueDate) {
		this.dueDate = dueDate;
		this.updatedAt = Instant.now();
	}

	public Double getBonusAmount() {
		return bonusAmount;
	}

	public void setBonusAmount(Double bonusAmount) {
		this.bonusAmount = bonusAmount;
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

