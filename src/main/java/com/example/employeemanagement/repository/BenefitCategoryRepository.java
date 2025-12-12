package com.example.employeemanagement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.employeemanagement.model.BenefitCategory;

public interface BenefitCategoryRepository extends MongoRepository<BenefitCategory, String> {
	
	Optional<BenefitCategory> findByNameIgnoreCase(String name);
	
	List<BenefitCategory> findByNameContainingIgnoreCase(String name);
	
	List<BenefitCategory> findAllByOrderByNameAsc();
	
	List<BenefitCategory> findAllByOrderByBenefitIdAsc();
	
	Optional<BenefitCategory> findTopByOrderByBenefitIdDesc();
	
	Optional<BenefitCategory> findByBenefitId(String benefitId);
}

