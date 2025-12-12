package com.example.employeemanagement.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.employeemanagement.dto.CompanyLeaveSettingsRequest;
import com.example.employeemanagement.dto.CompanyLeaveSettingsResponse;
import com.example.employeemanagement.model.CompanyLeaveSettings;
import com.example.employeemanagement.model.LeaveBalance;
import com.example.employeemanagement.model.UserAccount;
import com.example.employeemanagement.repository.CompanyLeaveSettingsRepository;
import com.example.employeemanagement.repository.LeaveBalanceRepository;
import com.example.employeemanagement.repository.UserAccountRepository;

@Service
public class CompanyLeaveSettingsService {

	private final CompanyLeaveSettingsRepository repository;
	private final LeaveBalanceRepository leaveBalanceRepository;
	private final UserAccountRepository userAccountRepository;

	public CompanyLeaveSettingsService(CompanyLeaveSettingsRepository repository,
			LeaveBalanceRepository leaveBalanceRepository, UserAccountRepository userAccountRepository) {
		this.repository = repository;
		this.leaveBalanceRepository = leaveBalanceRepository;
		this.userAccountRepository = userAccountRepository;
	}

	public CompanyLeaveSettingsResponse getSettings() {
		CompanyLeaveSettings settings = getOrCreateSettings();
		return toResponse(settings);
	}

	public CompanyLeaveSettingsResponse updateSettings(CompanyLeaveSettingsRequest request) {
		CompanyLeaveSettings settings = getOrCreateSettings();
		
		// Create a map of current leave types (name -> daysAllowed) for comparison
		Map<String, Integer> currentLeaveTypesMap = settings.getAvailableLeaveTypes().stream()
				.collect(Collectors.toMap(
						CompanyLeaveSettings.LeaveTypeConfig::getName,
						CompanyLeaveSettings.LeaveTypeConfig::getDaysAllowed));
		
		// Get current leave type names
		Set<String> currentLeaveTypes = currentLeaveTypesMap.keySet();
		
		// Get new leave types from request
		Map<String, Integer> newLeaveTypesMap = request.availableLeaveTypes().stream()
				.collect(Collectors.toMap(
						dto -> dto.name(),
						dto -> dto.daysAllowed()));
		
		Set<String> newLeaveTypes = newLeaveTypesMap.keySet();
		
		// Find deleted leave types (in current but not in new)
		Set<String> deletedLeaveTypes = currentLeaveTypes.stream()
				.filter(type -> !newLeaveTypes.contains(type))
				.collect(Collectors.toSet());
		
		// Delete leave balances for removed leave types
		for (String deletedLeaveType : deletedLeaveTypes) {
			leaveBalanceRepository.deleteByLeaveType(deletedLeaveType);
		}
		
		// Create leave balances for newly added leave types
		Set<String> addedLeaveTypes = newLeaveTypes.stream()
				.filter(type -> !currentLeaveTypes.contains(type))
				.collect(Collectors.toSet());
		initializeBalancesForNewLeaveTypes(addedLeaveTypes, newLeaveTypesMap);

		// Update leave balances for leave types that still exist but have different days
		for (String leaveTypeName : newLeaveTypes) {
			if (currentLeaveTypes.contains(leaveTypeName)) {
				// Leave type exists, check if days changed
				Integer currentDays = currentLeaveTypesMap.get(leaveTypeName);
				Integer newDays = newLeaveTypesMap.get(leaveTypeName);
				
				if (currentDays != null && !currentDays.equals(newDays)) {
					// Days have changed, update all existing balances for this leave type
					List<LeaveBalance> balances = leaveBalanceRepository.findAll().stream()
							.filter(balance -> leaveTypeName.equals(balance.getLeaveType()))
							.collect(Collectors.toList());
					
					for (LeaveBalance balance : balances) {
						int newTotalDays = newDays == -1 ? Integer.MAX_VALUE : newDays;
						int usedDays = balance.getUsedDays();
						
						// Update total days and recalculate remaining days
						balance.setTotalDays(newTotalDays);
						balance.setUsedDays(usedDays); // This will trigger recalculation of remainingDays
						leaveBalanceRepository.save(balance);
					}
				}
			}
		}
		
		// Update settings
		settings.setAvailableLeaveTypes(
				request.availableLeaveTypes().stream()
						.map(dto -> new CompanyLeaveSettings.LeaveTypeConfig(dto.name(), dto.daysAllowed()))
						.collect(Collectors.toList()));
		
		if (request.publicHolidays() != null) {
			settings.setPublicHolidays(
					request.publicHolidays().stream()
							.map(dto -> new CompanyLeaveSettings.PublicHoliday(dto.date(), dto.name()))
							.collect(Collectors.toList()));
		}
		
		return toResponse(repository.save(settings));
	}

	private void initializeBalancesForNewLeaveTypes(Set<String> newLeaveTypes,
			Map<String, Integer> newLeaveTypesMap) {
		if (newLeaveTypes.isEmpty()) {
			return;
		}
		List<UserAccount> employees = userAccountRepository.findAll().stream()
				.filter(user -> user.getEmployeeId() != null && !user.getEmployeeId().isBlank())
				.toList();

		for (String leaveTypeName : newLeaveTypes) {
			Integer daysAllowed = newLeaveTypesMap.getOrDefault(leaveTypeName, 0);
			int totalDays = daysAllowed != null && daysAllowed == -1 ? Integer.MAX_VALUE : (daysAllowed != null ? daysAllowed : 0);

			for (UserAccount employee : employees) {
				boolean exists = leaveBalanceRepository
						.findByEmployeeIdAndLeaveType(employee.getEmployeeId(), leaveTypeName)
						.isPresent();
				if (!exists) {
					LeaveBalance balance = new LeaveBalance(employee.getEmployeeId(),
							employee.getId(), leaveTypeName, totalDays, 0);
					leaveBalanceRepository.save(balance);
				}
			}
		}
	}

	private CompanyLeaveSettings getOrCreateSettings() {
		return repository.findAll().stream()
				.findFirst()
				.map(settings -> {
					// Migration: if leave types list is empty or null, reinitialize
					if (settings.getAvailableLeaveTypes() == null || settings.getAvailableLeaveTypes().isEmpty()) {
						settings = new CompanyLeaveSettings();
						settings.setPublicHolidays(repository.findAll().stream()
								.findFirst()
								.map(CompanyLeaveSettings::getPublicHolidays)
								.orElse(new java.util.ArrayList<>()));
						return repository.save(settings);
					}
					return settings;
				})
				.orElseGet(() -> repository.save(new CompanyLeaveSettings()));
	}

	private CompanyLeaveSettingsResponse toResponse(CompanyLeaveSettings settings) {
		var leaveTypes = settings.getAvailableLeaveTypes().stream()
				.map(lt -> new CompanyLeaveSettingsResponse.LeaveTypeDto(lt.getName(), lt.getDaysAllowed()))
				.collect(Collectors.toList());
		var holidays = settings.getPublicHolidays().stream()
				.map(h -> new CompanyLeaveSettingsResponse.PublicHolidayDto(h.getDate(), h.getName()))
				.collect(Collectors.toList());
		return new CompanyLeaveSettingsResponse(leaveTypes, holidays);
	}
}

