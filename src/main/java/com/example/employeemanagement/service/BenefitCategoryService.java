package com.example.employeemanagement.service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.example.employeemanagement.dto.BenefitCategoryRequest;
import com.example.employeemanagement.dto.BenefitCategoryResponse;
import com.example.employeemanagement.model.BenefitCategory;
import com.example.employeemanagement.repository.BenefitCategoryRepository;
import com.example.employeemanagement.repository.EmployeeBenefitRepository;

@Service
public class BenefitCategoryService {

	private final BenefitCategoryRepository repository;
	private final EmployeeBenefitRepository employeeBenefitRepository;

	public BenefitCategoryService(BenefitCategoryRepository repository, EmployeeBenefitRepository employeeBenefitRepository) {
		this.repository = repository;
		this.employeeBenefitRepository = employeeBenefitRepository;
	}

	public List<BenefitCategoryResponse> findAll() {
		// Initialize benefitId for any existing records that don't have one
		initializeMissingFields();
		
		return repository.findAllByOrderByBenefitIdAsc().stream()
				.map(this::toResponse)
				.collect(Collectors.toList());
	}

	/**
	 * Initialize benefitId and ensure benefitAmount is set for existing records
	 * that were created before these fields were added.
	 */
	private void initializeMissingFields() {
		List<BenefitCategory> categoriesNeedingUpdate = repository.findAll().stream()
				.filter(cat -> cat.getBenefitId() == null || cat.getBenefitId().isEmpty() || cat.getBenefitAmount() == null)
				.collect(Collectors.toList());
		
		if (!categoriesNeedingUpdate.isEmpty()) {
			// Get the highest existing benefitId to continue from there
			String highestId = repository.findTopByOrderByBenefitIdDesc()
					.map(BenefitCategory::getBenefitId)
					.filter(id -> id != null && id.startsWith("B"))
					.orElse(null);
			
			int startNumber = 1;
			if (highestId != null) {
				try {
					startNumber = Integer.parseInt(highestId.substring(1)) + 1;
				} catch (NumberFormatException e) {
					startNumber = 1;
				}
			}
			
			// Assign benefitId to records without one, and set default benefitAmount if null
			for (BenefitCategory category : categoriesNeedingUpdate) {
				if (category.getBenefitId() == null || category.getBenefitId().isEmpty()) {
					category.setBenefitId(String.format("B%03d", startNumber++));
				}
				if (category.getBenefitAmount() == null) {
					category.setBenefitAmount(0.0); // Set default, admin can update later
				}
				repository.save(category);
			}
		}
	}

	public List<BenefitCategoryResponse> search(String searchTerm) {
		if (!StringUtils.hasText(searchTerm)) {
			return findAll();
		}
		// Search by name and then sort by Benefit ID
		return repository.findByNameContainingIgnoreCase(searchTerm.trim()).stream()
				.sorted((a, b) -> {
					// Sort by Benefit ID (handle nulls)
					String idA = a.getBenefitId() != null ? a.getBenefitId() : "";
					String idB = b.getBenefitId() != null ? b.getBenefitId() : "";
					return idA.compareTo(idB);
				})
				.map(this::toResponse)
				.collect(Collectors.toList());
	}

	public BenefitCategoryResponse findById(String id) {
		BenefitCategory category = repository.findById(requireId(id))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Benefit category not found"));
		return toResponse(Objects.requireNonNull(category));
	}

	public BenefitCategoryResponse findByBenefitId(String benefitId) {
		BenefitCategory category = repository.findByBenefitId(requireBenefitId(benefitId))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Benefit category not found"));
		return toResponse(Objects.requireNonNull(category));
	}

	public String getNextBenefitId() {
		return generateNextBenefitId();
	}

	/**
	 * Generates the next Benefit ID by retrieving the highest existing Benefit ID from the database
	 * and incrementing it. This ensures sequential, human-readable IDs (B001, B002, B003, etc.).
	 * 
	 * The method:
	 * 1. Queries the database using findTopByOrderByBenefitIdDesc() to get the record with the highest Benefit ID
	 * 2. Extracts the numeric part from the Benefit ID (e.g., "001" from "B001")
	 * 3. Increments the number by 1
	 * 4. Formats it back to the next Benefit ID (e.g., "B002")
	 * 
	 * @return The next available Benefit ID (e.g., "B001", "B002", "B003")
	 */
	private String generateNextBenefitId() {
		// Query the database to retrieve the highest Benefit ID
		return repository.findTopByOrderByBenefitIdDesc()
				.map(lastCategory -> {
					String lastId = lastCategory.getBenefitId();
					// Only process if benefitId exists and starts with "B"
					if (lastId != null && lastId.startsWith("B")) {
						try {
							// Extract the number part (e.g., "001" from "B001")
							int number = Integer.parseInt(lastId.substring(1));
							// Increment and format back to "B002", "B003", etc.
							return String.format("B%03d", number + 1);
						} catch (NumberFormatException e) {
							// If parsing fails, start from B001
							return "B001";
						}
					}
					// If benefitId is null or doesn't start with "B", start from B001
					return "B001";
				})
				.orElse("B001"); // If no records exist in database, start from B001
	}

	public BenefitCategoryResponse create(BenefitCategoryRequest request) {
		// Check if name already exists
		String name = requireName(request.name());
		if (repository.findByNameIgnoreCase(name).isPresent()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Benefit category with this name already exists");
		}

		// Validate benefit amount
		if (request.benefitAmount() == null || request.benefitAmount() <= 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Benefit amount must be greater than 0");
		}

		// Generate the next Benefit ID
		String nextBenefitId = generateNextBenefitId();
		
		// Create new benefit category
		BenefitCategory category = new BenefitCategory(
				name,
				request.description() != null ? request.description().trim() : null,
				request.benefitAmount());
		
		// Set the Benefit ID
		category.setBenefitId(nextBenefitId);
		
		// Save to database
		BenefitCategory savedCategory = repository.save(category);
		
		// Return response
		return toResponse(savedCategory);
	}

	public BenefitCategoryResponse update(String id, BenefitCategoryRequest request) {
		BenefitCategory category = repository.findById(requireId(id))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Benefit category not found"));

		// Check if new name conflicts with existing category (excluding current one)
		String name = requireName(request.name());
		repository.findByNameIgnoreCase(name)
				.ifPresent(existing -> {
					if (!existing.getId().equals(id)) {
						throw new ResponseStatusException(HttpStatus.CONFLICT, "Benefit category with this name already exists");
					}
				});

		// Validate benefit amount
		if (request.benefitAmount() == null || request.benefitAmount() <= 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Benefit amount must be greater than 0");
		}

		category.setName(name);
		category.setDescription(request.description() != null ? request.description().trim() : null);
		category.setBenefitAmount(request.benefitAmount());
		
		return toResponse(repository.save(category));
	}

	public BenefitCategoryResponse updateStatus(String id, boolean active) {
		BenefitCategory category = repository.findById(requireId(id))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Benefit category not found"));
		category.setActive(active);
		BenefitCategory saved = repository.save(category);
		return toResponse(saved);
	}

	public void delete(String id) {
		BenefitCategory category = repository.findById(requireId(id))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Benefit category not found"));
		
		// Cascade delete: Delete all employee benefit assignments for this benefit category
		// This must be done BEFORE deleting the category to ensure referential integrity
		// Use the category's ID to delete all related employee benefit assignments
		String benefitCategoryId = category.getId();
		employeeBenefitRepository.deleteByBenefitCategoryId(benefitCategoryId);
		
		// Finally, delete the benefit category
		repository.deleteById(Objects.requireNonNull(category.getId(), "Benefit category ID is required"));
	}

	private BenefitCategoryResponse toResponse(@NonNull BenefitCategory category) {
		return new BenefitCategoryResponse(
				category.getId(),
				category.getBenefitId(),
				category.getName(),
				category.getDescription(),
				category.getBenefitAmount(),
				category.getActive(),
				category.getCreatedAt(),
				category.getUpdatedAt());
	}

	private @NonNull String requireId(String id) {
		return Objects.requireNonNull(id, "Benefit category ID is required");
	}

	private @NonNull String requireBenefitId(String benefitId) {
		return Objects.requireNonNull(benefitId, "Benefit ID is required");
	}

	private @NonNull String requireName(String name) {
		String validatedName = Objects.requireNonNull(name, "Benefit category name is required");
		String trimmed = validatedName.trim();
		return Objects.requireNonNull(trimmed, "Benefit category name is required");
	}
}

