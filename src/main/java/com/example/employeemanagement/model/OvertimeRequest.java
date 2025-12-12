package com.example.employeemanagement.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "overtime_requests")
public class OvertimeRequest {

	@Id
	private String id;

	private String overtimeId;

	@Indexed
	private String userId;

	private String employeeId;

	private LocalDate workDate;

	private LocalTime startTime;

	private LocalTime endTime;

	private Double hours;

	private String reason;

	private OvertimeStatus status = OvertimeStatus.PENDING;

	private String managerComment;

	private String decidedBy;

	private Instant decidedAt;

	private Instant createdAt = Instant.now();

	public OvertimeRequest() {
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getOvertimeId() {
		return overtimeId;
	}

	public void setOvertimeId(String overtimeId) {
		this.overtimeId = overtimeId;
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

	public LocalDate getWorkDate() {
		return workDate;
	}

	public void setWorkDate(LocalDate workDate) {
		this.workDate = workDate;
	}

	public LocalTime getStartTime() {
		return startTime;
	}

	public void setStartTime(LocalTime startTime) {
		this.startTime = startTime;
	}

	public LocalTime getEndTime() {
		return endTime;
	}

	public void setEndTime(LocalTime endTime) {
		this.endTime = endTime;
	}

	public Double getHours() {
		return hours;
	}

	public void setHours(Double hours) {
		this.hours = hours;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public OvertimeStatus getStatus() {
		return status;
	}

	public void setStatus(OvertimeStatus status) {
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

