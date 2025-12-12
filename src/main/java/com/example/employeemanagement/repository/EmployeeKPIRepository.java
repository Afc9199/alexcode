package com.example.employeemanagement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.employeemanagement.model.EmployeeKPI;

public interface EmployeeKPIRepository extends MongoRepository<EmployeeKPI, String> {

	List<EmployeeKPI> findByUserId(String userId);

	List<EmployeeKPI> findByEmployeeId(String employeeId);

	List<EmployeeKPI> findByKpiCategoryId(String kpiCategoryId);

	List<EmployeeKPI> findByKpiId(String kpiId);

	Optional<EmployeeKPI> findByUserIdAndKpiCategoryId(String userId, String kpiCategoryId);

	Optional<EmployeeKPI> findByEmployeeIdAndKpiCategoryId(String employeeId, String kpiCategoryId);

	void deleteByUserId(String userId);

	void deleteByEmployeeId(String employeeId);

	void deleteByKpiCategoryId(String kpiCategoryId);

	void deleteByKpiId(String kpiId);
}

