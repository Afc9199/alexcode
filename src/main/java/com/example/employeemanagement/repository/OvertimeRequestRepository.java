package com.example.employeemanagement.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.example.employeemanagement.model.OvertimeRequest;
import com.example.employeemanagement.model.OvertimeStatus;

public interface OvertimeRequestRepository extends MongoRepository<OvertimeRequest, String> {
	List<OvertimeRequest> findByUserIdOrderByCreatedAtDesc(String userId);
	
	List<OvertimeRequest> findByStatusOrderByCreatedAtAsc(OvertimeStatus status);
	
	Optional<OvertimeRequest> findTopByOrderByOvertimeIdDesc();
	
	Optional<OvertimeRequest> findByOvertimeId(String overtimeId);
	
	List<OvertimeRequest> findByEmployeeIdOrderByCreatedAtDesc(String employeeId);
	
	List<OvertimeRequest> findByUserIdAndWorkDateBetweenOrderByCreatedAtDesc(String userId, LocalDate start, LocalDate end);
	
	@Query("{ 'userId': ?0, 'status': ?1, 'workDate': { $gte: ?2, $lte: ?3 } }")
	List<OvertimeRequest> findByUserIdAndStatusAndWorkDateBetween(String userId, OvertimeStatus status, LocalDate start, LocalDate end);
}

