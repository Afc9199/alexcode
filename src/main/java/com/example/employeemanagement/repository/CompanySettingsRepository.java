package com.example.employeemanagement.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.employeemanagement.model.CompanySettings;

public interface CompanySettingsRepository extends MongoRepository<CompanySettings, String> {
}

