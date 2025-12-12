package com.example.employeemanagement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.employeemanagement.model.LeaveBalance;

public interface LeaveBalanceRepository extends MongoRepository<LeaveBalance, String> {
	
	Optional<LeaveBalance> findByEmployeeIdAndLeaveType(String employeeId, String leaveType);
	
	Optional<LeaveBalance> findByUserIdAndLeaveType(String userId, String leaveType);
	
	List<LeaveBalance> findByEmployeeId(String employeeId);
	
	List<LeaveBalance> findByUserId(String userId);
	
	void deleteByUserId(String userId);
	
	void deleteByEmployeeId(String employeeId);
	
	void deleteByLeaveType(String leaveType);
}

