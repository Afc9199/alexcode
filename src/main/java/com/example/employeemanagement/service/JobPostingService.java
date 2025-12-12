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

import com.example.employeemanagement.dto.JobPostingRequest;
import com.example.employeemanagement.dto.JobPostingResponse;
import com.example.employeemanagement.model.JobPosting;
import com.example.employeemanagement.model.UserAccount;
import com.example.employeemanagement.repository.JobPostingRepository;
import com.example.employeemanagement.repository.UserAccountRepository;

@Service
public class JobPostingService {

	private final JobPostingRepository repository;
	private final ResumeService resumeService;
	private final UserAccountRepository userAccountRepository;

	public JobPostingService(JobPostingRepository repository, ResumeService resumeService, UserAccountRepository userAccountRepository) {
		this.repository = repository;
		this.resumeService = resumeService;
		this.userAccountRepository = userAccountRepository;
	}

	public List<JobPostingResponse> findAll() {
		return repository.findAll().stream()
				.map(this::toResponse)
				.collect(Collectors.toList());
	}

	public List<JobPostingResponse> search(String searchTerm, String statusFilter) {
		List<JobPosting> jobPostings;

		if (StringUtils.hasText(searchTerm)) {
			String trimmed = searchTerm.trim();
			jobPostings = repository.findByJobPostingIdContainingIgnoreCaseOrJobTitleContainingIgnoreCase(trimmed, trimmed);
		} else {
			jobPostings = repository.findAll();
		}

		if (StringUtils.hasText(statusFilter) && !"all".equalsIgnoreCase(statusFilter.trim())) {
			String normalizedStatus = statusFilter.trim();
			jobPostings = jobPostings.stream()
					.filter(job -> normalizedStatus.equalsIgnoreCase(job.getStatus()))
					.collect(Collectors.toList());
		}

		return jobPostings.stream()
				.map(this::toResponse)
				.collect(Collectors.toList());
	}

	public JobPostingResponse findById(String id) {
		String safeId = Objects.requireNonNull(id, "Job posting ID is required");
		JobPosting jobPosting = repository.findById(safeId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job posting not found"));
		return toResponse(jobPosting);
	}

	public JobPostingResponse findByJobPostingId(String jobPostingId) {
		String safeJobPostingId = Objects.requireNonNull(jobPostingId, "Job posting ID is required");
		JobPosting jobPosting = repository.findByJobPostingId(safeJobPostingId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job posting not found"));
		return toResponse(jobPosting);
	}

	public JobPostingResponse create(JobPostingRequest request, String createdBy) {
		// Validate status
		if (!"Available".equals(request.status()) && !"Hired".equals(request.status())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status must be either 'Available' or 'Hired'");
		}

		// Fetch admin user to get employee ID and name
		String safeCreatedBy = Objects.requireNonNull(createdBy, "Creator user ID is required");
		UserAccount adminUser = userAccountRepository.findById(safeCreatedBy)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin user not found"));

		JobPosting jobPosting = new JobPosting(
				request.jobTitle().trim(),
				request.jobDescription() != null ? request.jobDescription().trim() : null,
				request.createdDate(),
				request.status(),
				safeCreatedBy);

		// Set admin employee ID and name
		jobPosting.setCreatedByEmployeeId(adminUser.getEmployeeId() != null ? adminUser.getEmployeeId() : "");
		jobPosting.setCreatedByName(adminUser.getFullName() != null ? adminUser.getFullName() : "");

		// Generate Job Posting ID
		jobPosting.setJobPostingId(generateNextJobPostingId());

		JobPosting saved = repository.save(jobPosting);
		return toResponse(saved);
	}

	public JobPostingResponse update(String id, JobPostingRequest request) {
		JobPosting jobPosting = repository.findById(requireJobPostingId(id))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job posting not found"));

		// Validate status
		if (!"Available".equals(request.status()) && !"Hired".equals(request.status())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status must be either 'Available' or 'Hired'");
		}

		jobPosting.setJobTitle(request.jobTitle().trim());
		jobPosting.setJobDescription(request.jobDescription() != null ? request.jobDescription().trim() : null);
		jobPosting.setCreatedDate(request.createdDate());
		jobPosting.setStatus(request.status());
		jobPosting.setUpdatedAt(Instant.now());

		return toResponse(repository.save(jobPosting));
	}

	public void delete(String id) {
		String safeId = requireJobPostingId(id);
		JobPosting jobPosting = repository.findById(safeId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job posting not found"));

		// Cascade delete: Delete all resumes (including files) for this job posting
		if (jobPosting.getJobPostingId() != null) {
			resumeService.deleteAllByJobPostingId(jobPosting.getJobPostingId());
		}

		// Delete the job posting
		repository.deleteById(safeId);
	}

	public String getNextJobPostingId() {
		return generateNextJobPostingId();
	}

	private String generateNextJobPostingId() {
		return repository.findTopByOrderByJobPostingIdDesc()
				.map(jobPosting -> {
					String lastId = jobPosting.getJobPostingId();
					if (lastId != null && lastId.startsWith("J")) {
						try {
							int num = Integer.parseInt(lastId.substring(1));
							return String.format("J%03d", num + 1);
						} catch (NumberFormatException e) {
							return "J001";
						}
					}
					return "J001";
				})
				.orElse("J001");
	}

	private JobPostingResponse toResponse(JobPosting jobPosting) {
		return new JobPostingResponse(
				jobPosting.getId(),
				jobPosting.getJobPostingId(),
				jobPosting.getJobTitle(),
				jobPosting.getJobDescription(),
				jobPosting.getCreatedDate(),
				jobPosting.getStatus(),
				jobPosting.getCreatedBy(),
				jobPosting.getCreatedByEmployeeId(),
				jobPosting.getCreatedByName(),
				jobPosting.getCreatedAt(),
				jobPosting.getUpdatedAt());
	}

	private @NonNull String requireJobPostingId(String id) {
		return Objects.requireNonNull(id, "Job posting ID is required");
	}
}

