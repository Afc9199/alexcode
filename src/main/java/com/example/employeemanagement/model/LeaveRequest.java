package com.example.employeemanagement.model;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "leave_requests")
public class LeaveRequest {

	@Id
	private String id;

	private String leaveId;

	@Indexed
	private String userId;

	private String employeeId;

	private String leaveType;

	private LocalDate startDate;

	private LocalDate endDate;

	private String reason;

	private String supportingDocumentFilename;

	private LeaveStatus status = LeaveStatus.PENDING;

	private String managerComment;

	private String decidedBy;

	private Instant decidedAt;

	private Instant createdAt = Instant.now();

	public LeaveRequest() {
	}

	public LeaveRequest(String id, String leaveId, String userId, String employeeId, String leaveType,
			LocalDate startDate, LocalDate endDate, String reason, String supportingDocumentFilename, LeaveStatus status, String managerComment,
			String decidedBy, Instant decidedAt, Instant createdAt) {
		this.id = id;
		this.leaveId = leaveId;
		this.userId = userId;
		this.employeeId = employeeId;
		this.leaveType = leaveType;
		this.startDate = startDate;
		this.endDate = endDate;
		this.reason = reason;
		this.supportingDocumentFilename = supportingDocumentFilename;
		this.status = status;
		this.managerComment = managerComment;
		this.decidedBy = decidedBy;
		this.decidedAt = decidedAt;
		this.createdAt = createdAt;
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

	public String getLeaveId() {
		return leaveId;
	}

	public void setLeaveId(String leaveId) {
		this.leaveId = leaveId;
	}

	public String getEmployeeId() {
		return employeeId;
	}

	public void setEmployeeId(String employeeId) {
		this.employeeId = employeeId;
	}

	public String getLeaveType() {
		return leaveType;
	}

	public void setLeaveType(String leaveType) {
		this.leaveType = leaveType;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public void setStartDate(LocalDate startDate) {
		this.startDate = startDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public void setEndDate(LocalDate endDate) {
		this.endDate = endDate;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public String getSupportingDocumentFilename() {
		return supportingDocumentFilename;
	}

	public void setSupportingDocumentFilename(String supportingDocumentFilename) {
		this.supportingDocumentFilename = supportingDocumentFilename;
	}

	public LeaveStatus getStatus() {
		return status;
	}

	public void setStatus(LeaveStatus status) {
		this.status = status;
	}

	public String getManagerComment() {
		return managerComment;
	}

	public void setManagerComment(String managerComment) {
		this.managerComment = managerComment;
	}

	public String getDecidedBy() {
		return decidedBy;
	}

	public void setDecidedBy(String decidedBy) {
		this.decidedBy = decidedBy;
	}

	public Instant getDecidedAt() {
		return decidedAt;
	}

	public void setDecidedAt(Instant decidedAt) {
		this.decidedAt = decidedAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}
}

