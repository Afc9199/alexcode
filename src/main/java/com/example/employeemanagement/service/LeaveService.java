package com.example.employeemanagement.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.example.employeemanagement.dto.CreateLeaveRequest;
import com.example.employeemanagement.dto.LeaveDecisionRequest;
import com.example.employeemanagement.dto.LeaveResponse;
import com.example.employeemanagement.model.LeaveRequest;
import com.example.employeemanagement.model.LeaveStatus;
import com.example.employeemanagement.model.UserAccount;
import com.example.employeemanagement.repository.LeaveRequestRepository;
import com.example.employeemanagement.repository.UserAccountRepository;

@Service
public class LeaveService {

	private final LeaveRequestRepository leaveRequestRepository;
	private final UserAccountRepository userAccountRepository;
	private final LeaveBalanceService leaveBalanceService;

	public LeaveService(LeaveRequestRepository leaveRequestRepository, UserAccountRepository userAccountRepository,
			LeaveBalanceService leaveBalanceService) {
		this.leaveRequestRepository = leaveRequestRepository;
		this.userAccountRepository = userAccountRepository;
		this.leaveBalanceService = leaveBalanceService;
	}

	private String generateNextLeaveId() {
		return leaveRequestRepository.findTopByOrderByLeaveIdDesc()
				.map(leave -> {
					String lastId = leave.getLeaveId();
					if (lastId != null && lastId.startsWith("L")) {
						int num = Integer.parseInt(lastId.substring(1));
						return String.format("L%03d", num + 1);
					}
					return "L001";
				})
				.orElse("L001");
	}

	public String getNextLeaveId() {
		return generateNextLeaveId();
	}

	public LeaveResponse submitLeave(CreateLeaveRequest request) {
		String userId = requireId(request.userId());
		UserAccount user = userAccountRepository.findById(userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
		if (request.endDate().isBefore(request.startDate())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date must be after start date");
		}
		
		// Validate gender-based leave types
		String userGender = user.getGender() != null ? user.getGender().trim() : "";
		String leaveTypeTrimmed = request.leaveType() != null ? request.leaveType().trim() : "";
		
		if ("Maternity Leave".equalsIgnoreCase(leaveTypeTrimmed)) {
			if (!"Female".equalsIgnoreCase(userGender)) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maternity Leave only for female employee");
			}
		} else if ("Paternity Leave".equalsIgnoreCase(leaveTypeTrimmed)) {
			if (!"Male".equalsIgnoreCase(userGender)) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Paternity Leave only for male employee");
			}
		}
		
		LeaveRequest leaveRequest = new LeaveRequest();
		leaveRequest.setLeaveId(generateNextLeaveId());
		leaveRequest.setUserId(userId);
		leaveRequest.setEmployeeId(user.getEmployeeId());
		leaveRequest.setLeaveType(request.leaveType());
		leaveRequest.setStartDate(request.startDate());
		leaveRequest.setEndDate(request.endDate());
		leaveRequest.setReason(request.reason());
		return toResponse(leaveRequestRepository.save(leaveRequest));
	}

	public LeaveResponse submitLeave(String userId, String leaveType, String startDateStr, String endDateStr,
			String reason, MultipartFile supportingDocument) {
		String safeUserId = requireId(userId);
		UserAccount user = userAccountRepository.findById(safeUserId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

		if (!StringUtils.hasText(leaveType)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Leave type is required");
		}
		if (!StringUtils.hasText(startDateStr) || !StringUtils.hasText(endDateStr)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date and end date are required");
		}
		if (!StringUtils.hasText(reason)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reason is required");
		}

		// Validate gender-based leave types
		String userGender = user.getGender() != null ? user.getGender().trim() : "";
		String leaveTypeTrimmed = leaveType.trim();
		
		if ("Maternity Leave".equalsIgnoreCase(leaveTypeTrimmed)) {
			if (!"Female".equalsIgnoreCase(userGender)) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maternity Leave only for female employee");
			}
		} else if ("Paternity Leave".equalsIgnoreCase(leaveTypeTrimmed)) {
			if (!"Male".equalsIgnoreCase(userGender)) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Paternity Leave only for male employee");
			}
		}

		LocalDate startDate = LocalDate.parse(startDateStr);
		LocalDate endDate = LocalDate.parse(endDateStr);

		if (endDate.isBefore(startDate)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date must be after start date");
		}

		// Handle file upload
		String supportingDocumentFilename = null;
		if (supportingDocument != null && !supportingDocument.isEmpty()) {
			// Validate file size (max 10MB)
			if (supportingDocument.getSize() > 10 * 1024 * 1024) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File size must not exceed 10MB");
			}

			// Validate file type
			String originalFilename = supportingDocument.getOriginalFilename();
			if (originalFilename != null) {
				String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
				if (!isValidFileType(extension)) {
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
							"Invalid file type. Only images, PDF, DOC, and DOCX files are allowed");
				}

				try {
					// Create upload directory if it doesn't exist (use absolute path)
					Path uploadDir = Paths.get(System.getProperty("user.dir"), "uploads", "leave-documents");
					if (!Files.exists(uploadDir)) {
						Files.createDirectories(uploadDir);
					}

					// Generate unique filename
					String uniqueFilename = UUID.randomUUID().toString() + "_" + originalFilename;
					Path filePath = uploadDir.resolve(uniqueFilename);

					// Save file
					Files.copy(supportingDocument.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
					supportingDocumentFilename = uniqueFilename;
				} catch (IOException e) {
					throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save file: " + e.getMessage());
				}
			}
		}

		LeaveRequest leaveRequest = new LeaveRequest();
		leaveRequest.setLeaveId(generateNextLeaveId());
		leaveRequest.setUserId(safeUserId);
		leaveRequest.setEmployeeId(user.getEmployeeId());
		leaveRequest.setLeaveType(leaveType);
		leaveRequest.setStartDate(startDate);
		leaveRequest.setEndDate(endDate);
		leaveRequest.setReason(reason);
		leaveRequest.setSupportingDocumentFilename(supportingDocumentFilename);

		return toResponse(leaveRequestRepository.save(leaveRequest));
	}

	private boolean isValidFileType(String extension) {
		return extension.equals("jpg") || extension.equals("jpeg") || extension.equals("png") || extension.equals("gif")
				|| extension.equals("pdf") || extension.equals("doc") || extension.equals("docx");
	}

	public List<LeaveResponse> findForUser(String userId) {
		String safeUserId = requireId(userId);
		if (!userAccountRepository.existsById(safeUserId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
		}
		return leaveRequestRepository.findByUserIdOrderByCreatedAtDesc(safeUserId).stream().map(this::toResponse).toList();
	}

	public List<LeaveResponse> findAll() {
		return leaveRequestRepository.findAll().stream().map(this::toResponse).toList();
	}

	public LeaveResponse decideLeave(String leaveId, LeaveDecisionRequest request) {
		LeaveRequest leaveRequest = leaveRequestRepository.findByLeaveId(requireId(leaveId))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request not found"));
		
		LeaveStatus oldStatus = leaveRequest.getStatus();
		LeaveStatus newStatus = request.status();
		
		// If approving, check leave balance first
		if (newStatus == LeaveStatus.APPROVED) {
			leaveBalanceService.validateLeaveBalance(leaveRequest.getUserId(), leaveRequest.getLeaveType(),
					leaveRequest.getStartDate(), leaveRequest.getEndDate());
		}
		
		// Update status
		leaveRequest.setStatus(newStatus);
		leaveRequest.setManagerComment(request.managerComment());
		leaveRequest.setDecidedBy(request.decidedBy());
		leaveRequest.setDecidedAt(Instant.now());
		
		LeaveRequest savedRequest = leaveRequestRepository.save(leaveRequest);
		
		// Update leave balance based on status change
		if (newStatus == LeaveStatus.APPROVED) {
			// Deduct from balance when approved
			leaveBalanceService.updateBalanceOnApproval(leaveRequest.getUserId(), leaveRequest.getLeaveType(),
					leaveRequest.getStartDate(), leaveRequest.getEndDate());
		} else if (oldStatus == LeaveStatus.APPROVED
				&& (newStatus == LeaveStatus.REJECTED || newStatus == LeaveStatus.CANCELLED)) {
			// Restore balance if previously approved leave is now rejected or cancelled
			leaveBalanceService.updateBalanceOnRejectionOrCancellation(leaveRequest.getUserId(),
					leaveRequest.getLeaveType(), leaveRequest.getStartDate(), leaveRequest.getEndDate());
		}
		
		return toResponse(savedRequest);
	}

	public void deleteLeaveRequest(String leaveId) {
		LeaveRequest leaveRequest = leaveRequestRepository.findByLeaveId(requireId(leaveId))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request not found"));
		
		// If the leave request was approved, restore the leave balance before deleting
		if (leaveRequest.getStatus() == LeaveStatus.APPROVED) {
			leaveBalanceService.updateBalanceOnRejectionOrCancellation(
					leaveRequest.getUserId(),
					leaveRequest.getLeaveType(),
					leaveRequest.getStartDate(),
					leaveRequest.getEndDate());
		}
		
		// Delete the leave request
		leaveRequestRepository.delete(leaveRequest);
	}

	private LeaveResponse toResponse(LeaveRequest leaveRequest) {
		String employeeName = userAccountRepository.findById(requireId(leaveRequest.getUserId()))
				.map(UserAccount::getFullName)
				.orElse("Unknown");
		return new LeaveResponse(
				leaveRequest.getId(),
				leaveRequest.getLeaveId(),
				leaveRequest.getUserId(),
				leaveRequest.getEmployeeId(),
				employeeName,
				leaveRequest.getLeaveType(),
				leaveRequest.getStartDate(),
				leaveRequest.getEndDate(),
				leaveRequest.getReason(),
				leaveRequest.getSupportingDocumentFilename(),
				leaveRequest.getStatus(),
				leaveRequest.getManagerComment(),
				leaveRequest.getCreatedAt(),
				leaveRequest.getDecidedAt());
	}

	private @NonNull String requireId(String userId) {
		return Objects.requireNonNull(userId, "Identifier is required");
	}
}

