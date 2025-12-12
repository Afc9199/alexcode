package com.example.employeemanagement.service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.employeemanagement.dto.CompanyLeaveSettingsResponse;
import com.example.employeemanagement.model.AttendanceRecord;
import com.example.employeemanagement.model.AttendanceStatus;
import com.example.employeemanagement.model.LeaveRequest;
import com.example.employeemanagement.model.LeaveStatus;
import com.example.employeemanagement.model.CompanySettings;
import com.example.employeemanagement.model.UserAccount;
import com.example.employeemanagement.repository.AttendanceRepository;
import com.example.employeemanagement.repository.LeaveRequestRepository;
import com.example.employeemanagement.repository.UserAccountRepository;

@Service
public class AttendanceSchedulerService {

	private final AttendanceRepository attendanceRepository;
	private final UserAccountRepository userAccountRepository;
	private final AttendanceService attendanceService;
	private final CompanyLeaveSettingsService companyLeaveSettingsService;
	private final LeaveRequestRepository leaveRequestRepository;
	private final CompanySettingsService companySettingsService;

	public AttendanceSchedulerService(AttendanceRepository attendanceRepository,
			UserAccountRepository userAccountRepository, AttendanceService attendanceService,
			CompanyLeaveSettingsService companyLeaveSettingsService,
			LeaveRequestRepository leaveRequestRepository,
			CompanySettingsService companySettingsService) {
		this.attendanceRepository = attendanceRepository;
		this.userAccountRepository = userAccountRepository;
		this.attendanceService = attendanceService;
		this.companyLeaveSettingsService = companyLeaveSettingsService;
		this.leaveRequestRepository = leaveRequestRepository;
		this.companySettingsService = companySettingsService;
	}

	/**
	 * Runs daily at 11:59 PM to mark employees who didn't submit attendance as
	 * ABSENT
	 */
	@Scheduled(cron = "0 59 23 * * ?") // Every day at 11:59 PM
	public void markAbsentEmployees() {
		LocalDate today = LocalDate.now();
		markAbsentForDate(today);
	}

	/**
	 * Marks all employees without attendance records as ABSENT for a specific date
	 * If the date is a public holiday, marks as PUBLIC_HOLIDAY instead
	 * If the employee has approved leave, marks as ON_LEAVE instead
	 */
	public void markAbsentForDate(LocalDate date) {
		if (!isWorkingDay(date)) {
			markOffdayForDate(date);
			return;
		}

		// Check if the date is a public holiday
		boolean isPublicHoliday = isDatePublicHoliday(date);
		
		// Get all active employees (non-admin users)
		List<UserAccount> allEmployees = userAccountRepository.findAll().stream()
				.filter(user -> user.isActive() && "EMPLOYEE".equals(user.getRole().name()))
				.toList();

		for (UserAccount employee : allEmployees) {
			// Check if employee has attendance record for this date
			boolean hasAttendance = attendanceRepository.existsByUserIdAndWorkDate(employee.getId(), date);

			if (!hasAttendance) {
				// Create attendance record
				AttendanceRecord record = new AttendanceRecord();
				record.setAttendanceId(attendanceService.getNextAttendanceId());
				record.setUserId(employee.getId());
				record.setEmployeeId(employee.getEmployeeId());
				record.setWorkDate(date);
				record.setCheckIn(null);
				record.setCheckOut(null);
				
				// Check priority: Public Holiday > Approved Leave > Absent
				if (isPublicHoliday) {
					record.setStatus(AttendanceStatus.PUBLIC_HOLIDAY);
					record.setNotes("Public Holiday - " + getPublicHolidayName(date));
				} else if (hasApprovedLeave(employee.getId(), date)) {
					record.setStatus(AttendanceStatus.ON_LEAVE);
					record.setNotes("On Leave - " + getLeaveType(employee.getId(), date));
				} else {
					record.setStatus(AttendanceStatus.ABSENT);
					record.setNotes("Auto-marked as absent - no attendance submitted");
				}
				
				record.setCreatedAt(Instant.now());
				record.setUpdatedAt(Instant.now());

				attendanceRepository.save(record);
			}
		}
	}

	private void markOffdayForDate(LocalDate date) {
		List<UserAccount> allEmployees = userAccountRepository.findAll().stream()
				.filter(user -> user.isActive() && "EMPLOYEE".equals(user.getRole().name()))
				.toList();

		for (UserAccount employee : allEmployees) {
			boolean hasAttendance = attendanceRepository.existsByUserIdAndWorkDate(employee.getId(), date);
			if (!hasAttendance) {
				AttendanceRecord record = new AttendanceRecord();
				record.setAttendanceId(attendanceService.getNextAttendanceId());
				record.setUserId(employee.getId());
				record.setEmployeeId(employee.getEmployeeId());
				record.setWorkDate(date);
				record.setCheckIn(null);
				record.setCheckOut(null);
				record.setStatus(AttendanceStatus.OFFDAY);
				record.setNotes("Company off day");
				record.setCreatedAt(Instant.now());
				record.setUpdatedAt(Instant.now());
				attendanceRepository.save(record);
			}
		}
	}

	private boolean isWorkingDay(LocalDate date) {
		try {
			CompanySettings settings = companySettingsService.getOrCreateSettings();
			List<String> workingDays = settings.getWorkingDays();
			if (workingDays == null || workingDays.isEmpty()) {
				return true;
			}
			DayOfWeek dayOfWeek = date.getDayOfWeek();
			return workingDays.contains(dayOfWeek.name());
		} catch (Exception e) {
			return true;
		}
	}
	
	/**
	 * Check if a specific date is a public holiday
	 */
	private boolean isDatePublicHoliday(LocalDate date) {
		try {
			CompanyLeaveSettingsResponse settings = companyLeaveSettingsService.getSettings();
			return settings.publicHolidays().stream()
					.anyMatch(holiday -> holiday.date().equals(date));
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * Get the name of the public holiday for a specific date
	 */
	private String getPublicHolidayName(LocalDate date) {
		try {
			CompanyLeaveSettingsResponse settings = companyLeaveSettingsService.getSettings();
			return settings.publicHolidays().stream()
					.filter(holiday -> holiday.date().equals(date))
					.map(CompanyLeaveSettingsResponse.PublicHolidayDto::name)
					.findFirst()
					.orElse("Public Holiday");
		} catch (Exception e) {
			return "Public Holiday";
		}
	}
	
	/**
	 * Check if employee has approved leave for a specific date
	 */
	private boolean hasApprovedLeave(String userId, LocalDate date) {
		try {
			List<LeaveRequest> approvedLeaves = leaveRequestRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
					.filter(leave -> leave.getStatus() == LeaveStatus.APPROVED)
					.filter(leave -> !date.isBefore(leave.getStartDate()) && !date.isAfter(leave.getEndDate()))
					.toList();
			return !approvedLeaves.isEmpty();
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * Get the leave type for an employee on a specific date
	 */
	private String getLeaveType(String userId, LocalDate date) {
		try {
			return leaveRequestRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
					.filter(leave -> leave.getStatus() == LeaveStatus.APPROVED)
					.filter(leave -> !date.isBefore(leave.getStartDate()) && !date.isAfter(leave.getEndDate()))
					.findFirst()
					.map(LeaveRequest::getLeaveType)
					.orElse("Leave");
		} catch (Exception e) {
			return "Leave";
		}
	}
}

