package com.example.employeemanagement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.employeemanagement.model.UserAccount;

public interface UserAccountRepository extends MongoRepository<UserAccount, String> {
	Optional<UserAccount> findByUsernameIgnoreCase(String username);
	Optional<UserAccount> findFirstByOrderByEmployeeIdDesc();
	Optional<UserAccount> findByEmployeeId(String employeeId);
	Optional<UserAccount> findByEmailIgnoreCase(String email);
	Optional<UserAccount> findByContactNumber(String contactNumber);
	Optional<UserAccount> findByBankAccountNumber(String bankAccountNumber);
	Optional<UserAccount> findByEpfNumber(String epfNumber);
	Optional<UserAccount> findByIcNumber(String icNumber);
	Optional<UserAccount> findByTaxNumber(String taxNumber);
	List<UserAccount> findByFullNameIgnoreCase(String fullName);

	List<UserAccount> findByDepartment(String department);
}

