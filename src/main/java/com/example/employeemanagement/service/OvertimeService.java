package com.example.employeemanagement.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.employeemanagement.dto.CreateOvertimeRequest;
import com.example.employeemanagement.dto.OvertimeDecisionRequest;
import com.example.employeemanagement.dto.OvertimeResponse;
import com.example.employeemanagement.model.OvertimeRequest;
import com.example.employeemanagement.model.OvertimeStatus;
import com.example.employeemanagement.model.UserAccount;
import com.example.employeemanagement.repository.OvertimeRequestRepository;
import com.example.employeemanagement.repository.UserAccountRepository;

@Service
public class OvertimeService {

	private final OvertimeRequestRepository overtimeRequestRepository;
	private final UserAccountRepository userAccountRepository;

	public OvertimeService(OvertimeRequestRepository overtimeRequestRepository,
			UserAccountRepository userAccountRepository) {
		this.overtimeRequestRepository = overtimeRequestRepository;
		this.userAccountRepository = userAccountRepository;
	}

	private String generateNextOvertimeId() {
		return overtimeRequestRepository.findTopByOrderByOvertimeIdDesc()
				.map(overtime -> {
					String lastId = overtime.getOvertimeId();
					if (lastId != null && lastId.startsWith("OT")) {
						int num = Integer.parseInt(lastId.substring(2));
						return String.format("OT%03d", num + 1);
					}
					return "OT001";
				})
				.orElse("OT001");
	}

	public String getNextOvertimeId() {
		return generateNextOvertimeId();
	}

	public OvertimeResponse submitOvertime(CreateOvertimeRequest request) {
		String userId = requireId(request.userId());
		UserAccount user = userAccountRepository.findById(userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

		// Validate time range
		if (request.endTime().isBefore(request.startTime()) || request.endTime().equals(request.startTime())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"End time must be after start time");
		}

		// Calculate hours
		Duration duration = Duration.between(request.startTime(), request.endTime());
		double hours = duration.toMinutes() / 60.0;

		OvertimeRequest overtimeRequest = new OvertimeRequest();
		overtimeRequest.setOvertimeId(generateNextOvertimeId());
		overtimeRequest.setUserId(userId);
		overtimeRequest.setEmployeeId(user.getEmployeeId());
		overtimeRequest.setWorkDate(request.workDate());
		overtimeRequest.setStartTime(request.startTime());
		overtimeRequest.setEndTime(request.endTime());
		overtimeRequest.setHours(hours);
		overtimeRequest.setReason(request.reason());
		overtimeRequest.setStatus(OvertimeStatus.PENDING);

		return toResponse(overtimeRequestRepository.save(overtimeRequest));
	}

	public List<OvertimeResponse> findForUser(String userId) {
		String safeUserId = requireId(userId);
		if (!userAccountRepository.existsById(safeUserId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
		}
		return overtimeRequestRepository.findByUserIdOrderByCreatedAtDesc(safeUserId).stream()
				.map(this::toResponse).toList();
	}

	public List<OvertimeResponse> findAll() {
		return overtimeRequestRepository.findAll().stream().map(this::toResponse).toList();
	}

	public OvertimeResponse decideOvertime(String overtimeId, OvertimeDecisionRequest request) {
		OvertimeRequest overtimeRequest = overtimeRequestRepository.findByOvertimeId(requireId(overtimeId))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Overtime request not found"));

		// Update status
		overtimeRequest.setStatus(request.status());
		overtimeRequest.setManagerComment(request.managerComment());
		overtimeRequest.setDecidedBy(request.decidedBy());
		overtimeRequest.setDecidedAt(Instant.now());

		OvertimeRequest savedRequest = overtimeRequestRepository.save(overtimeRequest);

		return toResponse(savedRequest);
	}

	public void deleteOvertimeRequest(String overtimeId) {
		OvertimeRequest overtimeRequest = overtimeRequestRepository.findByOvertimeId(requireId(overtimeId))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Overtime request not found"));

		// Delete the overtime request
		overtimeRequestRepository.delete(overtimeRequest);
	}

	public List<OvertimeResponse> findByUserIdAndStatusAndWorkDateBetween(String userId, OvertimeStatus status,
			java.time.LocalDate start, java.time.LocalDate end) {
		return overtimeRequestRepository.findByUserIdAndStatusAndWorkDateBetween(userId, status, start, end).stream()
				.map(this::toResponse).toList();
	}

	private OvertimeResponse toResponse(OvertimeRequest overtimeRequest) {
		String employeeName = userAccountRepository.findById(requireId(overtimeRequest.getUserId()))
				.map(UserAccount::getFullName).orElse("Unknown");
		return new OvertimeResponse(
				overtimeRequest.getId(),
				overtimeRequest.getOvertimeId(),
				overtimeRequest.getUserId(),
				overtimeRequest.getEmployeeId(),
				employeeName,
				overtimeRequest.getWorkDate(),
				overtimeRequest.getStartTime(),
				overtimeRequest.getEndTime(),
				overtimeRequest.getHours(),
				overtimeRequest.getReason(),
				overtimeRequest.getStatus(),
				overtimeRequest.getManagerComment(),
				overtimeRequest.getCreatedAt(),
				overtimeRequest.getDecidedAt());
	}

	private @NonNull String requireId(String userId) {
		return Objects.requireNonNull(userId, "Identifier is required");
	}
}

