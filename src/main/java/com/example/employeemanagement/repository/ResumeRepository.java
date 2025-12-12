package com.example.employeemanagement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.employeemanagement.model.Resume;

public interface ResumeRepository extends MongoRepository<Resume, String> {

	Optional<Resume> findByResumeId(String resumeId);

	Optional<Resume> findByResumeFilename(String resumeFilename);

	Optional<Resume> findTopByOrderByResumeIdDesc();

	List<Resume> findByJobPostingId(String jobPostingId);

	void deleteByResumeId(String resumeId);

	void deleteByJobPostingId(String jobPostingId);

}

