package com.example.employeemanagement.service;

import java.time.LocalDate;
import java.util.List;

import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.employeemanagement.dto.LeaveBalanceResponse;
import com.example.employeemanagement.model.LeaveBalance;
import com.example.employeemanagement.model.CompanyLeaveSettings;
import com.example.employeemanagement.model.UserAccount;
import com.example.employeemanagement.repository.LeaveBalanceRepository;
import com.example.employeemanagement.repository.UserAccountRepository;
import com.example.employeemanagement.repository.CompanyLeaveSettingsRepository;

@Service
public class LeaveBalanceService {

	private final LeaveBalanceRepository leaveBalanceRepository;
	private final UserAccountRepository userAccountRepository;
	private final CompanyLeaveSettingsRepository companyLeaveSettingsRepository;

	public LeaveBalanceService(LeaveBalanceRepository leaveBalanceRepository,
			UserAccountRepository userAccountRepository,
			CompanyLeaveSettingsRepository companyLeaveSettingsRepository) {
		this.leaveBalanceRepository = leaveBalanceRepository;
		this.userAccountRepository = userAccountRepository;
		this.companyLeaveSettingsRepository = companyLeaveSettingsRepository;
	}

	/**
	 * Initialize leave balances for an employee based on company leave settings
	 */
	public void initializeLeaveBalancesForEmployee(String userId, String employeeId) {
		// Verify user exists
		String safeUserId = Objects.requireNonNull(userId, "User ID is required");
		if (!userAccountRepository.existsById(safeUserId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
		}

		CompanyLeaveSettings settings = companyLeaveSettingsRepository.findAll().stream()
				.findFirst()
				.orElseGet(() -> {
					CompanyLeaveSettings defaultSettings = new CompanyLeaveSettings();
					return companyLeaveSettingsRepository.save(defaultSettings);
				});

		// Ensure settings has leave types
		if (settings.getAvailableLeaveTypes() == null || settings.getAvailableLeaveTypes().isEmpty()) {
			// If no leave types configured, use default
			settings = new CompanyLeaveSettings();
			settings = companyLeaveSettingsRepository.save(settings);
		}

		// Initialize balances for each leave type
		for (CompanyLeaveSettings.LeaveTypeConfig leaveTypeConfig : settings.getAvailableLeaveTypes()) {
			// Check if balance already exists
			if (!leaveBalanceRepository.findByEmployeeIdAndLeaveType(employeeId, leaveTypeConfig.getName()).isPresent()) {
				// Create new balance
				int totalDays = leaveTypeConfig.getDaysAllowed();
				// For unlimited leave types (-1), set a high number or handle differently
				if (totalDays == -1) {
					totalDays = Integer.MAX_VALUE; // Represent unlimited as max int
				}
				LeaveBalance balance = new LeaveBalance(employeeId, userId, leaveTypeConfig.getName(),
						totalDays, 0);
				leaveBalanceRepository.save(balance);
			}
		}
	}

	/**
	 * Get leave balances for an employee
	 */
	public LeaveBalanceResponse getLeaveBalances(String userId) {
		String safeUserId = Objects.requireNonNull(userId, "User ID is required");
		UserAccount user = userAccountRepository.findById(safeUserId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

		// Check if employee has employeeId
		if (user.getEmployeeId() == null || user.getEmployeeId().trim().isEmpty()) {
			// Return empty balances if employeeId is not set
			return new LeaveBalanceResponse(null, new java.util.ArrayList<>());
		}

		// Ensure balances are initialized
		List<LeaveBalance> balances = leaveBalanceRepository.findByUserId(safeUserId);
		if (balances.isEmpty()) {
			// Initialize balances if they don't exist
			initializeLeaveBalancesForEmployee(safeUserId, user.getEmployeeId());
			balances = leaveBalanceRepository.findByUserId(safeUserId);
		}

		List<LeaveBalanceResponse.LeaveBalanceDto> balanceDtos = balances.stream()
				.map(balance -> new LeaveBalanceResponse.LeaveBalanceDto(
						balance.getLeaveType(),
						balance.getTotalDays() == Integer.MAX_VALUE ? -1 : balance.getTotalDays(),
						balance.getUsedDays(),
						balance.getTotalDays() == Integer.MAX_VALUE ? -1 : balance.getRemainingDays(),
						balance.getTotalDays() == Integer.MAX_VALUE))
				.toList();

		return new LeaveBalanceResponse(user.getEmployeeId(), balanceDtos);
	}

	/**
	 * Get leave balances for an employee by searching with Employee ID or Full Name
	 */
	public LeaveBalanceResponse getLeaveBalancesBySearch(String searchTerm) {
		if (searchTerm == null || searchTerm.trim().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Search term is required");
		}

		String trimmedSearch = searchTerm.trim();
		UserAccount user = null;

		// First, try to find by Employee ID (if it starts with 'E' followed by digits)
		if (trimmedSearch.startsWith("E") && trimmedSearch.length() > 1 && trimmedSearch.substring(1).matches("\\d+")) {
			user = userAccountRepository.findByEmployeeId(trimmedSearch)
					.orElse(null);
		}

		// If not found by Employee ID, try to find by Full Name
		if (user == null) {
			List<UserAccount> usersByName = userAccountRepository.findByFullNameIgnoreCase(trimmedSearch);
			if (usersByName.size() == 1) {
				user = usersByName.get(0);
			} else if (usersByName.size() > 1) {
				throw new ResponseStatusException(HttpStatus.CONFLICT, 
						"Multiple employees found with name: " + trimmedSearch + ". Please use Employee ID instead.");
			}
		}

		if (user == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, 
					"Employee not found with ID or Name: " + trimmedSearch);
		}

		// Use the existing getLeaveBalances method with the found user's ID
		return getLeaveBalances(user.getId());
	}

	/**
	 * Check if employee has sufficient leave balance for the requested days
	 */
	public void validateLeaveBalance(String userId, String leaveType, LocalDate startDate, LocalDate endDate) {
		UserAccount user = userAccountRepository.findById(Objects.requireNonNull(userId, "User ID is required"))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

		if (user.getEmployeeId() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee ID not found");
		}

		// Calculate requested days (inclusive of start and end date)
		long requestedDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;

		// Get leave balance
		LeaveBalance balance = leaveBalanceRepository.findByEmployeeIdAndLeaveType(user.getEmployeeId(), leaveType)
				.orElseGet(() -> {
					// Initialize if not exists
					initializeLeaveBalancesForEmployee(userId, user.getEmployeeId());
					return leaveBalanceRepository.findByEmployeeIdAndLeaveType(user.getEmployeeId(), leaveType)
							.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
									"Leave type not configured: " + leaveType));
				});

		// Check if unlimited
		if (balance.getTotalDays() == Integer.MAX_VALUE) {
			return; // Unlimited leave, no need to check
		}

		// Check if sufficient balance
		if (balance.getRemainingDays() < requestedDays) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					String.format("Insufficient leave balance. Requested: %d days, Available: %d days",
							requestedDays, balance.getRemainingDays()));
		}
	}

	/**
	 * Update leave balance when leave is approved
	 */
	public void updateBalanceOnApproval(String userId, String leaveType, LocalDate startDate, LocalDate endDate) {
		String safeUserId = Objects.requireNonNull(userId, "User ID is required");
		UserAccount user = userAccountRepository.findById(safeUserId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

		if (user.getEmployeeId() == null) {
			return;
		}

		LeaveBalance balance = leaveBalanceRepository.findByEmployeeIdAndLeaveType(user.getEmployeeId(), leaveType)
				.orElse(null);

		if (balance == null) {
			// Initialize if not exists
			initializeLeaveBalancesForEmployee(userId, user.getEmployeeId());
			balance = leaveBalanceRepository.findByEmployeeIdAndLeaveType(user.getEmployeeId(), leaveType)
					.orElse(null);
		}

		if (balance != null && balance.getTotalDays() != Integer.MAX_VALUE) {
			// Calculate days (inclusive)
			long days = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
			balance.addUsedDays((int) days);
			leaveBalanceRepository.save(balance);
		}
	}

	/**
	 * Update leave balance when leave is rejected or cancelled (restore balance)
	 */
	public void updateBalanceOnRejectionOrCancellation(String userId, String leaveType, LocalDate startDate,
			LocalDate endDate) {
		String safeUserId = Objects.requireNonNull(userId, "User ID is required");
		UserAccount user = userAccountRepository.findById(safeUserId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

		if (user.getEmployeeId() == null) {
			return;
		}

		LeaveBalance balance = leaveBalanceRepository.findByEmployeeIdAndLeaveType(user.getEmployeeId(), leaveType)
				.orElse(null);

		if (balance != null && balance.getTotalDays() != Integer.MAX_VALUE) {
			// Calculate days (inclusive)
			long days = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
			balance.subtractUsedDays((int) days);
			leaveBalanceRepository.save(balance);
		}
	}

	/**
	 * Check if employee exceeds leave limits and return alert message if so
	 */
	public String checkLeaveLimitAlert(String userId, String leaveType) {
		UserAccount user = userAccountRepository.findById(Objects.requireNonNull(userId, "User ID is required")).orElse(null);
		if (user == null || user.getEmployeeId() == null) {
			return null;
		}

		LeaveBalance balance = leaveBalanceRepository.findByEmployeeIdAndLeaveType(user.getEmployeeId(), leaveType)
				.orElse(null);

		if (balance == null) {
			return null;
		}

		// Check if unlimited
		if (balance.getTotalDays() == Integer.MAX_VALUE) {
			return null; // No alert for unlimited leave
		}

		// Alert if remaining days are low (less than 20% of total)
		if (balance.getRemainingDays() < (balance.getTotalDays() * 0.2)) {
			return String.format("Warning: You have only %d days remaining for %s (Total: %d days)",
					balance.getRemainingDays(), leaveType, balance.getTotalDays());
		}

		// Alert if no days remaining
		if (balance.getRemainingDays() <= 0) {
			return String.format("Alert: You have exhausted your %s balance (Total: %d days)",
					leaveType, balance.getTotalDays());
		}

		return null;
	}
}

