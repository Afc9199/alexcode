package com.example.employeemanagement.service;

import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.example.employeemanagement.dto.ChangePasswordRequest;
import com.example.employeemanagement.dto.ProfileResponse;
import com.example.employeemanagement.dto.ProfileUpdateRequest;
import com.example.employeemanagement.model.UserAccount;
import com.example.employeemanagement.repository.UserAccountRepository;
import com.example.employeemanagement.util.PasswordValidator;

@Service
public class UserProfileService {

	private final UserAccountRepository userAccountRepository;
	private final PasswordEncoder passwordEncoder;

	public UserProfileService(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder) {
		this.userAccountRepository = userAccountRepository;
		this.passwordEncoder = passwordEncoder;
	}

	public ProfileResponse getProfile(String userId) {
		UserAccount account = userAccountRepository.findById(requireUserId(userId))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
		return toResponse(account);
	}

	public ProfileResponse updateProfile(String userId, ProfileUpdateRequest request) {
		UserAccount account = userAccountRepository.findById(requireUserId(userId))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
		
		// Validate required fields and formats
		validateRequiredFields(request);
		validateFieldFormats(request);
		
		// Validate unique fields before updating
		validateUniqueFields(request, account.getId());
		
		account.setFullName(request.fullName());
		// Store null instead of empty string for unique fields to avoid index conflicts
		account.setEmail(StringUtils.hasText(request.email()) ? request.email().trim() : null);
		account.setDepartment(request.department());
		account.setJobTitle(request.jobTitle());
		
		// Clean and set contact number (ensure +60 prefix)
		if (request.contactNumber() != null) {
			String contactNumber = request.contactNumber().trim().replaceAll("[\\s\\-]", "");
			if (StringUtils.hasText(contactNumber)) {
				if (!contactNumber.startsWith("+60")) {
					contactNumber = "+60" + contactNumber;
				}
				account.setContactNumber(contactNumber);
			} else {
				account.setContactNumber(null);
			}
		}
		
		if (request.gender() != null) {
			account.setGender(request.gender());
		}
		if (request.age() != null) {
			account.setAge(request.age());
		}
		if (request.race() != null) {
			account.setRace(request.race());
		}
		if (request.religion() != null) {
			account.setReligion(request.religion());
		}
		if (request.address() != null) {
			account.setAddress(request.address());
		}
		if (request.maritalStatus() != null) {
			account.setMaritalStatus(request.maritalStatus());
		}
		if (request.bankName() != null) {
			account.setBankName(request.bankName());
		}
		
		// Clean bank account number (remove spaces and dashes)
		if (request.bankAccountNumber() != null) {
			String bankAccountNumber = request.bankAccountNumber().trim().replaceAll("[\\s\\-]", "");
			account.setBankAccountNumber(StringUtils.hasText(bankAccountNumber) ? bankAccountNumber : null);
		}
		
		// Clean and set EPF number
		if (request.epfNumber() != null) {
			account.setEpfNumber(StringUtils.hasText(request.epfNumber()) ? request.epfNumber().trim() : null);
		}
		
		// Clean IC number (remove spaces and dashes)
		if (request.icNumber() != null) {
			String icNumber = request.icNumber().trim().replaceAll("[\\s\\-]", "");
			account.setIcNumber(StringUtils.hasText(icNumber) ? icNumber : null);
		}
		
		// Set passport number
		if (request.passportNumber() != null) {
			account.setPassportNumber(StringUtils.hasText(request.passportNumber()) ? request.passportNumber().trim() : null);
		}
		
		// Clean and set tax number
		if (request.taxNumber() != null) {
			account.setTaxNumber(StringUtils.hasText(request.taxNumber()) ? request.taxNumber().trim() : null);
		}
		
		if (request.numberOfChildren() != null) {
			account.setNumberOfChildren(request.numberOfChildren());
		}
		if (request.nationality() != null) {
			account.setNationality(request.nationality());
		}
		if (request.residentStatus() != null) {
			account.setResidentStatus(request.residentStatus());
		}
		if (request.spouseWorking() != null) {
			account.setSpouseWorking(request.spouseWorking());
		}
		
		// Set date of birth
		if (request.dateOfBirth() != null) {
			account.setDateOfBirth(request.dateOfBirth());
		}
		
		// Clean SOCSO number (remove spaces and dashes)
		if (request.socsoNumber() != null) {
			String socsoNumber = request.socsoNumber().trim().replaceAll("[\\s\\-]", "");
			account.setSocsoNumber(StringUtils.hasText(socsoNumber) ? socsoNumber : null);
		}
		
		// Set date of hire
		if (request.dateOfHire() != null) {
			account.setDateOfHire(request.dateOfHire());
		}
		
		// Set emergency contact information
		if (request.emergencyContactName() != null) {
			account.setEmergencyContactName(StringUtils.hasText(request.emergencyContactName()) ? request.emergencyContactName().trim() : "");
		}
		if (request.emergencyContactRelationship() != null) {
			account.setEmergencyContactRelationship(StringUtils.hasText(request.emergencyContactRelationship()) ? request.emergencyContactRelationship().trim() : "");
		}
		if (request.emergencyContactNumber() != null) {
			String emergencyContactNumber = request.emergencyContactNumber().trim().replaceAll("[\\s\\-]", "");
			if (StringUtils.hasText(emergencyContactNumber)) {
				if (!emergencyContactNumber.startsWith("+60")) {
					emergencyContactNumber = "+60" + emergencyContactNumber;
				}
				account.setEmergencyContactNumber(emergencyContactNumber);
			} else {
				account.setEmergencyContactNumber(null);
			}
		}
		
		userAccountRepository.save(account);
		return toResponse(account);
	}
	
	private void validateRequiredFields(ProfileUpdateRequest request) {
		// All fields are optional for employee profile edit
		// Only validate age if provided
		if (request.age() != null && request.age() < 18) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Age must be 18 or above");
		}
		
		// Passport number is required when Non-Malaysian is selected
		if ("Non-Malaysian".equals(request.nationality()) && 
			(request.passportNumber() == null || !StringUtils.hasText(request.passportNumber()))) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passport number is required for Non-Malaysian");
		}
	}
	
	private void validateFieldFormats(ProfileUpdateRequest request) {
		// Validate contact number format (9-10 digits after +60)
		if (request.contactNumber() != null && StringUtils.hasText(request.contactNumber())) {
			String contactNumber = request.contactNumber().trim().replaceAll("[\\s\\-]", "");
			if (!contactNumber.startsWith("+60")) {
				contactNumber = "+60" + contactNumber;
			}
			if (!contactNumber.equals("+60")) {
				String digits = contactNumber.substring(3);
				if (!digits.matches("\\d{9,10}")) {
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contact number must be 9-10 digits");
				}
			}
		}
		
		// Validate IC number format (exactly 12 digits, no spaces)
		if (request.icNumber() != null && StringUtils.hasText(request.icNumber())) {
			if (request.icNumber().contains(" ")) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IC number cannot contain spaces");
			}
			String cleanedIc = request.icNumber().trim().replaceAll("[\\s\\-]", "");
			if (!cleanedIc.matches("\\d{12}")) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IC number must be exactly 12 digits");
			}
		}
		
		// Validate SOCSO number format (exactly 12 digits, no spaces)
		if (request.socsoNumber() != null && StringUtils.hasText(request.socsoNumber())) {
			if (request.socsoNumber().contains(" ")) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SOCSO number cannot contain spaces");
			}
			String cleanedSocso = request.socsoNumber().trim().replaceAll("[\\s\\-]", "");
			if (!cleanedSocso.matches("\\d{12}")) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SOCSO number must be exactly 12 digits");
			}
		}
		
		// Validate tax number format (must start with "IG" prefix, then 9-11 digits)
		if (request.taxNumber() != null && StringUtils.hasText(request.taxNumber())) {
			String taxNumber = request.taxNumber().trim();
			if (!taxNumber.matches("(?i)^IG\\d{9,11}$")) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tax number must start with \"IG\" prefix, followed by 9 to 11 digits");
			}
		}
		
		// Validate emergency contact number format (9-10 digits after +60)
		if (request.emergencyContactNumber() != null && StringUtils.hasText(request.emergencyContactNumber())) {
			String emergencyContactNumber = request.emergencyContactNumber().trim().replaceAll("[\\s\\-]", "");
			if (!emergencyContactNumber.startsWith("+60")) {
				emergencyContactNumber = "+60" + emergencyContactNumber;
			}
			if (!emergencyContactNumber.equals("+60")) {
				String digits = emergencyContactNumber.substring(3);
				if (!digits.matches("\\d{9,10}")) {
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Emergency contact number must be 9-10 digits");
				}
			}
		}
		
		// Validate email format (no spaces, must contain @ and . after @)
		if (request.email() != null && StringUtils.hasText(request.email())) {
			String email = request.email().trim();
			if (email.contains(" ")) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email cannot contain spaces");
			}
			if (!email.contains("@")) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email must contain @ symbol");
			}
			String[] parts = email.split("@");
			if (parts.length != 2 || !parts[1].contains(".")) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email must contain a dot (.) after @");
			}
		}
		
		// Validate full name length (max 50 characters)
		if (request.fullName() != null && request.fullName().length() > 50) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Full name must not exceed 50 characters");
		}
		
		// Validate department length (max 50 characters)
		if (request.department() != null && request.department().length() > 50) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department must not exceed 50 characters");
		}
		
		// Validate job title length (max 50 characters)
		if (request.jobTitle() != null && request.jobTitle().length() > 50) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Job title must not exceed 50 characters");
		}
		
		// Validate emergency contact name length (max 100 characters)
		if (request.emergencyContactName() != null && request.emergencyContactName().length() > 100) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Emergency contact name must not exceed 100 characters");
		}
	}
	
	private void validateUniqueFields(ProfileUpdateRequest request, String currentId) {
		// Validate email uniqueness (only if not empty)
		if (StringUtils.hasText(request.email())) {
			String email = request.email().trim();
			userAccountRepository.findByEmailIgnoreCase(email).ifPresent(existing -> {
				if (currentId == null || !existing.getId().equals(currentId)) {
					throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
				}
			});
		}

		// Validate contact number uniqueness (with +60 prefix, only if not empty)
		if (request.contactNumber() != null && StringUtils.hasText(request.contactNumber())) {
			String contactNumber = request.contactNumber().trim().replaceAll("[\\s\\-]", "");
			if (!contactNumber.isEmpty()) {
				// Ensure it has +60 prefix for comparison
				if (!contactNumber.startsWith("+60")) {
					contactNumber = "+60" + contactNumber;
				}
				if (!contactNumber.equals("+60")) {
					// Check all users and compare cleaned values to catch duplicates even if stored with spaces/dashes
					List<UserAccount> allUsers = userAccountRepository.findAll();
					for (UserAccount existing : allUsers) {
						if (currentId != null && existing.getId().equals(currentId)) {
							continue; // Skip the current user being updated
						}
						if (existing.getContactNumber() != null) {
							String cleanedExisting = existing.getContactNumber().trim().replaceAll("[\\s\\-]", "");
							// Ensure existing also has +60 prefix for comparison
							if (!cleanedExisting.startsWith("+60") && !cleanedExisting.isEmpty()) {
								cleanedExisting = "+60" + cleanedExisting;
							}
							if (contactNumber.equals(cleanedExisting)) {
								throw new ResponseStatusException(HttpStatus.CONFLICT, "Contact number already exists");
							}
						}
					}
				}
			}
		}

		// Validate bank account number uniqueness (remove spaces and dashes, only if not empty)
		if (request.bankAccountNumber() != null && StringUtils.hasText(request.bankAccountNumber())) {
			String cleanedInput = request.bankAccountNumber().trim().replaceAll("[\\s\\-]", "");
			if (!cleanedInput.isEmpty()) {
				// Check all users and compare cleaned values to catch duplicates even if stored with spaces/dashes
				List<UserAccount> allUsers = userAccountRepository.findAll();
				for (UserAccount existing : allUsers) {
					if (currentId != null && existing.getId().equals(currentId)) {
						continue; // Skip the current user being updated
					}
					if (existing.getBankAccountNumber() != null) {
						String cleanedExisting = existing.getBankAccountNumber().trim().replaceAll("[\\s\\-]", "");
						if (cleanedInput.equals(cleanedExisting)) {
							throw new ResponseStatusException(HttpStatus.CONFLICT, "Bank account number already exists");
						}
					}
				}
			}
		}

		// Validate EPF number uniqueness (only if not empty)
		if (request.epfNumber() != null && StringUtils.hasText(request.epfNumber())) {
			String epfNumber = request.epfNumber().trim();
			if (!epfNumber.isEmpty()) {
				userAccountRepository.findByEpfNumber(epfNumber).ifPresent(existing -> {
					if (currentId == null || !existing.getId().equals(currentId)) {
						throw new ResponseStatusException(HttpStatus.CONFLICT, "EPF number already exists");
					}
				});
			}
		}

		// Validate IC number uniqueness (remove spaces and dashes, only if not empty)
		if (request.icNumber() != null && StringUtils.hasText(request.icNumber())) {
			String cleanedInput = request.icNumber().trim().replaceAll("[\\s\\-]", "");
			if (!cleanedInput.isEmpty()) {
				// Check all users and compare cleaned values to catch duplicates even if stored with spaces/dashes
				List<UserAccount> allUsers = userAccountRepository.findAll();
				for (UserAccount existing : allUsers) {
					if (currentId != null && existing.getId().equals(currentId)) {
						continue; // Skip the current user being updated
					}
					if (existing.getIcNumber() != null) {
						String cleanedExisting = existing.getIcNumber().trim().replaceAll("[\\s\\-]", "");
						if (cleanedInput.equals(cleanedExisting)) {
							throw new ResponseStatusException(HttpStatus.CONFLICT, "IC number already exists");
						}
					}
				}
			}
		}

		// Validate tax number uniqueness (only if not empty)
		if (request.taxNumber() != null && StringUtils.hasText(request.taxNumber())) {
			String taxNumber = request.taxNumber().trim();
			if (!taxNumber.isEmpty()) {
				userAccountRepository.findByTaxNumber(taxNumber).ifPresent(existing -> {
					if (currentId == null || !existing.getId().equals(currentId)) {
						throw new ResponseStatusException(HttpStatus.CONFLICT, "Tax number already exists");
					}
				});
			}
		}

		// Validate SOCSO number uniqueness (remove spaces and dashes, only if not empty)
		if (request.socsoNumber() != null && StringUtils.hasText(request.socsoNumber())) {
			String cleanedInput = request.socsoNumber().trim().replaceAll("[\\s\\-]", "");
			if (!cleanedInput.isEmpty()) {
				// Check all users and compare cleaned values to catch duplicates even if stored with spaces/dashes
				List<UserAccount> allUsers = userAccountRepository.findAll();
				for (UserAccount existing : allUsers) {
					if (currentId != null && existing.getId().equals(currentId)) {
						continue; // Skip the current user being updated
					}
					if (existing.getSocsoNumber() != null) {
						String cleanedExisting = existing.getSocsoNumber().trim().replaceAll("[\\s\\-]", "");
						if (cleanedInput.equals(cleanedExisting)) {
							throw new ResponseStatusException(HttpStatus.CONFLICT, "SOCSO number already exists");
						}
					}
				}
			}
		}
	}

	private ProfileResponse toResponse(UserAccount account) {
		return new ProfileResponse(account.getId(), account.getUsername(), account.getEmployeeId(), account.getFullName(), account.getEmail(),
				account.getDepartment(), account.getJobTitle(), account.getBasicSalary(), account.getContactNumber(), account.getGender(), account.getAge(),
				account.getRace(), account.getReligion(), account.getAddress(), account.getMaritalStatus(),
				account.getBankName(), account.getBankAccountNumber(), account.getEpfNumber(), account.getIcNumber(), account.getPassportNumber(),
				account.getTaxNumber(), account.getNumberOfChildren(), account.getRole().name(),
				account.getNationality(), account.getResidentStatus(), account.getSpouseWorking(),
				account.getDateOfBirth(), account.getSocsoNumber(), account.getDateOfHire(),
				account.getEmergencyContactName(), account.getEmergencyContactRelationship(), account.getEmergencyContactNumber());
	}

	private @NonNull String requireUserId(String userId) {
		return Objects.requireNonNull(userId, "userId is required");
	}

	public void changePassword(String userId, ChangePasswordRequest request) {
		UserAccount account = userAccountRepository.findById(requireUserId(userId))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
		
		// Validate that new password and confirm password match
		if (!request.newPassword().equals(request.confirmPassword())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password and confirm password do not match");
		}
		
		// Validate new password strength
		PasswordValidator.ValidationResult result = PasswordValidator.validate(request.newPassword().trim());
		if (!result.isValid()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, result.getErrorMessage());
		}
		
		// Verify old password
		String oldPasswordInput = request.oldPassword().trim();
		// Clean old password input (remove spaces and dashes) to match how IC number is stored
		String cleanedOldPassword = oldPasswordInput.replaceAll("[\\s\\-]", "");
		boolean oldPasswordValid = false;
		
		// First, check if old password matches IC number (default password - 12 digits only)
		String icNumber = account.getIcNumber();
		if (icNumber != null && !icNumber.isEmpty()) {
			// Compare cleaned input with stored IC number (which is already cleaned to 12 digits)
			if (cleanedOldPassword.equals(icNumber)) {
				oldPasswordValid = true;
			}
		}
		
		// If not IC number, check if it matches the current hashed password (for subsequent changes)
		if (!oldPasswordValid) {
			// Try with cleaned password first
			if (passwordEncoder.matches(cleanedOldPassword, account.getPasswordHash())) {
				oldPasswordValid = true;
			} else if (passwordEncoder.matches(oldPasswordInput, account.getPasswordHash())) {
				// Also try with original input (in case password was set with spaces/dashes)
				oldPasswordValid = true;
			}
		}
		
		if (!oldPasswordValid) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Old password is incorrect");
		}
		
		// Update password
		account.setPasswordHash(passwordEncoder.encode(request.newPassword().trim()));
		userAccountRepository.save(account);
	}
}

