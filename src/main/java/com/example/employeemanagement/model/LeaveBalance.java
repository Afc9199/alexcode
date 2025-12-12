package com.example.employeemanagement.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "leave_balances")
public class LeaveBalance {

	@Id
	private String id;

	@Indexed
	private String employeeId;

	@Indexed
	private String userId;

	private String leaveType;

	private int totalDays; // Total days allocated for this leave type

	private int usedDays; // Days already used

	private int remainingDays; // Remaining days (totalDays - usedDays)

	private Instant lastUpdated = Instant.now();

	public LeaveBalance() {
	}

	public LeaveBalance(String employeeId, String userId, String leaveType, int totalDays, int usedDays) {
		this.employeeId = employeeId;
		this.userId = userId;
		this.leaveType = leaveType;
		this.totalDays = totalDays;
		this.usedDays = usedDays;
		this.remainingDays = totalDays - usedDays;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getEmployeeId() {
		return employeeId;
	}

	public void setEmployeeId(String employeeId) {
		this.employeeId = employeeId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getLeaveType() {
		return leaveType;
	}

	public void setLeaveType(String leaveType) {
		this.leaveType = leaveType;
	}

	public int getTotalDays() {
		return totalDays;
	}

	public void setTotalDays(int totalDays) {
		this.totalDays = totalDays;
		this.remainingDays = this.totalDays - this.usedDays;
	}

	public int getUsedDays() {
		return usedDays;
	}

	public void setUsedDays(int usedDays) {
		this.usedDays = usedDays;
		this.remainingDays = this.totalDays - this.usedDays;
	}

	public int getRemainingDays() {
		return remainingDays;
	}

	public void setRemainingDays(int remainingDays) {
		this.remainingDays = remainingDays;
	}

	public Instant getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(Instant lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	// Helper method to add used days
	public void addUsedDays(int days) {
		this.usedDays += days;
		this.remainingDays = this.totalDays - this.usedDays;
		this.lastUpdated = Instant.now();
	}

	// Helper method to subtract used days (when leave is rejected or cancelled)
	public void subtractUsedDays(int days) {
		this.usedDays = Math.max(0, this.usedDays - days);
		this.remainingDays = this.totalDays - this.usedDays;
		this.lastUpdated = Instant.now();
	}
}

