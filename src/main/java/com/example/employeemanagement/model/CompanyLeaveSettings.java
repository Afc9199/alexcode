package com.example.employeemanagement.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "company_leave_settings")
public class CompanyLeaveSettings {

	@Id
	private String id;

	private List<LeaveTypeConfig> availableLeaveTypes = new ArrayList<>();

	private List<PublicHoliday> publicHolidays = new ArrayList<>();

	public CompanyLeaveSettings() {
		// Default leave types with days
		availableLeaveTypes.add(new LeaveTypeConfig("Annual Leave", 14));
		availableLeaveTypes.add(new LeaveTypeConfig("Sick Leave", 14));
		availableLeaveTypes.add(new LeaveTypeConfig("Emergency Leave", 5));
		availableLeaveTypes.add(new LeaveTypeConfig("Unpaid Leave", -1)); // -1 means unlimited
		availableLeaveTypes.add(new LeaveTypeConfig("Maternity Leave", 60));
		availableLeaveTypes.add(new LeaveTypeConfig("Paternity Leave", 7));
		availableLeaveTypes.add(new LeaveTypeConfig("Other", -1));
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<LeaveTypeConfig> getAvailableLeaveTypes() {
		return availableLeaveTypes;
	}

	public void setAvailableLeaveTypes(List<LeaveTypeConfig> availableLeaveTypes) {
		this.availableLeaveTypes = availableLeaveTypes;
	}

	public List<PublicHoliday> getPublicHolidays() {
		return publicHolidays;
	}

	public void setPublicHolidays(List<PublicHoliday> publicHolidays) {
		this.publicHolidays = publicHolidays;
	}

	public static class LeaveTypeConfig {
		private String name;
		private int daysAllowed; // -1 means unlimited

		public LeaveTypeConfig() {
		}

		public LeaveTypeConfig(String name, int daysAllowed) {
			this.name = name;
			this.daysAllowed = daysAllowed;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int getDaysAllowed() {
			return daysAllowed;
		}

		public void setDaysAllowed(int daysAllowed) {
			this.daysAllowed = daysAllowed;
		}
	}

	public static class PublicHoliday {
		private LocalDate date;
		private String name;

		public PublicHoliday() {
		}

		public PublicHoliday(LocalDate date, String name) {
			this.date = date;
			this.name = name;
		}

		public LocalDate getDate() {
			return date;
		}

		public void setDate(LocalDate date) {
			this.date = date;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}

