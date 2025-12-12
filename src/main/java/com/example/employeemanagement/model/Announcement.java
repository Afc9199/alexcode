package com.example.employeemanagement.model;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "announcements")
public class Announcement {

	@Id
	private String id;

	private String title;

	private String message;

	private AnnouncementAudience audience = AnnouncementAudience.ALL;

	private String createdBy;

	private Instant createdAt = Instant.now();

	private LocalDate expiresOn;

	private boolean pinned;

	public Announcement() {
	}

	public Announcement(String id, String title, String message, AnnouncementAudience audience, String createdBy,
			Instant createdAt, LocalDate expiresOn, boolean pinned) {
		this.id = id;
		this.title = title;
		this.message = message;
		this.audience = audience;
		this.createdBy = createdBy;
		this.createdAt = createdAt;
		this.expiresOn = expiresOn;
		this.pinned = pinned;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public AnnouncementAudience getAudience() {
		return audience;
	}

	public void setAudience(AnnouncementAudience audience) {
		this.audience = audience;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDate getExpiresOn() {
		return expiresOn;
	}

	public void setExpiresOn(LocalDate expiresOn) {
		this.expiresOn = expiresOn;
	}

	public boolean isPinned() {
		return pinned;
	}

	public void setPinned(boolean pinned) {
		this.pinned = pinned;
	}
}

