package com.example.employeemanagement.model;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "job_postings")
public class JobPosting {

	@Id
	private String id;

	@Indexed
	private String jobPostingId;

	private String jobTitle;

	private String jobDescription;

	private LocalDate createdDate;

	private String status; // "Available" or "Hired"

	private String createdBy; // Admin user ID (MongoDB ID)
	
	private String createdByEmployeeId; // Admin employee ID (human-readable)
	
	private String createdByName; // Admin full name

	private Instant createdAt = Instant.now();

	private Instant updatedAt = Instant.now();

	public JobPosting() {
	}

	public JobPosting(String jobTitle, String jobDescription, LocalDate createdDate, String status, String createdBy) {
		this.jobTitle = jobTitle;
		this.jobDescription = jobDescription;
		this.createdDate = createdDate;
		this.status = status;
		this.createdBy = createdBy;
		this.createdAt = Instant.now();
		this.updatedAt = Instant.now();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getJobPostingId() {
		return jobPostingId;
	}

	public void setJobPostingId(String jobPostingId) {
		this.jobPostingId = jobPostingId;
	}

	public String getJobTitle() {
		return jobTitle;
	}

	public void setJobTitle(String jobTitle) {
		this.jobTitle = jobTitle;
		this.updatedAt = Instant.now();
	}

	public String getJobDescription() {
		return jobDescription;
	}

	public void setJobDescription(String jobDescription) {
		this.jobDescription = jobDescription;
		this.updatedAt = Instant.now();
	}

	public LocalDate getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(LocalDate createdDate) {
		this.createdDate = createdDate;
		this.updatedAt = Instant.now();
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
		this.updatedAt = Instant.now();
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public String getCreatedByEmployeeId() {
		return createdByEmployeeId;
	}

	public void setCreatedByEmployeeId(String createdByEmployeeId) {
		this.createdByEmployeeId = createdByEmployeeId;
	}

	public String getCreatedByName() {
		return createdByName;
	}

	public void setCreatedByName(String createdByName) {
		this.createdByName = createdByName;
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

