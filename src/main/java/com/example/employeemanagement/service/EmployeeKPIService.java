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
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.example.employeemanagement.dto.AssignKPIRequest;
import com.example.employeemanagement.dto.EmployeeKPIResponse;
import com.example.employeemanagement.dto.UpdateEmployeeKPIStatusRequest;
import com.example.employeemanagement.model.EmployeeKPI;
import com.example.employeemanagement.model.KPI;
import com.example.employeemanagement.model.UserAccount;
import com.example.employeemanagement.repository.EmployeeKPIRepository;
import com.example.employeemanagement.repository.KPIRepository;
import com.example.employeemanagement.repository.UserAccountRepository;

@Service
public class EmployeeKPIService {

	private final EmployeeKPIRepository employeeKPIRepository;
	private final UserAccountRepository userAccountRepository;
	private final KPIRepository kpiRepository;
	private final Path evidenceDirectory;

	public EmployeeKPIService(EmployeeKPIRepository employeeKPIRepository,
			UserAccountRepository userAccountRepository, KPIRepository kpiRepository) {
		this.employeeKPIRepository = employeeKPIRepository;
		this.userAccountRepository = userAccountRepository;
		this.kpiRepository = kpiRepository;
		this.evidenceDirectory = Paths.get(System.getProperty("user.dir"), "uploads", "kpi-evidence");
		try {
			Files.createDirectories(this.evidenceDirectory);
		} catch (IOException e) {
			throw new IllegalStateException("Failed to create KPI evidence directory", e);
		}
	}

	public EmployeeKPIResponse assignKPI(AssignKPIRequest request) {
		// Validate that either employeeId or departmentName is provided
		if (!StringUtils.hasText(request.employeeId()) && !StringUtils.hasText(request.departmentName())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Either employee ID or department name must be provided");
		}

		// Find KPI category
		String kpiCategoryId = Objects.requireNonNull(request.kpiCategoryId(), "KPI category ID is required");
		KPI kpi = kpiRepository.findById(kpiCategoryId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "KPI category not found"));

		if (!kpi.getActive()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This KPI category is inactive");
		}

		// Individual assignment
		if (StringUtils.hasText(request.employeeId())) {
			return assignToEmployee(request.employeeId(), kpi);
		}

		// Bulk assignment by department
		if (StringUtils.hasText(request.departmentName())) {
			return bulkAssignToDepartment(request.departmentName(), kpi);
		}

		throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid assignment request");
	}

	private EmployeeKPIResponse assignToEmployee(String employeeId, KPI kpi) {
		// Find employee by Employee ID
		UserAccount employee = userAccountRepository.findByEmployeeId(employeeId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

		// Check if KPI is already assigned to this employee
		employeeKPIRepository.findByEmployeeIdAndKpiCategoryId(employeeId, kpi.getId())
				.ifPresent(existing -> {
					throw new ResponseStatusException(HttpStatus.CONFLICT,
							"This KPI is already assigned to this employee");
				});

		// Create new employee KPI assignment
		EmployeeKPI employeeKPI = new EmployeeKPI(
				employee.getId(),
				employee.getEmployeeId(),
				kpi.getId(),
				kpi.getKpiId(),
				kpi.getName(),
				kpi.getDescription(),
				kpi.getMeasurableValue(),
				kpi.getDueDate(),
				kpi.getBonusAmount());

		EmployeeKPI saved = employeeKPIRepository.save(employeeKPI);
		return toResponse(saved, employee.getFullName());
	}

	private EmployeeKPIResponse bulkAssignToDepartment(String departmentName, KPI kpi) {
		// Find all employees in the department
		List<UserAccount> employees = userAccountRepository.findAll().stream()
				.filter(emp -> emp.getDepartment() != null && emp.getDepartment().equals(departmentName))
				.collect(Collectors.toList());

		if (employees.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND,
					"No employees found in department: " + departmentName);
		}

		// Assign KPI to all employees in the department (skip if already assigned)
		int assignedCount = 0;
		EmployeeKPI lastAssigned = null;
		String lastEmployeeName = null;

		for (UserAccount employee : employees) {
			// Check if already assigned
			if (employeeKPIRepository.findByEmployeeIdAndKpiCategoryId(employee.getEmployeeId(), kpi.getId())
					.isPresent()) {
				continue;
			}

			// Create new employee KPI assignment
			EmployeeKPI employeeKPI = new EmployeeKPI(
					employee.getId(),
					employee.getEmployeeId(),
					kpi.getId(),
					kpi.getKpiId(),
					kpi.getName(),
					kpi.getDescription(),
					kpi.getMeasurableValue(),
					kpi.getDueDate(),
					kpi.getBonusAmount());

			lastAssigned = employeeKPIRepository.save(employeeKPI);
			lastEmployeeName = employee.getFullName();
			assignedCount++;
		}

		if (assignedCount == 0) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"All employees in department already have this KPI assigned");
		}

		// Return the last assigned KPI (for response structure)
		// Note: In a real scenario, you might want to return a bulk response
		return toResponse(requireEmployeeKpi(lastAssigned),
				Objects.requireNonNull(lastEmployeeName, "Employee name is required"));
	}

	public void unassignKPI(String employeeKPIId) {
		String safeId = Objects.requireNonNull(employeeKPIId, "Employee KPI ID is required");
		EmployeeKPI employeeKPI = employeeKPIRepository.findById(safeId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee KPI not found"));

		employeeKPIRepository.delete(Objects.requireNonNull(employeeKPI));
	}

	public List<EmployeeKPIResponse> getKPIsByEmployeeId(String employeeId) {
		UserAccount employee = userAccountRepository.findByEmployeeId(employeeId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

		return employeeKPIRepository.findByEmployeeId(employeeId).stream()
				.map(ek -> toResponse(requireEmployeeKpi(ek), employee.getFullName()))
				.collect(Collectors.toList());
	}

	public List<EmployeeKPIResponse> getKPIsByUserId(String userId) {
		String safeUserId = Objects.requireNonNull(userId, "User ID is required");
		UserAccount employee = userAccountRepository.findById(safeUserId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

		return employeeKPIRepository.findByUserId(safeUserId).stream()
				.map(ek -> toResponse(requireEmployeeKpi(ek), employee.getFullName()))
				.collect(Collectors.toList());
	}

	public List<EmployeeKPIResponse> getAllAssignments() {
		return employeeKPIRepository.findAll().stream()
				.map(ek -> {
					String employeeName = userAccountRepository.findById(Objects.requireNonNull(ek.getUserId(), "User ID is required"))
							.map(UserAccount::getFullName)
							.orElse("Unknown");
					return toResponse(requireEmployeeKpi(ek), employeeName);
				})
				.collect(Collectors.toList());
	}

	public List<EmployeeKPIResponse> getAssignmentsByKpiCategoryId(String kpiCategoryId) {
		return employeeKPIRepository.findByKpiCategoryId(kpiCategoryId).stream()
				.map(ek -> {
					String employeeName = userAccountRepository.findById(Objects.requireNonNull(ek.getUserId(), "User ID is required"))
							.map(UserAccount::getFullName)
							.orElse("Unknown");
					return toResponse(requireEmployeeKpi(ek), employeeName);
				})
				.collect(Collectors.toList());
	}

	public List<EmployeeKPIResponse> getAssignmentsByKpiId(String kpiId) {
		return employeeKPIRepository.findByKpiId(kpiId).stream()
				.map(ek -> {
					String employeeName = userAccountRepository.findById(Objects.requireNonNull(ek.getUserId(), "User ID is required"))
							.map(UserAccount::getFullName)
							.orElse("Unknown");
					return toResponse(requireEmployeeKpi(ek), employeeName);
				})
				.collect(Collectors.toList());
	}

	public EmployeeKPIResponse updateAssignmentStatus(String assignmentId, UpdateEmployeeKPIStatusRequest request) {
		String safeAssignmentId = Objects.requireNonNull(assignmentId, "Assignment ID is required");
		EmployeeKPI assignment = employeeKPIRepository.findById(safeAssignmentId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "KPI assignment not found"));

		String normalizedStatus = request.status() != null ? request.status().trim().toUpperCase() : "";
		if (!normalizedStatus.equals("COMPLETED") && !normalizedStatus.equals("INCOMPLETE") && !normalizedStatus.equals("PENDING")) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status must be Pending, Completed, or Incomplete");
		}

		LocalDate dueDate = assignment.getDueDate();
		LocalDate today = LocalDate.now();
		if (dueDate != null && today.isBefore(dueDate)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You can only evaluate after the KPI due date");
		}

		double targetValue = parseTargetValue(assignment.getMeasurableValue());
		double currentProgress = assignment.getCurrentProgressValue() != null ? assignment.getCurrentProgressValue() : 0.0;

		if (normalizedStatus.equals("COMPLETED") && targetValue > 0 && currentProgress < targetValue) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Progress does not meet the target value. Cannot mark as Completed.");
		}

		String evaluationNote = StringUtils.hasText(request.evaluationNote()) ? request.evaluationNote().trim() : null;

		if (normalizedStatus.equals("PENDING")) {
			assignment.setEvaluatedAt(null);
			assignment.setEvaluationNote(null);
		} else {
			assignment.setEvaluatedAt(Instant.now());
			assignment.setEvaluationNote(evaluationNote);
		}

		assignment.setStatus(normalizedStatus);
		assignment.setUpdatedAt(Instant.now());

		EmployeeKPI saved = employeeKPIRepository.save(assignment);
		String employeeName = userAccountRepository.findById(Objects.requireNonNull(saved.getUserId(), "User ID is required"))
				.map(UserAccount::getFullName)
				.orElse("Unknown");
		return toResponse(requireEmployeeKpi(saved), employeeName);
	}

	public EmployeeKPIResponse updateProgress(String assignmentId, String userId, double progressValue,
			MultipartFile evidence) {
		if (progressValue <= 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Progress value must be greater than 0");
		}
		if (evidence == null || evidence.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Evidence file is required");
		}

		String safeAssignmentId = Objects.requireNonNull(assignmentId, "Assignment ID is required");
		EmployeeKPI assignment = employeeKPIRepository.findById(safeAssignmentId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "KPI assignment not found"));

		if (!assignment.getUserId().equals(userId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to update this KPI");
		}

		LocalDate dueDate = assignment.getDueDate();
		LocalDate today = LocalDate.now();
		if (dueDate != null && (today.isEqual(dueDate) || today.isAfter(dueDate))) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "KPI due date has been reached. Progress locked");
		}

		double targetValue = parseTargetValue(assignment.getMeasurableValue());
		if (targetValue <= 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid measurable value for this KPI");
		}

		double newTotalProgress = (assignment.getCurrentProgressValue() != null ? assignment.getCurrentProgressValue() : 0.0)
				+ progressValue;
		double clampedProgress = Math.min(newTotalProgress, targetValue);
		double progressPercentage = (clampedProgress / targetValue) * 100.0;

		String storedFilename = storeEvidence(evidence);
		if (assignment.getEvidenceFilename() != null) {
			deleteEvidenceFile(assignment.getEvidenceFilename());
		}

		assignment.setCurrentProgressValue(clampedProgress);
		assignment.setProgressPercentage(progressPercentage);
		assignment.setEvidenceFilename(storedFilename);
		assignment.setEvidenceOriginalName(evidence.getOriginalFilename());
		assignment.setEvidenceUploadedAt(Instant.now());
		assignment.setUpdatedAt(Instant.now());

		EmployeeKPI saved = employeeKPIRepository.save(assignment);
		String employeeName = userAccountRepository.findById(Objects.requireNonNull(saved.getUserId(), "User ID is required"))
				.map(UserAccount::getFullName)
				.orElse("Unknown");
		return toResponse(requireEmployeeKpi(saved), employeeName);
	}

	private EmployeeKPIResponse toResponse(@NonNull EmployeeKPI employeeKPI, String employeeName) {
		return new EmployeeKPIResponse(
				employeeKPI.getId(),
				employeeKPI.getUserId(),
				employeeKPI.getEmployeeId(),
				employeeName,
				employeeKPI.getKpiCategoryId(),
				employeeKPI.getKpiId(),
				employeeKPI.getKpiName(),
				employeeKPI.getDescription(),
				employeeKPI.getMeasurableValue(),
				employeeKPI.getDueDate(),
				employeeKPI.getBonusAmount(),
				employeeKPI.getCurrentProgressValue(),
				employeeKPI.getProgressPercentage(),
				employeeKPI.getEvidenceFilename(),
				employeeKPI.getEvidenceOriginalName(),
				employeeKPI.getEvidenceUploadedAt(),
				employeeKPI.getAssignedAt(),
				employeeKPI.getUpdatedAt(),
				employeeKPI.getStatus(),
				employeeKPI.getEvaluationNote(),
				employeeKPI.getEvaluatedAt());
	}

	private double parseTargetValue(String measurableValue) {
		if (!StringUtils.hasText(measurableValue)) {
			return 0;
		}
		try {
			return Double.parseDouble(measurableValue.trim());
		} catch (NumberFormatException ex) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Measurable value must be numeric");
		}
	}

	private String storeEvidence(MultipartFile evidence) {
		String cleanedOriginalName = StringUtils.cleanPath(
				Objects.requireNonNull(evidence.getOriginalFilename(), "Evidence filename is required"));
		String extension = "";
		int extIndex = cleanedOriginalName.lastIndexOf('.');
		if (extIndex >= 0) {
			extension = cleanedOriginalName.substring(extIndex);
		}
		String storedName = UUID.randomUUID().toString() + extension;
		try {
			Files.copy(evidence.getInputStream(), evidenceDirectory.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
			return storedName;
		} catch (IOException e) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store evidence file");
		}
	}

	private void deleteEvidenceFile(String filename) {
		if (!StringUtils.hasText(filename)) {
			return;
		}
		try {
			Files.deleteIfExists(evidenceDirectory.resolve(filename));
		} catch (IOException ignored) {
		}
	}

	private @NonNull EmployeeKPI requireEmployeeKpi(EmployeeKPI employeeKpi) {
		return Objects.requireNonNull(employeeKpi, "Employee KPI record is required");
	}
}

