package com.example.employeemanagement.model;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "resumes")
public class Resume {

	@Id
	private String id;

	@Indexed
	private String resumeId;

	@Indexed
	private String jobPostingId; // Reference to JobPosting

	private String candidateName;

	private String candidateEmail;

	private String candidateContactNumber;

	private String resumeFilename; // Stored filename

	private String resumeOriginalName; // Original filename

	private LocalDate uploadDate;

	private Instant createdAt = Instant.now();

	private Instant updatedAt = Instant.now();

	public Resume() {
	}

	public Resume(String jobPostingId, String candidateName, String candidateEmail, String candidateContactNumber,
			String resumeFilename, String resumeOriginalName, LocalDate uploadDate) {
		this.jobPostingId = jobPostingId;
		this.candidateName = candidateName;
		this.candidateEmail = candidateEmail;
		this.candidateContactNumber = candidateContactNumber;
		this.resumeFilename = resumeFilename;
		this.resumeOriginalName = resumeOriginalName;
		this.uploadDate = uploadDate;
		this.createdAt = Instant.now();
		this.updatedAt = Instant.now();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getResumeId() {
		return resumeId;
	}

	public void setResumeId(String resumeId) {
		this.resumeId = resumeId;
	}

	public String getJobPostingId() {
		return jobPostingId;
	}

	public void setJobPostingId(String jobPostingId) {
		this.jobPostingId = jobPostingId;
		this.updatedAt = Instant.now();
	}

	public String getCandidateName() {
		return candidateName;
	}

	public void setCandidateName(String candidateName) {
		this.candidateName = candidateName;
		this.updatedAt = Instant.now();
	}

	public String getCandidateEmail() {
		return candidateEmail;
	}

	public void setCandidateEmail(String candidateEmail) {
		this.candidateEmail = candidateEmail;
		this.updatedAt = Instant.now();
	}

	public String getCandidateContactNumber() {
		return candidateContactNumber;
	}

	public void setCandidateContactNumber(String candidateContactNumber) {
		this.candidateContactNumber = candidateContactNumber;
		this.updatedAt = Instant.now();
	}

	public String getResumeFilename() {
		return resumeFilename;
	}

	public void setResumeFilename(String resumeFilename) {
		this.resumeFilename = resumeFilename;
		this.updatedAt = Instant.now();
	}

	public String getResumeOriginalName() {
		return resumeOriginalName;
	}

	public void setResumeOriginalName(String resumeOriginalName) {
		this.resumeOriginalName = resumeOriginalName;
		this.updatedAt = Instant.now();
	}

	public LocalDate getUploadDate() {
		return uploadDate;
	}

	public void setUploadDate(LocalDate uploadDate) {
		this.uploadDate = uploadDate;
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

