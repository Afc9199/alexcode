package com.example.employeemanagement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.employeemanagement.model.Department;

public interface DepartmentRepository extends MongoRepository<Department, String> {
	Optional<Department> findByNameIgnoreCase(String name);
	
	List<Department> findByNameContainingIgnoreCase(String name);
	
	List<Department> findAllByOrderByNameAsc();
}

