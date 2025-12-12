package com.example.employeemanagement.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.example.employeemanagement.dto.AttendanceResponse;
import com.example.employeemanagement.dto.ChatContextItem;
import com.example.employeemanagement.dto.EmployeeBenefitResponse;
import com.example.employeemanagement.dto.EmployeeKPIResponse;
import com.example.employeemanagement.dto.LeaveBalanceResponse;
import com.example.employeemanagement.dto.LeaveBalanceResponse.LeaveBalanceDto;
import com.example.employeemanagement.dto.LeaveResponse;
import com.example.employeemanagement.dto.PayrollResponse;
import com.example.employeemanagement.dto.ProfileResponse;

@Service
public class EmployeeChatContextService {

	private static final Logger log = LoggerFactory.getLogger(EmployeeChatContextService.class);

	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	private final UserProfileService userProfileService;
	private final AttendanceService attendanceService;
	private final LeaveService leaveService;
	private final LeaveBalanceService leaveBalanceService;
	private final PayrollService payrollService;
	private final EmployeeBenefitService employeeBenefitService;
	private final EmployeeKPIService employeeKPIService;

	public EmployeeChatContextService(UserProfileService userProfileService, AttendanceService attendanceService,
			LeaveService leaveService, LeaveBalanceService leaveBalanceService, PayrollService payrollService,
			EmployeeBenefitService employeeBenefitService, EmployeeKPIService employeeKPIService) {
		this.userProfileService = userProfileService;
		this.attendanceService = attendanceService;
		this.leaveService = leaveService;
		this.leaveBalanceService = leaveBalanceService;
		this.payrollService = payrollService;
		this.employeeBenefitService = employeeBenefitService;
		this.employeeKPIService = employeeKPIService;
	}

	public EmployeeContextSummary prepareContext(String userId) {
		EmployeeDataSnapshot snapshot = new EmployeeDataSnapshot(
				userProfileService.getProfile(userId),
				loadListSafely("attendance", () -> attendanceService.findForUser(userId)),
				loadListSafely("leave requests", () -> leaveService.findForUser(userId)),
				loadSafely("leave balances", () -> leaveBalanceService.getLeaveBalances(userId)),
				loadListSafely("payroll", () -> payrollService.findForUser(userId)),
				loadListSafely("benefits", () -> employeeBenefitService.getBenefitsByUserId(userId)),
				loadListSafely("kpis", () -> employeeKPIService.getKPIsByUserId(userId)));

		List<ChatContextItem> contextItems = List.copyOf(buildContextItems(snapshot));
		String narrative = contextItems.isEmpty()
				? "No reliable employee records were found for this user."
				: contextItems.stream()
						.map(item -> "- " + item.label() + ": " + item.details())
						.collect(Collectors.joining("\n"));

		return new EmployeeContextSummary(snapshot, contextItems, narrative);
	}

	private List<ChatContextItem> buildContextItems(EmployeeDataSnapshot snapshot) {
		if (snapshot == null || snapshot.profile() == null) {
			return List.of();
		}
		return new ContextBuilder(snapshot).build();
	}

	private <T> T loadSafely(String label, Supplier<T> supplier) {
		try {
			return supplier.get();
		} catch (Exception ex) {
			log.warn("Unable to load {} for chat context: {}", label, ex.getMessage());
			return null;
		}
	}

	private <T> List<T> loadListSafely(String label, Supplier<List<T>> supplier) {
		List<T> result = loadSafely(label, supplier);
		return result != null ? result : List.of();
	}

	public record EmployeeContextSummary(
			EmployeeDataSnapshot snapshot,
			List<ChatContextItem> items,
			String narrative) {
	}

	public record EmployeeDataSnapshot(
			ProfileResponse profile,
			List<AttendanceResponse> attendance,
			List<LeaveResponse> leaveRequests,
			LeaveBalanceResponse leaveBalance,
			List<PayrollResponse> payrolls,
			List<EmployeeBenefitResponse> benefits,
			List<EmployeeKPIResponse> kpis) {
	}

	private static final class ContextBuilder {
		private final EmployeeDataSnapshot snapshot;

		private ContextBuilder(EmployeeDataSnapshot snapshot) {
			this.snapshot = snapshot;
		}

		private List<ChatContextItem> build() {
			if (snapshot == null || snapshot.profile() == null) {
				return List.of();
			}
			List<ChatContextItem> items = new java.util.ArrayList<>();
			items.add(buildProfileItem(snapshot.profile()));
			addIfPresent(items, buildAttendanceItem(snapshot.attendance()));
			addIfPresent(items, buildLeaveBalanceItem(snapshot.leaveBalance()));
			addIfPresent(items, buildLeaveStatusItem(snapshot.leaveRequests()));
			addIfPresent(items, buildPayrollItem(snapshot.payrolls()));
			addIfPresent(items, buildBenefitsItem(snapshot.benefits()));
			addIfPresent(items, buildKpiItem(snapshot.kpis()));
			return items;
		}

		private void addIfPresent(List<ChatContextItem> items, ChatContextItem candidate) {
			if (candidate != null) {
				items.add(candidate);
			}
		}

		private ChatContextItem buildProfileItem(ProfileResponse profile) {
			StringBuilder details = new StringBuilder();
			details.append(profile.fullName() != null ? profile.fullName() : profile.username());
			if (profile.employeeId() != null) {
				details.append(" (ID: ").append(profile.employeeId()).append(')');
			}
			if (profile.department() != null) {
				details.append(", Department: ").append(profile.department());
			}
			if (profile.jobTitle() != null) {
				details.append(", Title: ").append(profile.jobTitle());
			}
			if (profile.basicSalary() != null) {
				details.append(", Basic salary: ").append(formatMoney(profile.basicSalary()));
			}
			return new ChatContextItem("PROFILE", "Employee profile", details.toString());
		}

		private ChatContextItem buildAttendanceItem(List<AttendanceResponse> records) {
			if (CollectionUtils.isEmpty(records)) {
				return null;
			}
			String summary = records.stream()
					.filter(Objects::nonNull)
					.limit(5)
					.map(record -> {
						String date = record.workDate() != null ? DATE.format(record.workDate()) : "Unknown date";
						String status = record.status() != null ? record.status().name() : "UNKNOWN";
						return date + " → " + status;
					})
					.collect(Collectors.joining("; "));
			return new ChatContextItem("ATTENDANCE", "Recent attendance", summary);
		}

		private ChatContextItem buildLeaveBalanceItem(LeaveBalanceResponse balances) {
			if (balances == null || CollectionUtils.isEmpty(balances.balances())) {
				return null;
			}
			String summary = balances.balances().stream()
					.limit(5)
					.map(this::formatLeaveBalance)
					.collect(Collectors.joining("; "));
			return new ChatContextItem("LEAVE_BALANCE", "Leave balances", summary);
		}

		private String formatLeaveBalance(LeaveBalanceDto dto) {
			if (dto == null) {
				return "";
			}
			if (dto.isUnlimited()) {
				return dto.leaveType() + ": Unlimited";
			}
			return dto.leaveType() + ": " + dto.remainingDays() + "/" + dto.totalDays() + " days remaining";
		}

		private ChatContextItem buildLeaveStatusItem(List<LeaveResponse> requests) {
			if (CollectionUtils.isEmpty(requests)) {
				return null;
			}
			String summary = requests.stream()
					.filter(Objects::nonNull)
					.limit(3)
					.map(request -> {
						String type = request.leaveType() != null ? request.leaveType() : "Leave";
						String range = formatDateRange(request.startDate(), request.endDate());
						String status = request.status() != null ? request.status().name() : "PENDING";
						return type + " " + range + " → " + status;
					})
					.collect(Collectors.joining("; "));
			return new ChatContextItem("LEAVE", "Recent leave requests", summary);
		}

		private ChatContextItem buildPayrollItem(List<PayrollResponse> payrolls) {
			if (CollectionUtils.isEmpty(payrolls)) {
				return null;
			}
			String summary = payrolls.stream()
					.filter(Objects::nonNull)
					.limit(3)
					.map(record -> {
						String range = formatDateRange(record.periodStart(), record.periodEnd());
						String netPay = record.netPay() != null ? formatMoney(record.netPay()) : "N/A";
						return range + " Net pay: " + netPay;
					})
					.collect(Collectors.joining("; "));
			return new ChatContextItem("PAYROLL", "Latest payroll", summary);
		}

		private ChatContextItem buildBenefitsItem(List<EmployeeBenefitResponse> benefits) {
			if (CollectionUtils.isEmpty(benefits)) {
				return null;
			}
			String summary = benefits.stream()
					.filter(Objects::nonNull)
					.limit(5)
					.map(benefit -> benefit.benefitName() + (benefit.benefitAmount() != null
							? " (" + formatDouble(benefit.benefitAmount()) + ")"
							: ""))
					.collect(Collectors.joining("; "));
			return new ChatContextItem("BENEFITS", "Active benefits", summary);
		}

		private ChatContextItem buildKpiItem(List<EmployeeKPIResponse> kpis) {
			if (CollectionUtils.isEmpty(kpis)) {
				return null;
			}
			String summary = kpis.stream()
					.filter(Objects::nonNull)
					.limit(5)
					.map(kpi -> {
						String name = kpi.kpiName() != null ? kpi.kpiName() : "KPI";
						String percentage = kpi.progressPercentage() != null
								? String.format(Locale.US, "%.0f%%", kpi.progressPercentage())
								: "0%";
						String status = kpi.status() != null ? kpi.status() : "PENDING";
						return name + " → " + percentage + " (" + status + ")";
					})
					.collect(Collectors.joining("; "));
			return new ChatContextItem("KPI", "Performance objectives", summary);
		}

		private String formatDateRange(java.time.LocalDate start, java.time.LocalDate end) {
			if (start == null && end == null) {
				return "Dates N/A";
			}
			if (start != null && end != null) {
				return DATE.format(start) + " to " + DATE.format(end);
			}
			if (start != null) {
				return DATE.format(start);
			}
			return DATE.format(end);
		}

		private String formatMoney(Number value) {
			if (value == null) {
				return "N/A";
			}
			BigDecimal decimal = value instanceof BigDecimal bd
					? bd
					: BigDecimal.valueOf(value.doubleValue());
			return "RM " + decimal.setScale(2, RoundingMode.HALF_UP).toPlainString();
		}

		private String formatDouble(Double value) {
			if (value == null) {
				return "N/A";
			}
			return String.format(Locale.US, "RM %.2f", value);
		}
	}
}

