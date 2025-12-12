package com.example.employeemanagement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.employeemanagement.model.KPI;

public interface KPIRepository extends MongoRepository<KPI, String> {
	Optional<KPI> findByNameIgnoreCase(String name);

	List<KPI> findByNameContainingIgnoreCase(String name);

	List<KPI> findAllByOrderByNameAsc();

	Optional<KPI> findTopByOrderByKpiIdDesc();

	Optional<KPI> findByKpiId(String kpiId);

	List<KPI> findAllByOrderByKpiIdAsc();
}

