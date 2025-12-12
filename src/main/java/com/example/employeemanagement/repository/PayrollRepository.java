package com.example.employeemanagement.repository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.employeemanagement.model.PayrollRecord;

public interface PayrollRepository extends MongoRepository<PayrollRecord, String> {
	List<PayrollRecord> findByUserIdOrderByPeriodEndDesc(String userId);

	Optional<PayrollRecord> findByUserIdAndPeriodStartAndPeriodEnd(String userId, LocalDate periodStart, LocalDate periodEnd);

	Optional<PayrollRecord> findByUserIdAndSalaryMonth(String userId, YearMonth salaryMonth);

	void deleteByUserId(String userId);
}

