package com.example.employeemanagement.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.example.employeemanagement.dto.KPIRequest;
import com.example.employeemanagement.dto.KPIResponse;
import com.example.employeemanagement.model.KPI;
import com.example.employeemanagement.repository.EmployeeKPIRepository;
import com.example.employeemanagement.repository.KPIRepository;

@Service
public class KPIService {

	private final KPIRepository repository;
	private final EmployeeKPIRepository employeeKPIRepository;

	public KPIService(KPIRepository repository, EmployeeKPIRepository employeeKPIRepository) {
		this.repository = repository;
		this.employeeKPIRepository = employeeKPIRepository;
	}

	public List<KPIResponse> findAll() {
		return repository.findAllByOrderByKpiIdAsc().stream()
				.map(this::toResponse)
				.collect(Collectors.toList());
	}

	public List<KPIResponse> search(String searchTerm) {
		if (!StringUtils.hasText(searchTerm)) {
			return findAll();
		}
		String trimmed = Objects.requireNonNull(searchTerm, "Search term is required when provided").trim();
		return repository.findByNameContainingIgnoreCase(trimmed).stream()
				.map(this::toResponse)
				.collect(Collectors.toList());
	}

	public KPIResponse findById(String id) {
		KPI kpi = repository.findById(requireKpiId(id))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "KPI not found"));
		return toResponse(kpi);
	}

	public KPIResponse findByKpiId(String kpiId) {
		KPI kpi = repository.findByKpiId(requireKpiId(kpiId))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "KPI not found"));
		return toResponse(kpi);
	}

	public KPIResponse create(KPIRequest request) {
		String kpiName = requireKpiName(request.name());
		// Check if name already exists
		if (repository.findByNameIgnoreCase(kpiName).isPresent()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "KPI with this name already exists");
		}

		// Validate measurable value is numeric and > 0
		if (request.measurableValue() != null && !request.measurableValue().trim().isEmpty()) {
			String measurableValue = request.measurableValue().trim();
			// Check if it contains only digits
			if (!measurableValue.matches("^\\d+$")) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Measurable value must contain digits only");
			}
			// Check if it starts with 0 (but allow single "0" which will be caught by > 0 check)
			if (measurableValue.length() > 1 && measurableValue.startsWith("0")) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Measurable value cannot start with 0");
			}
			// Check if it's greater than 0
			try {
				long value = Long.parseLong(measurableValue);
				if (value <= 0) {
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Measurable value must be greater than 0");
				}
			} catch (NumberFormatException e) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Measurable value must be a valid number");
			}
		}

		validateDueDate(request.dueDate());

		KPI kpi = new KPI(
				kpiName,
				request.description() != null ? request.description().trim() : null);
		
		// Generate KPI ID
		kpi.setKpiId(generateNextKpiId());
		
		// Set new fields
		kpi.setMeasurableValue(request.measurableValue() != null ? request.measurableValue().trim() : null);
		kpi.setDueDate(request.dueDate());
		kpi.setBonusAmount(request.bonusAmount());
		
		kpi.setActive(Boolean.TRUE);
		KPI saved = repository.save(kpi);
		return toResponse(saved);
	}

	public KPIResponse update(String id, KPIRequest request) {
		String safeId = requireKpiId(id);
		KPI kpi = repository.findById(safeId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "KPI not found"));

		String kpiName = requireKpiName(request.name());
		// Check if new name conflicts with existing KPI (excluding current one)
		repository.findByNameIgnoreCase(kpiName)
				.ifPresent(existing -> {
					if (!existing.getId().equals(safeId)) {
						throw new ResponseStatusException(HttpStatus.CONFLICT, "KPI with this name already exists");
					}
				});

		// Validate measurable value is numeric and > 0
		if (request.measurableValue() != null && !request.measurableValue().trim().isEmpty()) {
		validateDueDate(request.dueDate());
			String measurableValue = request.measurableValue().trim();
			// Check if it contains only digits
			if (!measurableValue.matches("^\\d+$")) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Measurable value must contain digits only");
			}
			// Check if it starts with 0 (but allow single "0" which will be caught by > 0 check)
			if (measurableValue.length() > 1 && measurableValue.startsWith("0")) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Measurable value cannot start with 0");
			}
			// Check if it's greater than 0
			try {
				long value = Long.parseLong(measurableValue);
				if (value <= 0) {
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Measurable value must be greater than 0");
				}
			} catch (NumberFormatException e) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Measurable value must be a valid number");
			}
		}

		kpi.setName(kpiName);
		kpi.setDescription(request.description() != null ? request.description().trim() : null);
		kpi.setMeasurableValue(request.measurableValue() != null ? request.measurableValue().trim() : null);
		kpi.setDueDate(request.dueDate());
		kpi.setBonusAmount(request.bonusAmount());
		kpi.setUpdatedAt(Instant.now());
		
		return toResponse(repository.save(kpi));
	}

	public KPIResponse updateStatus(String id, boolean active) {
		KPI kpi = repository.findById(requireKpiId(id))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "KPI not found"));
		kpi.setActive(active);
		return toResponse(repository.save(kpi));
	}

	public void delete(String id) {
		String safeDeleteId = requireKpiId(id);
		KPI kpi = repository.findById(safeDeleteId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "KPI not found"));
		
		// Cascade delete: Delete all employee KPI assignments for this KPI category
		// This must be done BEFORE deleting the KPI to ensure referential integrity
		String kpiCategoryId = kpi.getId();
		employeeKPIRepository.deleteByKpiCategoryId(kpiCategoryId);
		
		// Also delete by KPI ID (human-readable ID like K001) for additional safety
		if (kpi.getKpiId() != null) {
			employeeKPIRepository.deleteByKpiId(kpi.getKpiId());
		}
		
		// Finally, delete the KPI category
		repository.deleteById(safeDeleteId);
	}

	public String getNextKpiId() {
		return generateNextKpiId();
	}

	private String generateNextKpiId() {
		return repository.findTopByOrderByKpiIdDesc()
				.map(kpi -> {
					String lastId = kpi.getKpiId();
					if (lastId != null && lastId.startsWith("K")) {
						try {
							int num = Integer.parseInt(lastId.substring(1));
							return String.format("K%03d", num + 1);
						} catch (NumberFormatException e) {
							return "K001";
						}
					}
					return "K001";
				})
				.orElse("K001");
	}

	private KPIResponse toResponse(KPI kpi) {
		return new KPIResponse(
				kpi.getId(),
				kpi.getKpiId(),
				kpi.getName(),
				kpi.getDescription(),
				kpi.getMeasurableValue(),
				kpi.getDueDate(),
				kpi.getBonusAmount(),
				kpi.getActive(),
				kpi.getCreatedAt(),
				kpi.getUpdatedAt());
	}

	private void validateDueDate(LocalDate dueDate) {
		if (dueDate == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Due date is required");
		}
		if (dueDate.isBefore(LocalDate.now())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Due date cannot be in the past");
		}
	}

	private @NonNull String requireKpiId(String id) {
		return Objects.requireNonNull(id, "KPI ID is required");
	}

	private @NonNull String requireKpiName(String name) {
		String trimmed = Objects.requireNonNull(name, "KPI name is required").trim();
		return Objects.requireNonNull(trimmed, "KPI name is required");
	}
}

