package com.example.employeemanagement.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.employeemanagement.model.AttendanceRecord;

public interface AttendanceRepository extends MongoRepository<AttendanceRecord, String> {
	List<AttendanceRecord> findByUserIdOrderByWorkDateDesc(String userId);

	List<AttendanceRecord> findByEmployeeIdOrderByWorkDateDesc(String employeeId);

	boolean existsByUserIdAndWorkDate(String userId, LocalDate workDate);

	Optional<AttendanceRecord> findByUserIdAndWorkDate(String userId, LocalDate workDate);

	Optional<AttendanceRecord> findTopByOrderByAttendanceIdDesc();

	Optional<AttendanceRecord> findByAttendanceId(String attendanceId);

	void deleteByUserId(String userId);

	void deleteByEmployeeId(String employeeId);
}

