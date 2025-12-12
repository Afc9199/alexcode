package com.example.employeemanagement.service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.employeemanagement.dto.AssignBenefitRequest;
import com.example.employeemanagement.dto.EmployeeBenefitResponse;
import com.example.employeemanagement.model.BenefitCategory;
import com.example.employeemanagement.model.EmployeeBenefit;
import com.example.employeemanagement.model.UserAccount;
import com.example.employeemanagement.repository.BenefitCategoryRepository;
import com.example.employeemanagement.repository.EmployeeBenefitRepository;
import com.example.employeemanagement.repository.UserAccountRepository;

@Service
public class EmployeeBenefitService {

	private final EmployeeBenefitRepository employeeBenefitRepository;
	private final UserAccountRepository userAccountRepository;
	private final BenefitCategoryRepository benefitCategoryRepository;

	public EmployeeBenefitService(EmployeeBenefitRepository employeeBenefitRepository,
			UserAccountRepository userAccountRepository, BenefitCategoryRepository benefitCategoryRepository) {
		this.employeeBenefitRepository = employeeBenefitRepository;
		this.userAccountRepository = userAccountRepository;
		this.benefitCategoryRepository = benefitCategoryRepository;
	}

	public EmployeeBenefitResponse assignBenefit(AssignBenefitRequest request) {
		// Find employee by Employee ID
		UserAccount employee = userAccountRepository.findByEmployeeId(requireEmployeeId(request.employeeId()))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

		// Find benefit category
		BenefitCategory benefitCategory = benefitCategoryRepository.findById(requireBenefitCategoryId(request.benefitCategoryId()))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Benefit category not found"));
		if (Boolean.FALSE.equals(benefitCategory.getActive())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Benefit category is inactive");
		}

		// Check if benefit is already assigned to this employee
		employeeBenefitRepository.findByEmployeeIdAndBenefitCategoryId(requireEmployeeId(request.employeeId()),
				requireBenefitCategoryId(request.benefitCategoryId()))
				.ifPresent(existing -> {
					throw new ResponseStatusException(HttpStatus.CONFLICT,
							"This benefit is already assigned to this employee");
				});

		// Create new employee benefit assignment
		EmployeeBenefit employeeBenefit = new EmployeeBenefit(
				employee.getId(),
				employee.getEmployeeId(),
				benefitCategory.getId(),
				benefitCategory.getBenefitId(),
				benefitCategory.getName(),
				benefitCategory.getBenefitAmount());

		EmployeeBenefit saved = employeeBenefitRepository.save(employeeBenefit);
		return toResponse(saved, employee.getFullName());
	}

	public void unassignBenefit(String employeeBenefitId) {
		String safeId = Objects.requireNonNull(employeeBenefitId, "Employee benefit ID is required");
		EmployeeBenefit employeeBenefit = employeeBenefitRepository.findById(safeId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee benefit not found"));
		employeeBenefitRepository.delete(requireBenefitRecord(employeeBenefit));
	}

	public List<EmployeeBenefitResponse> getBenefitsByEmployeeId(String employeeId) {
		String safeEmployeeId = requireEmployeeId(employeeId);
		UserAccount employee = userAccountRepository.findByEmployeeId(safeEmployeeId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

		return employeeBenefitRepository.findByEmployeeId(safeEmployeeId).stream()
				.map(eb -> toResponse(requireBenefitRecord(eb), employee.getFullName()))
				.collect(Collectors.toList());
	}

	public List<EmployeeBenefitResponse> getBenefitsByUserId(String userId) {
		String safeUserId = requireUserId(userId);
		UserAccount employee = userAccountRepository.findById(safeUserId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

		return employeeBenefitRepository.findByUserId(safeUserId).stream()
				.map(eb -> toResponse(requireBenefitRecord(eb), employee.getFullName()))
				.collect(Collectors.toList());
	}

	public List<EmployeeBenefitResponse> getAllAssignments() {
		return employeeBenefitRepository.findAll().stream()
				.map(eb -> {
					String employeeName = userAccountRepository.findById(requireUserId(eb.getUserId()))
							.map(UserAccount::getFullName)
							.orElse("Unknown");
					return toResponse(requireBenefitRecord(eb), employeeName);
				})
				.collect(Collectors.toList());
	}

	public List<EmployeeBenefitResponse> getAssignmentsByBenefitCategoryId(String benefitCategoryId) {
		return employeeBenefitRepository.findByBenefitCategoryId(requireBenefitCategoryId(benefitCategoryId)).stream()
				.map(eb -> {
					String employeeName = userAccountRepository.findById(requireUserId(eb.getUserId()))
							.map(UserAccount::getFullName)
							.orElse("Unknown");
					return toResponse(requireBenefitRecord(eb), employeeName);
				})
				.collect(Collectors.toList());
	}

	private EmployeeBenefitResponse toResponse(@NonNull EmployeeBenefit employeeBenefit, String employeeName) {
		return new EmployeeBenefitResponse(
				employeeBenefit.getId(),
				employeeBenefit.getUserId(),
				employeeBenefit.getEmployeeId(),
				employeeName,
				employeeBenefit.getBenefitCategoryId(),
				employeeBenefit.getBenefitId(),
				employeeBenefit.getBenefitName(),
				employeeBenefit.getBenefitAmount(),
				employeeBenefit.getAssignedAt(),
				employeeBenefit.getUpdatedAt());
	}

	private @NonNull String requireEmployeeId(String employeeId) {
		return java.util.Objects.requireNonNull(employeeId, "Employee ID is required");
	}

	private @NonNull String requireBenefitCategoryId(String benefitCategoryId) {
		return java.util.Objects.requireNonNull(benefitCategoryId, "Benefit category ID is required");
	}

	private @NonNull String requireUserId(String userId) {
		return Objects.requireNonNull(userId, "User ID is required");
	}

	private @NonNull EmployeeBenefit requireBenefitRecord(EmployeeBenefit benefit) {
		return Objects.requireNonNull(benefit, "Employee benefit record is required");
	}
}

