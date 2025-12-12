package com.example.employeemanagement.service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import java.net.InetAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.example.employeemanagement.dto.AdminAttendanceRequest;
import com.example.employeemanagement.dto.AttendanceResponse;
import com.example.employeemanagement.dto.CreateAttendanceRequest;
import com.example.employeemanagement.dto.UpdateAttendanceStatusRequest;
import com.example.employeemanagement.config.AttendanceSecurityProperties;
import com.example.employeemanagement.model.AttendanceRecord;
import com.example.employeemanagement.model.AttendanceStatus;
import com.example.employeemanagement.model.CompanySettings;
import com.example.employeemanagement.model.UserAccount;
import com.example.employeemanagement.repository.AttendanceRepository;
import com.example.employeemanagement.repository.UserAccountRepository;

@Service
public class AttendanceService {

	private static final double EARTH_RADIUS_METERS = 6_371_000;
	private static final Logger logger = LoggerFactory.getLogger(AttendanceService.class);

	private final AttendanceRepository attendanceRepository;
	private final UserAccountRepository userAccountRepository;
	private final AttendanceSecurityProperties securityProperties;
	private final CompanySettingsService companySettingsService;

	public AttendanceService(AttendanceRepository attendanceRepository, UserAccountRepository userAccountRepository,
			AttendanceSecurityProperties securityProperties, CompanySettingsService companySettingsService) {
		this.attendanceRepository = attendanceRepository;
		this.userAccountRepository = userAccountRepository;
		this.securityProperties = securityProperties;
		this.companySettingsService = companySettingsService;
	}

	public AttendanceResponse recordAttendance(CreateAttendanceRequest request, String clientIpAddress) {
		String userId = requireId(request.userId());
		UserAccount user = userAccountRepository.findById(userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

		// Always enforce location & network for both clock in and clock out
		enforceLocation(request);
		enforceNetwork(clientIpAddress);

		// If a record already exists for this user & date, treat this as a Clock Out
		// operation (update existing record) instead of creating a duplicate.
		var existingOpt = attendanceRepository.findByUserIdAndWorkDate(user.getId(), request.workDate());
		if (existingOpt.isPresent()) {
			AttendanceRecord existing = existingOpt.get();
			
			// If check-out already recorded, prevent duplicate submissions
			if (existing.getCheckOut() != null) {
				throw new ResponseStatusException(HttpStatus.CONFLICT, "Attendance already recorded for this date");
			}

			// Update existing record with clock-out information
			// If check-in was not set previously (edge case), use the incoming value.
			if (existing.getCheckIn() == null && request.checkIn() != null) {
				existing.setCheckIn(request.checkIn());
			}
			existing.setCheckOut(request.checkOut());
			existing.setNotes(request.notes());
			existing.setLatitude(request.latitude());
			existing.setLongitude(request.longitude());
			existing.setAccuracyMeters(request.accuracyMeters());
			existing.setSourceNetwork(clientIpAddress);
			existing.setUpdatedAt(Instant.now());
			
			AttendanceRecord updated = attendanceRepository.save(existing);
			return toResponse(updated);
		}

		// No existing record: treat as first Clock In for the day
		AttendanceRecord record = new AttendanceRecord();
		record.setAttendanceId(generateNextAttendanceId());
		record.setUserId(user.getId());
		record.setEmployeeId(user.getEmployeeId());
		record.setWorkDate(request.workDate());
		record.setCheckIn(request.checkIn());
		record.setCheckOut(request.checkOut());
		
		// Auto-detect late status based on company settings
		AttendanceStatus autoStatus = determineAttendanceStatus(request.checkIn());
		record.setStatus(autoStatus);
		
		record.setNotes(request.notes());
		record.setLatitude(request.latitude());
		record.setLongitude(request.longitude());
		record.setAccuracyMeters(request.accuracyMeters());
		record.setSourceNetwork(clientIpAddress);
		record.setCreatedAt(Instant.now());
		record.setUpdatedAt(Instant.now());
		AttendanceRecord saved = attendanceRepository.save(record);
		return toResponse(saved);
	}

	public List<AttendanceResponse> findForUser(String userId) {
		String safeUserId = requireId(userId);
		if (!userAccountRepository.existsById(safeUserId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
		}
		return attendanceRepository.findByUserIdOrderByWorkDateDesc(safeUserId).stream().map(this::toResponse).toList();
	}

	public List<AttendanceResponse> findAll() {
		return attendanceRepository.findAll().stream().map(this::toResponse).toList();
	}

	public AttendanceResponse updateStatus(String attendanceId, UpdateAttendanceStatusRequest request) {
		AttendanceRecord record = attendanceRepository.findById(requireId(attendanceId))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attendance record not found"));
		record.setStatus(request.status());
		record.setNotes(request.notes());
		record.setUpdatedAt(Instant.now());
		return toResponse(attendanceRepository.save(record));
	}

	// Admin CRUD Methods
	
	public AttendanceResponse adminCreateAttendance(AdminAttendanceRequest request) {
		UserAccount user = userAccountRepository.findByEmployeeId(request.employeeId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found with ID: " + request.employeeId()));

		// Check if attendance already exists for this employee on this date
		// If exists, find all duplicates and delete them, then create a new one
		Optional<AttendanceRecord> existingOpt = attendanceRepository.findByUserIdAndWorkDate(user.getId(), request.workDate());
		
		AttendanceRecord record;
		if (existingOpt.isPresent()) {
			// Delete all existing records for this user and date to prevent duplicates
			// First, find all records (there might be multiple duplicates)
			List<AttendanceRecord> allRecords = attendanceRepository.findAll().stream()
					.filter(r -> r.getUserId().equals(user.getId()) && r.getWorkDate().equals(request.workDate()))
					.toList();
			
			if (!allRecords.isEmpty()) {
				logger.warn("Found {} duplicate attendance record(s) for employee {} on {}. Deleting duplicates and creating new record.", 
						allRecords.size(), request.employeeId(), request.workDate());
				// Delete all duplicates
				attendanceRepository.deleteAll(allRecords);
			}
			
			// Create new record
			record = new AttendanceRecord();
			record.setAttendanceId(generateNextAttendanceId());
			record.setUserId(user.getId());
			record.setEmployeeId(user.getEmployeeId());
			record.setWorkDate(request.workDate());
			record.setCreatedAt(Instant.now());
		} else {
			// Create new record
			record = new AttendanceRecord();
			record.setAttendanceId(generateNextAttendanceId());
			record.setUserId(user.getId());
			record.setEmployeeId(user.getEmployeeId());
			record.setWorkDate(request.workDate());
			record.setCreatedAt(Instant.now());
		}
		
		// Set all fields
		record.setCheckIn(request.checkIn());
		record.setCheckOut(request.checkOut());
		
		// Auto-calculate status if check-in time is provided
		// Otherwise, use the status from the request (for ABSENT, REMOTE, PUBLIC_HOLIDAY, ON_LEAVE)
		AttendanceStatus finalStatus;
		if (request.checkIn() != null) {
			// Auto-calculate PRESENT or LATE based on check-in time and company settings
			finalStatus = determineAttendanceStatus(request.checkIn());
		} else {
			// No check-in time provided, use the status from request
			finalStatus = request.status();
		}
		record.setStatus(finalStatus);
		
		record.setNotes(request.notes());
		record.setUpdatedAt(Instant.now());
		AttendanceRecord saved = attendanceRepository.save(record);
		return toResponse(saved);
	}

	public AttendanceResponse getAttendanceById(String attendanceId) {
		if (attendanceId == null || attendanceId.trim().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attendance ID is required");
		}
		
		String trimmedId = attendanceId.trim();
		
		// Try to find by attendanceId first
		Optional<AttendanceRecord> recordOpt = attendanceRepository.findByAttendanceId(trimmedId);
		
		if (recordOpt.isPresent()) {
			return toResponse(recordOpt.get());
		}
		
		// If not found by attendanceId, try to find by MongoDB _id (in case attendanceId is actually the _id)
		try {
			String id = Objects.requireNonNull(trimmedId);
			Optional<AttendanceRecord> byIdOpt = attendanceRepository.findById(id);
			if (byIdOpt.isPresent()) {
				return toResponse(byIdOpt.get());
			}
		} catch (Exception e) {
			// Ignore, continue to throw not found
			logger.debug("Error finding attendance by MongoDB _id: {}", e.getMessage());
		}
		
		// If still not found, log for debugging
		logger.warn("Attendance record not found with ID: {}", trimmedId);
		
		throw new ResponseStatusException(HttpStatus.NOT_FOUND, 
				"Attendance record not found with ID: " + trimmedId);
	}

	public AttendanceResponse adminUpdateAttendance(String attendanceId, AdminAttendanceRequest request) {
		// Try to find record by attendanceId
		Optional<AttendanceRecord> recordOpt = attendanceRepository.findByAttendanceId(attendanceId);
		
		// If not found by attendanceId, try to find by any matching criteria
		AttendanceRecord record;
		if (recordOpt.isPresent()) {
			record = recordOpt.get();
		} else {
			// Record not found by attendanceId, throw error
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, 
					"Attendance record not found with ID: " + attendanceId);
		}

		// Verify the employee exists
		UserAccount user = userAccountRepository.findByEmployeeId(request.employeeId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found with ID: " + request.employeeId()));

		// Check if updating workDate or employeeId would create a duplicate
		// Only check if the combination has changed
		boolean dateOrEmployeeChanged = !record.getWorkDate().equals(request.workDate()) 
				|| !record.getUserId().equals(user.getId());
		
		if (dateOrEmployeeChanged) {
			// Find all records with the new combination (excluding current record)
			List<AttendanceRecord> duplicates = attendanceRepository.findAll().stream()
					.filter(r -> r.getUserId().equals(user.getId()) 
							&& r.getWorkDate().equals(request.workDate())
							&& !r.getId().equals(record.getId()))
					.toList();
			
			if (!duplicates.isEmpty()) {
				// Delete all duplicates
				logger.warn("Found {} duplicate attendance record(s) when updating. Deleting duplicates.", duplicates.size());
				attendanceRepository.deleteAll(duplicates);
			}
		}

		// Update all fields - admin has full manual control, no auto-calculation
		record.setUserId(user.getId());
		record.setEmployeeId(user.getEmployeeId());
		record.setWorkDate(request.workDate());
		record.setCheckIn(request.checkIn());
		record.setCheckOut(request.checkOut());
		record.setStatus(request.status());
		record.setNotes(request.notes());
		record.setUpdatedAt(Instant.now());
		
		return toResponse(attendanceRepository.save(record));
	}

	public void deleteAttendance(String attendanceId) {
		AttendanceRecord record = attendanceRepository.findByAttendanceId(attendanceId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attendance record not found"));
		
		// Delete by ID to ensure we delete the correct record even if there are duplicates
		String recordId = Objects.requireNonNull(record.getId());
		attendanceRepository.deleteById(recordId);
	}

	public String getNextAttendanceId() {
		return generateNextAttendanceId();
	}

	private AttendanceStatus determineAttendanceStatus(java.time.LocalTime checkInTime) {
		if (checkInTime == null) {
			return AttendanceStatus.ABSENT;
		}

		CompanySettings settings = companySettingsService.getOrCreateSettings();
		java.time.LocalTime workStartTime = settings.getWorkStartTime();
		Integer lateThresholdMinutes = settings.getLateThresholdMinutes();

		// Add threshold to work start time
		java.time.LocalTime lateTime = workStartTime.plusMinutes(lateThresholdMinutes);

		// If check-in is after the late time, mark as LATE
		if (checkInTime.isAfter(lateTime)) {
			return AttendanceStatus.LATE;
		}

		// Otherwise, mark as PRESENT
		return AttendanceStatus.PRESENT;
	}

	private String generateNextAttendanceId() {
		return attendanceRepository.findTopByOrderByAttendanceIdDesc()
				.map(lastRecord -> {
					String lastId = lastRecord.getAttendanceId();
					if (lastId != null && lastId.startsWith("A")) {
						try {
							int number = Integer.parseInt(lastId.substring(1));
							return String.format("A%03d", number + 1);
						} catch (NumberFormatException e) {
							return "A001";
						}
					}
					return "A001";
				})
				.orElse("A001");
	}

	private AttendanceResponse toResponse(AttendanceRecord record) {
		// Fetch employee name from userId
		String employeeName = null;
		if (record.getUserId() != null) {
			try {
				UserAccount user = userAccountRepository.findById(Objects.requireNonNull(record.getUserId())).orElse(null);
				if (user != null) {
					employeeName = user.getFullName();
				}
			} catch (Exception ignored) {
				// If user not found, leave employeeName as null
			}
		}
		
		return new AttendanceResponse(record.getId(), record.getAttendanceId(), record.getUserId(), record.getEmployeeId(), 
				employeeName, record.getWorkDate(), record.getCheckIn(), record.getCheckOut(), record.getStatus(), 
				record.getNotes(), record.getLatitude(), record.getLongitude(), record.getAccuracyMeters(), 
				record.getSourceNetwork());
	}

	private @NonNull String requireId(String userId) {
		return Objects.requireNonNull(userId, "Identifier is required");
	}

	private void enforceLocation(CreateAttendanceRequest request) {
		double requiredLat = securityProperties.getLocation().getLatitude();
		double requiredLon = securityProperties.getLocation().getLongitude();
		double radius = securityProperties.getLocation().getRadiusMeters();
		if (radius <= 0) {
			return;
		}
		if (request.latitude() == null || request.longitude() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Location is required for attendance");
		}
		double distance = haversineDistanceMeters(request.latitude(), request.longitude(), requiredLat, requiredLon);
		if (distance > radius) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Attendance is allowed only on-site");
		}
		if (request.accuracyMeters() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Location accuracy is required");
		}
		if (request.accuracyMeters() > securityProperties.getLocation().getMaxAccuracyMeters()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Location accuracy is too low to validate attendance");
		}
	}

	private void enforceNetwork(String clientIpAddress) {
		if (securityProperties.getAllowedNetworks().isEmpty()) {
			logger.warn("No allowed networks configured, skipping network validation");
			return;
		}
		
		if (!StringUtils.hasText(clientIpAddress)) {
			logger.warn("Client IP address is empty, rejecting attendance");
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wi-Fi verification failed: Unable to determine client IP address");
		}

		logger.info("Validating client IP address: {} against allowed networks: {}", 
			clientIpAddress, securityProperties.getAllowedNetworks());

		boolean allowed = securityProperties.getAllowedNetworks().stream()
				.anyMatch(cidr -> {
					boolean matches = ipWithinCidr(clientIpAddress, cidr);
					if (matches) {
						logger.info("Client IP {} matches allowed network {}", clientIpAddress, cidr);
					}
					return matches;
				});

		// Only allow IPv6 local addresses in development (localhost scenarios)
		// In production, this should be removed or restricted
		if (!allowed && clientIpAddress.contains(":")) {
			try {
				InetAddress addr = InetAddress.getByName(clientIpAddress);
				// Only allow link-local (fe80::) for development, not site-local
				if (addr.isLinkLocalAddress()) {
					logger.warn("Allowing IPv6 link-local address {} for development (should be restricted in production)", clientIpAddress);
					allowed = true;
				}
			} catch (Exception e) {
				logger.error("Error parsing IPv6 address: {}", clientIpAddress, e);
			}
		}

		if (!allowed) {
			logger.warn("Client IP {} is not in allowed networks. Rejecting attendance.", clientIpAddress);
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
				"Attendance requires company Wi-Fi network. Your current IP address (" + clientIpAddress + ") is not authorized.");
		}
		
		logger.info("Network validation passed for IP: {}", clientIpAddress);
	}

	private double haversineDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
		double latDistance = Math.toRadians(lat2 - lat1);
		double lonDistance = Math.toRadians(lon2 - lon1);
		double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
				+ Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(lonDistance / 2)
						* Math.sin(lonDistance / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		return EARTH_RADIUS_METERS * c;
	}

	private boolean ipWithinCidr(String ip, String cidr) {
		try {
			String[] parts = cidr.split("/");
			if (parts.length != 2) {
				return false;
			}
			int prefixLength = Integer.parseInt(parts[1]);
			InetAddress networkAddress = InetAddress.getByName(parts[0]);
			InetAddress targetAddress = InetAddress.getByName(ip);
			byte[] networkBytes = networkAddress.getAddress();
			byte[] targetBytes = targetAddress.getAddress();
			if (networkBytes.length != targetBytes.length) {
				return false;
			}
			int fullBytes = prefixLength / 8;
			int remainingBits = prefixLength % 8;
			for (int i = 0; i < fullBytes; i++) {
				if (networkBytes[i] != targetBytes[i]) {
					return false;
				}
			}
			if (remainingBits > 0) {
				int mask = 0xFF << (8 - remainingBits);
				if ((networkBytes[fullBytes] & mask) != (targetBytes[fullBytes] & mask)) {
					return false;
				}
			}
			return true;
		} catch (Exception ex) {
			return false;
		}
	}
}

