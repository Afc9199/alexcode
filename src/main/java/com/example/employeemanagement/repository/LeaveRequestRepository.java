package com.example.employeemanagement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.employeemanagement.model.LeaveRequest;
import com.example.employeemanagement.model.LeaveStatus;

public interface LeaveRequestRepository extends MongoRepository<LeaveRequest, String> {
	List<LeaveRequest> findByUserIdOrderByCreatedAtDesc(String userId);

	List<LeaveRequest> findByStatusOrderByCreatedAtAsc(LeaveStatus status);

	Optional<LeaveRequest> findTopByOrderByLeaveIdDesc();

	Optional<LeaveRequest> findByLeaveId(String leaveId);

	List<LeaveRequest> findByEmployeeIdOrderByCreatedAtDesc(String employeeId);

	void deleteByUserId(String userId);

	void deleteByEmployeeId(String employeeId);
}

