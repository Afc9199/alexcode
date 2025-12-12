package com.example.employeemanagement.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.employeemanagement.dto.PayslipResponse;
import com.example.employeemanagement.dto.PayrollResponse;
import com.example.employeemanagement.dto.PayrollUpsertRequest;
import com.example.employeemanagement.dto.ProfileResponse;
import com.example.employeemanagement.model.PayrollRecord;
import com.example.employeemanagement.repository.PayrollRepository;
import com.example.employeemanagement.repository.UserAccountRepository;

@Service
public class PayrollService {

	private final PayrollRepository payrollRepository;
	private final UserAccountRepository userAccountRepository;
	private final PayrollCalculationService payrollCalculationService;
	private final UserProfileService userProfileService;
	private final CompanySettingsService companySettingsService;

	public PayrollService(
			PayrollRepository payrollRepository,
			UserAccountRepository userAccountRepository,
			PayrollCalculationService payrollCalculationService,
			UserProfileService userProfileService,
			CompanySettingsService companySettingsService) {
		this.payrollRepository = payrollRepository;
		this.userAccountRepository = userAccountRepository;
		this.payrollCalculationService = payrollCalculationService;
		this.userProfileService = userProfileService;
		this.companySettingsService = companySettingsService;
	}

	public PayrollResponse upsertPayroll(PayrollUpsertRequest request) {
		String userId = requireId(request.userId());
		if (!userAccountRepository.existsById(userId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
		}
		if (request.periodEnd().isBefore(request.periodStart())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Period end must be after period start");
		}
		PayrollRecord record = resolvePayrollRecord(request);
		record.setUserId(userId);
		record.setPeriodStart(request.periodStart());
		record.setPeriodEnd(request.periodEnd());
		record.setBaseSalary(request.baseSalary());
		record.setBonus(request.bonus());
		record.setDeductions(request.deductions());
		record.setNotes(request.notes());
		record.setNetPay(calculateNetPay(request.baseSalary(), request.bonus(), request.deductions()));
		record.setUpdatedAt(Instant.now());
		return toResponse(payrollRepository.save(record));
	}

	public List<PayrollResponse> findForUser(String userId) {
		String safeUserId = requireId(userId);
		if (!userAccountRepository.existsById(safeUserId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
		}
		return payrollRepository.findByUserIdOrderByPeriodEndDesc(safeUserId).stream().map(this::toResponse).toList();
	}

	public List<PayrollResponse> findAll() {
		return payrollRepository.findAll().stream().map(this::toResponse).toList();
	}

	/**
	 * Find payroll record by ID
	 */
	public PayrollResponse findById(String payrollId) {
		String safeId = requireId(payrollId);
		PayrollRecord record = payrollRepository.findById(safeId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payroll record not found"));
		return toResponse(record);
	}

	/**
	 * Delete payroll record
	 */
	public void deletePayroll(String payrollId) {
		String safeId = requireId(payrollId);
		if (!payrollRepository.existsById(safeId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Payroll record not found");
		}
		payrollRepository.deleteById(safeId);
	}

	/**
	 * Calculate and generate payroll for an employee for a specific month
	 */
	public PayslipResponse calculateAndGeneratePayroll(String userId, YearMonth month) {
		String safeUserId = requireId(userId);
		if (!userAccountRepository.existsById(safeUserId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
		}

		// Check if payroll already exists for this month
		Optional<PayrollRecord> existing = payrollRepository.findByUserIdAndSalaryMonth(safeUserId, month);
		PayrollRecord record;

		if (existing.isPresent()) {
			// Recalculate and update existing record
			record = payrollCalculationService.calculatePayroll(safeUserId, month);
			record.setId(existing.get().getId());
			record.setCreatedAt(existing.get().getCreatedAt());
		} else {
			// Create new record
			record = payrollCalculationService.calculatePayroll(safeUserId, month);
		}

		record.setUpdatedAt(Instant.now());
		PayrollRecord saved = payrollRepository.save(record);

		return toPayslipResponse(saved);
	}

	/**
	 * Get payslip for an employee for a specific month
	 */
	public PayslipResponse getPayslip(String userId, YearMonth month) {
		String safeUserId = requireId(userId);
		PayrollRecord record = payrollRepository.findByUserIdAndSalaryMonth(safeUserId, month)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"Payroll record not found for the specified month"));
		return toPayslipResponse(record);
	}

	private PayrollRecord resolvePayrollRecord(PayrollUpsertRequest request) {
		if (request.payrollId() != null && !request.payrollId().isBlank()) {
			return payrollRepository.findById(requireId(request.payrollId()))
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payroll record not found"));
		}
		return payrollRepository
				.findByUserIdAndPeriodStartAndPeriodEnd(requireId(request.userId()), request.periodStart(),
						request.periodEnd())
				.orElseGet(PayrollRecord::new);
	}

	private BigDecimal calculateNetPay(BigDecimal baseSalary, BigDecimal bonus, BigDecimal deductions) {
		return baseSalary.add(bonus).subtract(deductions);
	}

	private PayrollResponse toResponse(PayrollRecord record) {
		return new PayrollResponse(record.getId(), record.getUserId(), record.getPeriodStart(), record.getPeriodEnd(),
				record.getBaseSalary(), record.getBonus(), record.getDeductions(), record.getNetPay(),
				record.getNotes());
	}

	private PayslipResponse toPayslipResponse(PayrollRecord record) {
		ProfileResponse profile = userProfileService.getProfile(record.getUserId());
		com.example.employeemanagement.dto.CompanySettingsResponse companySettings = companySettingsService.getSettings();

		return new PayslipResponse(
				record.getId(),
				record.getUserId(),
				record.getEmployeeId(),
				profile != null ? profile.fullName() : "",
				profile != null ? profile.icNumber() : "",
				profile != null ? profile.epfNumber() : "",
				null, // SOCSO number not in profile
				profile != null ? profile.taxNumber() : "",
				record.getSalaryMonth(),
				companySettings.companyName(),
				companySettings.companyAddress(),
				record.getTotalWorkingDays() != null ? record.getTotalWorkingDays() : 0,
				record.getOtHoursNormal() != null ? record.getOtHoursNormal() : 0.0,
				record.getOtHoursOffDay() != null ? record.getOtHoursOffDay() : 0.0,
				record.getOtHoursPublicHoliday() != null ? record.getOtHoursPublicHoliday() : 0.0,
				record.getUnpaidLeaveDays() != null ? record.getUnpaidLeaveDays() : 0,
				record.getAbsentDays() != null ? record.getAbsentDays() : 0,
				record.getBasicSalary(),
				record.getAdjustedBasicSalary(),
				record.getOtPayNormal() != null ? record.getOtPayNormal() : BigDecimal.ZERO,
				record.getOtPayOffDay() != null ? record.getOtPayOffDay() : BigDecimal.ZERO,
				record.getOtPayPublicHoliday() != null ? record.getOtPayPublicHoliday() : BigDecimal.ZERO,
				record.getTotalOtPay() != null ? record.getTotalOtPay() : BigDecimal.ZERO,
				record.getKpiBonus(),
				record.getBenefitBonus(),
				record.getGrossPay(),
				record.getEpfEmployee(),
				record.getEpfEmployer(),
				record.getSocsoEmployee(),
				record.getSocsoEmployer(),
				record.getEisEmployee(),
				record.getEisEmployer(),
				record.getPcb(),
				record.getTotalEmployeeDeductions(),
				record.getTotalEmployerContributions(),
				record.getNetPay(),
				record.getEmployerCost());
	}

	private @NonNull String requireId(String value) {
		return Objects.requireNonNull(value, "Identifier is required");
	}
}

