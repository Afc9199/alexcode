package com.example.employeemanagement.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "attendance_records")
public class AttendanceRecord {

	@Id
	private String id;

	@Indexed
	private String attendanceId;

	@Indexed
	private String userId;

	@Indexed
	private String employeeId;

	private LocalDate workDate;

	private LocalTime checkIn;

	private LocalTime checkOut;

	private AttendanceStatus status = AttendanceStatus.PRESENT;

	private String notes;

	private Double latitude;

	private Double longitude;

	private Double accuracyMeters;

	private String sourceNetwork;

	private Instant createdAt = Instant.now();

	private Instant updatedAt = Instant.now();

	public AttendanceRecord() {
	}

	public AttendanceRecord(String id, String attendanceId, String userId, String employeeId, LocalDate workDate, LocalTime checkIn, LocalTime checkOut,
			AttendanceStatus status, String notes, Instant createdAt, Instant updatedAt) {
		this.id = id;
		this.attendanceId = attendanceId;
		this.userId = userId;
		this.employeeId = employeeId;
		this.workDate = workDate;
		this.checkIn = checkIn;
		this.checkOut = checkOut;
		this.status = status;
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

	public String getAttendanceId() {
		return attendanceId;
	}

	public void setAttendanceId(String attendanceId) {
		this.attendanceId = attendanceId;
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

	public LocalTime getCheckIn() {
		return checkIn;
	}

	public void setCheckIn(LocalTime checkIn) {
		this.checkIn = checkIn;
	}

	public LocalTime getCheckOut() {
		return checkOut;
	}

	public void setCheckOut(LocalTime checkOut) {
		this.checkOut = checkOut;
	}

	public AttendanceStatus getStatus() {
		return status;
	}

	public void setStatus(AttendanceStatus status) {
		this.status = status;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public Double getLatitude() {
		return latitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	public Double getAccuracyMeters() {
		return accuracyMeters;
	}

	public void setAccuracyMeters(Double accuracyMeters) {
		this.accuracyMeters = accuracyMeters;
	}

	public String getSourceNetwork() {
		return sourceNetwork;
	}

	public void setSourceNetwork(String sourceNetwork) {
		this.sourceNetwork = sourceNetwork;
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

