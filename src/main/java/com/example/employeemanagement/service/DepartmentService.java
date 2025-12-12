package com.example.employeemanagement.service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.example.employeemanagement.dto.DepartmentRequest;
import com.example.employeemanagement.dto.DepartmentResponse;
import com.example.employeemanagement.model.Department;
import com.example.employeemanagement.model.UserAccount;
import com.example.employeemanagement.repository.DepartmentRepository;
import com.example.employeemanagement.repository.UserAccountRepository;

@Service
public class DepartmentService {

	private final DepartmentRepository repository;
	private final UserAccountRepository userAccountRepository;

	public DepartmentService(DepartmentRepository repository, UserAccountRepository userAccountRepository) {
		this.repository = repository;
		this.userAccountRepository = userAccountRepository;
	}

	public List<DepartmentResponse> findAll() {
		return repository.findAllByOrderByNameAsc().stream()
				.map(this::toResponse)
				.collect(Collectors.toList());
	}

	public List<DepartmentResponse> search(String searchTerm) {
		if (!StringUtils.hasText(searchTerm)) {
			return findAll();
		}
		String trimmedSearch = Objects.requireNonNull(searchTerm, "Search term is required").trim();
		return repository.findByNameContainingIgnoreCase(trimmedSearch).stream()
				.map(this::toResponse)
				.collect(Collectors.toList());
	}

	public DepartmentResponse findById(String id) {
		Department department = repository.findById(requireId(id))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found"));
		return toResponse(Objects.requireNonNull(department));
	}

	public DepartmentResponse create(DepartmentRequest request) {
		String name = requireName(request.name());
		// Check if name already exists
		if (repository.findByNameIgnoreCase(name).isPresent()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Department with this name already exists");
		}

		Department department = new Department(
				name,
				sanitizeDescription(request.description()));
		
		Department saved = repository.save(department);
		return toResponse(Objects.requireNonNull(saved));
	}

	public DepartmentResponse update(String id, DepartmentRequest request) {
		Department department = repository.findById(requireId(id))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found"));

		String previousName = department.getName();
		String newName = requireName(request.name());

		// Check if new name conflicts with existing department (excluding current one)
		repository.findByNameIgnoreCase(newName)
				.ifPresent(existing -> {
					if (!existing.getId().equals(id)) {
						throw new ResponseStatusException(HttpStatus.CONFLICT, "Department with this name already exists");
					}
				});

		department.setName(newName);
		department.setDescription(sanitizeDescription(request.description()));
		department.setUpdatedAt(Instant.now());

		Department saved = repository.save(department);

		if (!previousName.equals(newName)) {
			List<UserAccount> employees = userAccountRepository.findByDepartment(previousName);
			for (UserAccount employee : employees) {
				employee.setDepartment(newName);
				userAccountRepository.save(employee);
			}
		}

		return toResponse(Objects.requireNonNull(saved));
	}

	public void delete(String id) {
		String safeId = requireId(id);
		Department department = repository.findById(safeId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found"));

		List<UserAccount> employees = userAccountRepository.findByDepartment(department.getName());
		for (UserAccount employee : employees) {
			employee.setDepartment("");
			userAccountRepository.save(employee);
		}

		repository.deleteById(safeId);
	}

	private DepartmentResponse toResponse(@NonNull Department department) {
		return new DepartmentResponse(
				department.getId(),
				department.getName(),
				department.getDescription(),
				department.getCreatedAt(),
				department.getUpdatedAt());
	}

	private @NonNull String requireId(String id) {
		return Objects.requireNonNull(id, "Department ID is required");
	}

	private @NonNull String requireName(String name) {
		String validatedName = Objects.requireNonNull(name, "Department name is required").trim();
		return Objects.requireNonNull(validatedName, "Department name is required");
	}

	private String sanitizeDescription(String description) {
		return description != null ? description.trim() : null;
	}
}

