package com.example.employeemanagement.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.example.employeemanagement.dto.ResumeRequest;
import com.example.employeemanagement.dto.ResumeResponse;
import com.example.employeemanagement.model.JobPosting;
import com.example.employeemanagement.model.Resume;
import com.example.employeemanagement.repository.JobPostingRepository;
import com.example.employeemanagement.repository.ResumeRepository;

import jakarta.annotation.PostConstruct;

@Service
public class ResumeService {

	private final ResumeRepository repository;
	private final JobPostingRepository jobPostingRepository;
	private Path resumeDirectory;

	@Value("${resume.upload.directory:uploads/resumes}")
	private String uploadDirectory;

	public ResumeService(ResumeRepository repository, JobPostingRepository jobPostingRepository) {
		this.repository = repository;
		this.jobPostingRepository = jobPostingRepository;
	}

	@PostConstruct
	public void init() {
		try {
			resumeDirectory = Paths.get(System.getProperty("user.dir"), "uploads", "resumes");
			if (!Files.exists(resumeDirectory)) {
				Files.createDirectories(resumeDirectory);
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to create resume upload directory", e);
		}
	}

	public List<ResumeResponse> findAll() {
		return repository.findAll().stream()
				.map(this::toResponse)
				.collect(Collectors.toList());
	}

	public List<ResumeResponse> findByJobPostingId(String jobPostingId) {
		return repository.findByJobPostingId(jobPostingId).stream()
				.map(this::toResponse)
				.collect(Collectors.toList());
	}

	public ResumeResponse findById(String id) {
		Resume resume = repository.findById(requireResumeId(id))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resume not found"));
		return toResponse(resume);
	}

	public ResumeResponse findByResumeId(String resumeId) {
		Resume resume = repository.findByResumeId(resumeId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resume not found"));
		return toResponse(resume);
	}

	public ResumeResponse create(ResumeRequest request, MultipartFile resumeFile) {
		// Validate job posting exists and is not hired
		JobPosting jobPosting = jobPostingRepository.findByJobPostingId(request.jobPostingId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job posting not found"));
		if ("Hired".equalsIgnoreCase(jobPosting.getStatus())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot add candidates to a job posting marked as Hired");
		}

		// Validate file is provided
		if (resumeFile == null || resumeFile.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resume file is required");
		}

		validateResumeFile(resumeFile);
		StoredResumeFile storedFile = storeResumeFile(resumeFile);

		// Normalize contact number to include +60 prefix
		String normalizedContactNumber = normalizeContactNumber(request.candidateContactNumber());

		// Create resume record
		Resume resume = new Resume(
				request.jobPostingId(),
				request.candidateName().trim(),
				request.candidateEmail().trim(),
				normalizedContactNumber,
				storedFile.storedName(),
				storedFile.originalName(),
				LocalDate.now()); // Auto-set upload date

		// Generate Resume ID
		resume.setResumeId(generateNextResumeId());

		Resume saved = repository.save(resume);
		return toResponse(saved);
	}

	public void delete(String id) {
		Resume resume = repository.findById(requireResumeId(id))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resume not found"));

		// Delete file
		deleteResumeFile(resume.getResumeFilename());

		// Delete resume record
		repository.deleteById(Objects.requireNonNull(resume.getId(), "Resume record ID is required"));
	}

	public void deleteByResumeId(String resumeId) {
		Resume resume = repository.findByResumeId(requireResumeId(resumeId))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resume not found"));

		// Delete file
		deleteResumeFile(resume.getResumeFilename());

		// Delete resume record
		repository.deleteByResumeId(Objects.requireNonNull(resume.getResumeId(), "Resume ID is required"));
	}

	public void deleteAllByJobPostingId(String jobPostingId) {
		// Get all resumes for this job posting
		List<Resume> resumes = repository.findByJobPostingId(Objects.requireNonNull(jobPostingId, "Job posting ID is required"));
		
		// Delete files for each resume
		for (Resume resume : resumes) {
			deleteResumeFile(resume.getResumeFilename());
		}
		
		// Delete all resume records
		repository.deleteByJobPostingId(jobPostingId);
	}

	public String getNextResumeId() {
		return generateNextResumeId();
	}

	public ResumeResponse update(String id, ResumeRequest request, MultipartFile resumeFile) {
		Resume resume = repository.findById(requireResumeId(id))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resume not found"));

		if (!resume.getJobPostingId().equals(request.jobPostingId())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot change job posting for this candidate");
		}

		resume.setCandidateName(request.candidateName().trim());
		resume.setCandidateEmail(request.candidateEmail().trim());
		resume.setCandidateContactNumber(normalizeContactNumber(request.candidateContactNumber()));

		if (resumeFile != null && !resumeFile.isEmpty()) {
			validateResumeFile(resumeFile);
			String previousFilename = resume.getResumeFilename();
			StoredResumeFile storedFile = storeResumeFile(resumeFile);
			resume.setResumeFilename(storedFile.storedName());
			resume.setResumeOriginalName(storedFile.originalName());
			resume.setUploadDate(LocalDate.now());
			if (StringUtils.hasText(previousFilename)) {
				deleteResumeFile(previousFilename);
			}
		}

		Resume saved = repository.save(resume);
		return toResponse(saved);
	}

	public String resolveOriginalFilename(String storedFilename) {
		String fallbackName = StringUtils.hasText(storedFilename) ? storedFilename : "resume";
		return repository.findByResumeFilename(fallbackName)
				.map(Resume::getResumeOriginalName)
				.filter(StringUtils::hasText)
				.orElse(fallbackName);
	}

	private String generateNextResumeId() {
		return repository.findTopByOrderByResumeIdDesc()
				.map(resume -> {
					String lastId = resume.getResumeId();
					if (lastId != null && lastId.startsWith("R")) {
						try {
							int num = Integer.parseInt(lastId.substring(1));
							return String.format("R%03d", num + 1);
						} catch (NumberFormatException e) {
							return "R001";
						}
					}
					return "R001";
				})
				.orElse("R001");
	}

	private @NonNull String requireResumeId(String id) {
		return Objects.requireNonNull(id, "Resume ID is required");
	}

	private boolean isValidFileType(String extension) {
		return extension.equals("pdf") || extension.equals("doc") || extension.equals("docx")
				|| extension.equals("jpg") || extension.equals("jpeg") || extension.equals("png");
	}

	private void validateResumeFile(MultipartFile resumeFile) {
		if (resumeFile.getSize() > 10 * 1024 * 1024) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File size must not exceed 10MB");
		}

		String originalFilename = resumeFile.getOriginalFilename();
		if (originalFilename != null) {
			int dotIndex = originalFilename.lastIndexOf('.');
			if (dotIndex >= 0) {
				String extension = originalFilename.substring(dotIndex + 1).toLowerCase();
				if (!isValidFileType(extension)) {
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
							"Invalid file type. Only PDF, DOC, DOCX, and image files are allowed");
				}
			}
		}
	}

	private StoredResumeFile storeResumeFile(MultipartFile resumeFile) {
		try {
			String originalFilename = resumeFile.getOriginalFilename();
			if (!StringUtils.hasText(originalFilename)) {
				originalFilename = "resume";
			}
			String cleanedOriginalName = StringUtils.cleanPath(Objects.requireNonNull(originalFilename, "Original filename is required"));
			String extension = "";
			int extIndex = cleanedOriginalName.lastIndexOf('.');
			if (extIndex >= 0) {
				extension = cleanedOriginalName.substring(extIndex);
			}
			String uniqueFilename = UUID.randomUUID().toString() + extension;
			Path filePath = resumeDirectory.resolve(uniqueFilename);
			Files.copy(resumeFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
			return new StoredResumeFile(uniqueFilename, cleanedOriginalName);
		} catch (IOException e) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save resume file: " + e.getMessage());
		}
	}

	private record StoredResumeFile(String storedName, String originalName) {
	}

	private String normalizeContactNumber(String rawContact) {
		if (!StringUtils.hasText(rawContact)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Candidate contact number is required");
		}
		String trimmed = rawContact.trim();
		String digitsOnly;
		if (trimmed.startsWith("+60")) {
			digitsOnly = trimmed.substring(3).replaceAll("[^0-9]", "");
		} else {
			digitsOnly = trimmed.replaceAll("[^0-9]", "");
		}
		if (!StringUtils.hasText(digitsOnly)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Candidate contact number is invalid");
		}
		if (digitsOnly.length() < 9 || digitsOnly.length() > 10) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Candidate contact number must be 9-10 digits");
		}
		return "+60" + digitsOnly;
	}

	private void deleteResumeFile(String filename) {
		if (!StringUtils.hasText(filename)) {
			return;
		}
		try {
			Files.deleteIfExists(resumeDirectory.resolve(filename));
		} catch (IOException ignored) {
		}
	}

	private ResumeResponse toResponse(Resume resume) {
		return new ResumeResponse(
				resume.getId(),
				resume.getResumeId(),
				resume.getJobPostingId(),
				resume.getCandidateName(),
				resume.getCandidateEmail(),
				resume.getCandidateContactNumber(),
				resume.getResumeFilename(),
				resume.getResumeOriginalName(),
				resume.getUploadDate(),
				resume.getCreatedAt(),
				resume.getUpdatedAt());
	}
}

