package com.example.employeemanagement.service;

import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.example.employeemanagement.dto.UserAccountResponse;
import com.example.employeemanagement.dto.UserAccountUpsertRequest;
import com.example.employeemanagement.model.Role;
import com.example.employeemanagement.model.UserAccount;
import com.example.employeemanagement.repository.AttendanceRepository;
import com.example.employeemanagement.repository.LeaveRequestRepository;
import com.example.employeemanagement.repository.PayrollRepository;
import com.example.employeemanagement.repository.UserAccountRepository;
import com.example.employeemanagement.util.PasswordValidator;

@Service
public class AdminUserService {

	private final UserAccountRepository userAccountRepository;
	private final PasswordEncoder passwordEncoder;
	private final AttendanceRepository attendanceRepository;
	private final LeaveRequestRepository leaveRequestRepository;
	private final PayrollRepository payrollRepository;
	private final LeaveBalanceService leaveBalanceService;
	private final com.example.employeemanagement.repository.LeaveBalanceRepository leaveBalanceRepository;
	private final com.example.employeemanagement.repository.EmployeeBenefitRepository employeeBenefitRepository;
	private final com.example.employeemanagement.repository.EmployeeKPIRepository employeeKPIRepository;

	public AdminUserService(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder,
			AttendanceRepository attendanceRepository, LeaveRequestRepository leaveRequestRepository,
			PayrollRepository payrollRepository, LeaveBalanceService leaveBalanceService,
			com.example.employeemanagement.repository.LeaveBalanceRepository leaveBalanceRepository,
			com.example.employeemanagement.repository.EmployeeBenefitRepository employeeBenefitRepository,
			com.example.employeemanagement.repository.EmployeeKPIRepository employeeKPIRepository) {
		this.userAccountRepository = userAccountRepository;
		this.passwordEncoder = passwordEncoder;
		this.attendanceRepository = attendanceRepository;
		this.leaveRequestRepository = leaveRequestRepository;
		this.payrollRepository = payrollRepository;
		this.leaveBalanceService = leaveBalanceService;
		this.leaveBalanceRepository = leaveBalanceRepository;
		this.employeeBenefitRepository = employeeBenefitRepository;
		this.employeeKPIRepository = employeeKPIRepository;
	}

	public List<UserAccountResponse> listUsers() {
		return userAccountRepository.findAll().stream().map(this::toResponse).toList();
	}

	public UserAccountResponse getUser(String userId) {
		UserAccount account = requireUser(userId);
		return toResponse(account);
	}

	public UserAccountResponse createUser(UserAccountUpsertRequest request) {
		UserAccountUpsertRequest safeRequest = Objects.requireNonNull(request, "User account request is required");
		ensureUsernameAvailable(safeRequest.username(), null);
		
		// Validate unique fields before creating
		// Ensures IC number, contact number, bank account number, EPF number, SOCSO number, and tax number are unique
		validateUniqueFields(safeRequest, null);

		// Get IC number for default password
		String icNumber = safeRequest.icNumber();
		if (!StringUtils.hasText(icNumber)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IC number is required to generate default password");
		}
		// Clean IC number (remove spaces and dashes) to use as default password
		String cleanedIc = icNumber.trim().replaceAll("[\\s\\-]", "");
		if (cleanedIc.length() != 12) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IC number must be exactly 12 digits");
		}
		
		// Use IC number as default password (12 digits only - no password strength validation needed)
		String defaultPassword = cleanedIc;
		// Note: IC number (12 digits) is intentionally used as default password
		// Password strength validation is not applied here as IC number is digits only

		UserAccount account = UserAccount.of(safeRequest.username().trim(),
				passwordEncoder.encode(defaultPassword),
				parseRole(safeRequest.role()));
		account.setEmployeeId(generateNextEmployeeId());
		applyUpserts(account, safeRequest, true);
		UserAccount savedAccount = Objects.requireNonNull(userAccountRepository.save(account), "Failed to save user account");
		
		// Initialize leave balances for the new employee
		if (savedAccount.getEmployeeId() != null) {
			leaveBalanceService.initializeLeaveBalancesForEmployee(savedAccount.getId(), savedAccount.getEmployeeId());
		}
		
		return toResponse(savedAccount);
	}

	public UserAccountResponse updateUser(String userId, UserAccountUpsertRequest request) {
		UserAccountUpsertRequest safeRequest = Objects.requireNonNull(request, "User account request is required");
		UserAccount account = requireUser(userId);

		if (!account.getUsername().equalsIgnoreCase(safeRequest.username().trim())) {
			ensureUsernameAvailable(safeRequest.username(), account.getId());
			account.setUsername(safeRequest.username().trim());
		}

		if (StringUtils.hasText(safeRequest.password())) {
			validatePasswordStrength(safeRequest.password());
			account.setPasswordHash(passwordEncoder.encode(safeRequest.password().trim()));
		}

		// Validate unique fields before updating
		validateUniqueFields(safeRequest, account.getId());

		account.setRole(parseRole(safeRequest.role()));
		applyUpserts(account, safeRequest, false);
		return toResponse(Objects.requireNonNull(userAccountRepository.save(account), "Failed to save user account"));
	}

	public void deleteUser(String userId) {
		String nonNullUserId = Objects.requireNonNull(userId, "userId is required");
		UserAccount user = userAccountRepository.findById(nonNullUserId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
		
		// Cascade delete: Delete all related records before deleting the user
		// 1. Delete attendance records (by userId and employeeId)
		attendanceRepository.deleteByUserId(nonNullUserId);
		if (user.getEmployeeId() != null && !user.getEmployeeId().isEmpty()) {
			attendanceRepository.deleteByEmployeeId(user.getEmployeeId());
		}
		
		// 2. Delete leave requests (by userId and employeeId)
		leaveRequestRepository.deleteByUserId(nonNullUserId);
		if (user.getEmployeeId() != null && !user.getEmployeeId().isEmpty()) {
			leaveRequestRepository.deleteByEmployeeId(user.getEmployeeId());
		}
		
		// 3. Delete leave balances (by userId and employeeId)
		leaveBalanceRepository.deleteByUserId(nonNullUserId);
		if (user.getEmployeeId() != null && !user.getEmployeeId().isEmpty()) {
			leaveBalanceRepository.deleteByEmployeeId(user.getEmployeeId());
		}
		
		// 4. Delete payroll records (by userId)
		payrollRepository.deleteByUserId(nonNullUserId);
		
		// 5. Delete employee benefits (by userId and employeeId)
		employeeBenefitRepository.deleteByUserId(nonNullUserId);
		if (user.getEmployeeId() != null && !user.getEmployeeId().isEmpty()) {
			employeeBenefitRepository.deleteByEmployeeId(user.getEmployeeId());
		}
		
		// 6. Delete employee KPIs (by userId and employeeId)
		employeeKPIRepository.deleteByUserId(nonNullUserId);
		if (user.getEmployeeId() != null && !user.getEmployeeId().isEmpty()) {
			employeeKPIRepository.deleteByEmployeeId(user.getEmployeeId());
		}
		
		// 7. Finally, delete the user account
		userAccountRepository.deleteById(nonNullUserId);
	}

	public String getNextEmployeeId() {
		return generateNextEmployeeId();
	}

	/**
	 * Check if user's password is still the default (IC number)
	 */
	public boolean isPasswordDefault(String userId) {
		UserAccount account = requireUser(userId);
		return isDefaultPassword(account);
	}

	public UserAccountResponse getUserByEmployeeId(String employeeId) {
		UserAccount account = userAccountRepository.findByEmployeeId(employeeId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
		return toResponse(account);
	}

	private UserAccount requireUser(String userId) {
		return userAccountRepository.findById(Objects.requireNonNull(userId))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
	}

	private void ensureUsernameAvailable(String username, String currentId) {
		if (!StringUtils.hasText(username)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is required");
		}
		userAccountRepository.findByUsernameIgnoreCase(username.trim()).ifPresent(existing -> {
			if (currentId == null || !existing.getId().equals(currentId)) {
				throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
			}
		});
	}

	/**
	 * Validates that the following fields are unique in the database:
	 * - IC number
	 * - Contact number
	 * - Bank account number
	 * - EPF number
	 * - SOCSO number
	 * - Tax number
	 * 
	 * @param request The user account request containing the fields to validate
	 * @param currentId The ID of the current user (null when creating a new user)
	 * @throws ResponseStatusException with CONFLICT status if any field already exists
	 */
	private void validateUniqueFields(UserAccountUpsertRequest request, String currentId) {
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
		if (StringUtils.hasText(request.contactNumber())) {
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
		if (StringUtils.hasText(request.bankAccountNumber())) {
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
		if (StringUtils.hasText(request.epfNumber())) {
			String epfNumber = request.epfNumber().trim();
			if (!epfNumber.isEmpty()) {
				userAccountRepository.findByEpfNumber(epfNumber).ifPresent(existing -> {
					if (currentId == null || !existing.getId().equals(currentId)) {
						throw new ResponseStatusException(HttpStatus.CONFLICT, "EPF number already exists");
					}
				});
			}
		}

		// Validate IC number format and uniqueness (remove spaces and dashes, only if not empty)
		if (StringUtils.hasText(request.icNumber())) {
			String cleanedInput = request.icNumber().trim().replaceAll("[\\s\\-]", "");
			if (!cleanedInput.isEmpty()) {
				// Validate format: must be exactly 12 digits, digits only
				if (!cleanedInput.matches("^\\d{12}$")) {
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IC number must be exactly 12 digits");
				}
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

		// Validate SOCSO number format and uniqueness (remove spaces and dashes, only if not empty)
		if (StringUtils.hasText(request.socsoNumber())) {
			String cleanedInput = request.socsoNumber().trim().replaceAll("[\\s\\-]", "");
			if (!cleanedInput.isEmpty()) {
				// Validate format: must be exactly 12 digits, digits only
				if (!cleanedInput.matches("^\\d{12}$")) {
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SOCSO number must be exactly 12 digits");
				}
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

		// Validate tax number format and uniqueness (must start with "IG" prefix, then 9-11 digits)
		if (StringUtils.hasText(request.taxNumber())) {
			String taxNumber = request.taxNumber().trim();
			if (!taxNumber.isEmpty()) {
				// Validate format: must start with "IG" (case insensitive), then 9-11 digits
				if (!taxNumber.matches("(?i)^IG\\d{9,11}$")) {
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tax number must start with 'IG' prefix, followed by 9 to 11 digits");
				}
				userAccountRepository.findByTaxNumber(taxNumber).ifPresent(existing -> {
					if (currentId == null || !existing.getId().equals(currentId)) {
						throw new ResponseStatusException(HttpStatus.CONFLICT, "Tax number already exists");
					}
				});
			}
		}
	}

	private void validatePasswordPresent(String password) {
		if (!StringUtils.hasText(password)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
		}
	}

	private void validatePasswordStrength(String password) {
		PasswordValidator.ValidationResult result = PasswordValidator.validate(password.trim());
		if (!result.isValid()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, result.getErrorMessage());
		}
	}

	private void applyUpserts(UserAccount account, UserAccountUpsertRequest request, boolean newAccount) {
		account.setFullName(nullToEmpty(request.fullName()));
		// Store null instead of empty string for unique fields to avoid index conflicts
		account.setEmail(StringUtils.hasText(request.email()) ? request.email().trim() : null);
		account.setDepartment(nullToEmpty(request.department()));
		account.setJobTitle(nullToEmpty(request.jobTitle()));
		
		// Clean and set contact number (ensure +60 prefix)
		String contactNumber = request.contactNumber();
		if (StringUtils.hasText(contactNumber)) {
			contactNumber = contactNumber.trim().replaceAll("[\\s\\-]", "");
			if (!contactNumber.isEmpty() && !contactNumber.startsWith("+60")) {
				contactNumber = "+60" + contactNumber;
			}
			account.setContactNumber(contactNumber);
		} else {
			account.setContactNumber(null);
		}
		
		account.setGender(nullToEmpty(request.gender()));
		account.setAge(request.age());
		account.setRace(nullToEmpty(request.race()));
		account.setReligion(nullToEmpty(request.religion()));
		account.setAddress(nullToEmpty(request.address()));
		account.setMaritalStatus(nullToEmpty(request.maritalStatus()));
		account.setBankName(nullToEmpty(request.bankName()));
		
		// Clean bank account number (remove spaces and dashes)
		String bankAccountNumber = request.bankAccountNumber();
		if (StringUtils.hasText(bankAccountNumber)) {
			bankAccountNumber = bankAccountNumber.trim().replaceAll("[\\s\\-]", "");
			account.setBankAccountNumber(bankAccountNumber.isEmpty() ? null : bankAccountNumber);
		} else {
			account.setBankAccountNumber(null);
		}
		
		account.setEpfNumber(StringUtils.hasText(request.epfNumber()) ? request.epfNumber().trim() : null);
		
		// Clean IC number (remove spaces and dashes)
		String icNumber = request.icNumber();
		if (StringUtils.hasText(icNumber)) {
			icNumber = icNumber.trim().replaceAll("[\\s\\-]", "");
			account.setIcNumber(icNumber.isEmpty() ? null : icNumber);
		} else {
			account.setIcNumber(null);
		}
		
		// Set passport number
		account.setPassportNumber(StringUtils.hasText(request.passportNumber()) ? request.passportNumber().trim() : null);
		
		account.setTaxNumber(StringUtils.hasText(request.taxNumber()) ? request.taxNumber().trim() : null);
		if (request.numberOfChildren() != null) {
			account.setNumberOfChildren(request.numberOfChildren());
		} else if (newAccount) {
			account.setNumberOfChildren(0);
		}
		account.setNationality(nullToEmpty(request.nationality()));
		account.setResidentStatus(nullToEmpty(request.residentStatus()));
		account.setSpouseWorking(nullToEmpty(request.spouseWorking()));
		if (request.basicSalary() != null) {
			account.setBasicSalary(request.basicSalary());
		} else if (newAccount) {
			account.setBasicSalary(0.0);
		}
		if (request.active() != null) {
			account.setActive(request.active());
		} else if (newAccount) {
			account.setActive(true);
		}
		
		// Compliance & Key Identity Information
		account.setDateOfBirth(request.dateOfBirth());
		// Clean SOCSO number (remove spaces and dashes, same as IC number)
		String socsoNumber = request.socsoNumber();
		if (StringUtils.hasText(socsoNumber)) {
			socsoNumber = socsoNumber.trim().replaceAll("[\\s\\-]", "");
			account.setSocsoNumber(socsoNumber.isEmpty() ? null : socsoNumber);
		} else {
			account.setSocsoNumber(null);
		}
		
		// Position & Employment Information
		account.setDateOfHire(request.dateOfHire());
		account.setProbationPeriodLength(request.probationPeriodLength() != null ? request.probationPeriodLength() : null);
		account.setEmploymentType(nullToEmpty(request.employmentType()));
		account.setReportingManagerId(StringUtils.hasText(request.reportingManagerId()) ? request.reportingManagerId().trim() : null);
		account.setLocation(nullToEmpty(request.location()));
		
		// Emergency Contact
		account.setEmergencyContactName(nullToEmpty(request.emergencyContactName()));
		account.setEmergencyContactRelationship(nullToEmpty(request.emergencyContactRelationship()));
		// Clean and set emergency contact number (ensure +60 prefix)
		String emergencyContactNumber = request.emergencyContactNumber();
		if (StringUtils.hasText(emergencyContactNumber)) {
			emergencyContactNumber = emergencyContactNumber.trim().replaceAll("[\\s\\-]", "");
			if (!emergencyContactNumber.isEmpty() && !emergencyContactNumber.startsWith("+60")) {
				emergencyContactNumber = "+60" + emergencyContactNumber;
			}
			account.setEmergencyContactNumber(emergencyContactNumber);
		} else {
			account.setEmergencyContactNumber(null);
		}
	}

	private Role parseRole(String role) {
		try {
			return Role.valueOf(role.trim().toUpperCase());
		} catch (Exception ex) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role: " + role);
		}
	}

	private String generateNextEmployeeId() {
		return userAccountRepository.findFirstByOrderByEmployeeIdDesc()
				.map(lastUser -> {
					String lastId = lastUser.getEmployeeId();
					if (lastId != null && lastId.startsWith("E")) {
						try {
							int number = Integer.parseInt(lastId.substring(1));
							return String.format("E%03d", number + 1);
						} catch (NumberFormatException e) {
							return "E001";
						}
					}
					return "E001";
				})
				.orElse("E001");
	}

	/**
	 * Check if password is still the default (IC number)
	 */
	private boolean isDefaultPassword(UserAccount account) {
		String icNumber = account.getIcNumber();
		if (icNumber == null || icNumber.isEmpty()) {
			return false;
		}
		// Clean IC number (remove spaces and dashes) to match how it's used as default password
		String cleanedIc = icNumber.trim().replaceAll("[\\s\\-]", "");
		if (cleanedIc.length() != 12) {
			return false;
		}
		// Check if password hash matches the IC number (default password)
		return passwordEncoder.matches(cleanedIc, account.getPasswordHash());
	}

	private UserAccountResponse toResponse(UserAccount account) {
		return new UserAccountResponse(account.getId(), account.getUsername(), account.getEmployeeId(), account.getRole().name(),
				account.isActive(), account.getFullName(), account.getEmail(), account.getDepartment(),
				account.getJobTitle(), account.getBasicSalary(), account.getContactNumber(), account.getGender(), account.getAge(),
				account.getRace(), account.getReligion(), account.getAddress(), account.getMaritalStatus(),
				account.getBankName(), account.getBankAccountNumber(), account.getEpfNumber(), account.getIcNumber(),
				account.getTaxNumber(), account.getNumberOfChildren(), account.getCreatedAt(),
				account.getNationality(), account.getResidentStatus(), account.getSpouseWorking(),
				account.getPassportNumber(),
				// Compliance & Key Identity Information
				account.getDateOfBirth(), account.getSocsoNumber(),
				// Position & Employment Information
				account.getDateOfHire(), account.getProbationPeriodLength(), account.getEmploymentType(),
				account.getReportingManagerId(), account.getLocation(),
				// Emergency Contact
				account.getEmergencyContactName(), account.getEmergencyContactRelationship(), account.getEmergencyContactNumber());
	}

	private String nullToEmpty(String value) {
		return value == null ? "" : value;
	}
}

