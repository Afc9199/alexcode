package com.example.employeemanagement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.employeemanagement.model.JobPosting;

public interface JobPostingRepository extends MongoRepository<JobPosting, String> {

	Optional<JobPosting> findByJobPostingId(String jobPostingId);

	Optional<JobPosting> findTopByOrderByJobPostingIdDesc();

	List<JobPosting> findByJobTitleContainingIgnoreCase(String search);

	List<JobPosting> findByJobPostingIdContainingIgnoreCaseOrJobTitleContainingIgnoreCase(String jobPostingId, String jobTitle);

	List<JobPosting> findByStatus(String status);

}

