package com.example.employeemanagement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.employeemanagement.model.EmployeeBenefit;

public interface EmployeeBenefitRepository extends MongoRepository<EmployeeBenefit, String> {

	List<EmployeeBenefit> findByUserId(String userId);

	List<EmployeeBenefit> findByEmployeeId(String employeeId);

	List<EmployeeBenefit> findByBenefitCategoryId(String benefitCategoryId);

	Optional<EmployeeBenefit> findByUserIdAndBenefitCategoryId(String userId, String benefitCategoryId);

	Optional<EmployeeBenefit> findByEmployeeIdAndBenefitCategoryId(String employeeId, String benefitCategoryId);

	void deleteByUserId(String userId);

	void deleteByEmployeeId(String employeeId);

	void deleteByBenefitCategoryId(String benefitCategoryId);
}

