package com.example.employeemanagement.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.employeemanagement.model.CompanyLeaveSettings;

public interface CompanyLeaveSettingsRepository extends MongoRepository<CompanyLeaveSettings, String> {
}

