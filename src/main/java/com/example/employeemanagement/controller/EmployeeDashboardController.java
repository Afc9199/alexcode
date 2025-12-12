package com.example.employeemanagement.controller;

import java.util.List;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.example.employeemanagement.dto.AnnouncementResponse;
import com.example.employeemanagement.dto.AttendanceResponse;
import com.example.employeemanagement.dto.CreateAttendanceRequest;
import com.example.employeemanagement.dto.LeaveResponse;
import com.example.employeemanagement.dto.OvertimeResponse;
import com.example.employeemanagement.dto.CreateOvertimeRequest;
import com.example.employeemanagement.dto.PayrollResponse;
import com.example.employeemanagement.dto.ProfileResponse;
import com.example.employeemanagement.dto.ProfileUpdateRequest;
import com.example.employeemanagement.dto.ChangePasswordRequest;
import com.example.employeemanagement.model.AnnouncementAudience;
import com.example.employeemanagement.dto.EmployeeBenefitResponse;
import com.example.employeemanagement.dto.EmployeeKPIResponse;
import com.example.employeemanagement.dto.LeaveBalanceResponse;
import com.example.employeemanagement.dto.PayslipResponse;
import com.example.employeemanagement.service.AnnouncementService;
import com.example.employeemanagement.service.AttendanceService;
import com.example.employeemanagement.service.CompanyLeaveSettingsService;
import com.example.employeemanagement.service.LeaveBalanceService;
import com.example.employeemanagement.service.LeaveService;
import com.example.employeemanagement.service.OvertimeService;
import com.example.employeemanagement.service.PayrollService;
import com.example.employeemanagement.service.EmployeeBenefitService;
import com.example.employeemanagement.service.EmployeeKPIService;
import com.example.employeemanagement.service.UserProfileService;
import com.example.employeemanagement.service.DepartmentService;
import com.example.employeemanagement.dto.DepartmentResponse;
import com.example.employeemanagement.dto.DashboardSummaryResponse;
import com.example.employeemanagement.service.ReportService;
import com.example.employeemanagement.service.ReportExportService;
import com.example.employeemanagement.dto.AttendanceReportResponse;
import com.example.employeemanagement.dto.PayrollReportResponse;
import com.example.employeemanagement.dto.KPIPerformanceReportResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.time.YearMonth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/employee")
@Validated
public class EmployeeDashboardController {

	private final UserProfileService userProfileService;
	private final AttendanceService attendanceService;
	private final LeaveService leaveService;
	private final OvertimeService overtimeService;
	private final PayrollService payrollService;
	private final AnnouncementService announcementService;
	private final CompanyLeaveSettingsService companyLeaveSettingsService;
	private final LeaveBalanceService leaveBalanceService;
	private final EmployeeBenefitService employeeBenefitService;
	private final EmployeeKPIService employeeKPIService;
	private final DepartmentService departmentService;
	private final com.example.employeemanagement.service.DashboardSummaryService dashboardSummaryService;
	private final ReportService reportService;
	private final ReportExportService reportExportService;

	public EmployeeDashboardController(UserProfileService userProfileService, AttendanceService attendanceService,
			LeaveService leaveService, OvertimeService overtimeService, PayrollService payrollService, AnnouncementService announcementService,
			CompanyLeaveSettingsService companyLeaveSettingsService, LeaveBalanceService leaveBalanceService,
			EmployeeBenefitService employeeBenefitService, EmployeeKPIService employeeKPIService,
			DepartmentService departmentService,
			com.example.employeemanagement.service.DashboardSummaryService dashboardSummaryService,
			ReportService reportService, ReportExportService reportExportService) {
		this.userProfileService = userProfileService;
		this.attendanceService = attendanceService;
		this.leaveService = leaveService;
		this.overtimeService = overtimeService;
		this.payrollService = payrollService;
		this.announcementService = announcementService;
		this.companyLeaveSettingsService = companyLeaveSettingsService;
		this.leaveBalanceService = leaveBalanceService;
		this.employeeBenefitService = employeeBenefitService;
		this.employeeKPIService = employeeKPIService;
		this.departmentService = departmentService;
		this.dashboardSummaryService = dashboardSummaryService;
		this.reportService = reportService;
		this.reportExportService = reportExportService;
	}

	@GetMapping("/profile/{userId}")
	public ProfileResponse getProfile(@PathVariable String userId) {
		return userProfileService.getProfile(userId);
	}

	@PutMapping("/profile/{userId}")
	public ProfileResponse updateProfile(@PathVariable String userId, @Valid @RequestBody ProfileUpdateRequest request) {
		return userProfileService.updateProfile(userId, request);
	}

	@PostMapping("/profile/{userId}/change-password")
	public void changePassword(@PathVariable String userId, @Valid @RequestBody ChangePasswordRequest request) {
		userProfileService.changePassword(userId, request);
	}

	@PostMapping("/attendance")
	public AttendanceResponse recordAttendance(@Valid @RequestBody CreateAttendanceRequest request,
			HttpServletRequest httpRequest) {
		return attendanceService.recordAttendance(request, extractClientIp(httpRequest));
	}

	@GetMapping("/attendance/{userId}")
	public List<AttendanceResponse> myAttendance(@PathVariable String userId) {
		return attendanceService.findForUser(userId);
	}

	@PostMapping("/leave")
	public LeaveResponse submitLeave(
			@RequestParam("userId") String userId,
			@RequestParam("leaveType") String leaveType,
			@RequestParam("startDate") String startDate,
			@RequestParam("endDate") String endDate,
			@RequestParam("reason") String reason,
			@RequestParam(value = "supportingDocument", required = false) MultipartFile supportingDocument) {
		return leaveService.submitLeave(userId, leaveType, startDate, endDate, reason, supportingDocument);
	}

	@GetMapping("/leave/{userId}")
	public List<LeaveResponse> myLeave(@PathVariable String userId) {
		return leaveService.findForUser(userId);
	}

	@GetMapping("/leave-types")
	public List<String> getAvailableLeaveTypes() {
		return companyLeaveSettingsService.getSettings().availableLeaveTypes().stream()
				.map(lt -> lt.name())
				.collect(java.util.stream.Collectors.toList());
	}

	@GetMapping("/leave-balance/{userId}")
	public LeaveBalanceResponse getLeaveBalance(@PathVariable String userId) {
		return leaveBalanceService.getLeaveBalances(userId);
	}

	@PostMapping("/overtime")
	public OvertimeResponse submitOvertime(@Valid @RequestBody CreateOvertimeRequest request) {
		return overtimeService.submitOvertime(request);
	}

	@GetMapping("/overtime/{userId}")
	public List<OvertimeResponse> myOvertime(@PathVariable String userId) {
		return overtimeService.findForUser(userId);
	}

	@GetMapping("/overtime/next-overtime-id")
	public String getNextOvertimeId() {
		return overtimeService.getNextOvertimeId();
	}

	@GetMapping("/benefits/{userId}")
	public List<EmployeeBenefitResponse> getMyBenefits(@PathVariable String userId) {
		return employeeBenefitService.getBenefitsByUserId(userId);
	}

	@GetMapping("/kpis/{userId}")
	public List<EmployeeKPIResponse> getMyKpis(@PathVariable String userId) {
		return employeeKPIService.getKPIsByUserId(userId);
	}

	@PostMapping(value = "/kpis/{assignmentId}/progress", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public EmployeeKPIResponse updateKpiProgress(@PathVariable String assignmentId,
			@RequestParam("userId") String userId,
			@RequestParam("progressValue") Double progressValue,
			@RequestPart("evidence") MultipartFile evidence) {
		if (progressValue == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Progress value is required");
		}
		return employeeKPIService.updateProgress(assignmentId, userId, progressValue, evidence);
	}

	@GetMapping("/payroll/{userId}")
	public List<PayrollResponse> myPayroll(@PathVariable String userId) {
		return payrollService.findForUser(userId);
	}

	@GetMapping("/payroll/payslip/{userId}/{year}/{month}")
	public ResponseEntity<?> getMyPayslip(@PathVariable String userId, @PathVariable int year, @PathVariable int month,
			HttpServletRequest request) {
		try {
			// Verify that the user is requesting their own payslip
			jakarta.servlet.http.HttpSession session = request.getSession(false);
			if (session == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(java.util.Map.of("error", "Not authenticated", "message", "Please log in"));
			}
			Object attribute = session.getAttribute(com.example.employeemanagement.security.SessionAttributes.AUTH_USER);
			if (!(attribute instanceof com.example.employeemanagement.security.AuthenticatedUser user)) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(java.util.Map.of("error", "Not authenticated", "message", "Please log in"));
			}
			// Verify the userId matches the logged-in user
			if (!user.userId().equals(userId)) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN)
						.body(java.util.Map.of("error", "Forbidden", "message", "You can only view your own payslip"));
			}
			java.time.YearMonth yearMonth = java.time.YearMonth.of(year, month);
			PayslipResponse response = payrollService.getPayslip(userId, yearMonth);
			return ResponseEntity.ok(response);
		} catch (org.springframework.web.server.ResponseStatusException e) {
			return ResponseEntity.status(e.getStatusCode())
					.body(java.util.Map.of("error", e.getReason(), "message", e.getReason()));
		} catch (Exception e) {
			return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
					.body(java.util.Map.of("error", "Failed to load payslip", "message", e.getMessage()));
		}
	}

	@GetMapping("/announcements")
	public ResponseEntity<List<AnnouncementResponse>> announcements() {
		return ResponseEntity.ok(announcementService.listForAudience(AnnouncementAudience.EMPLOYEE));
	}

	@GetMapping("/leave-documents/{filename}")
	public ResponseEntity<Resource> getLeaveDocument(@PathVariable String filename) {
		try {
			// Use absolute path to match where files are saved
			Path filePath = Paths.get(System.getProperty("user.dir"), "uploads", "leave-documents", filename).normalize();
			
			// Security check: ensure the resolved path is within the uploads directory
			Path uploadsDir = Paths.get(System.getProperty("user.dir"), "uploads", "leave-documents").normalize();
			if (!filePath.startsWith(uploadsDir)) {
				return ResponseEntity.badRequest().build();
			}
			
			Resource resource = new UrlResource(Objects.requireNonNull(filePath.toUri()));
			
			if (resource.exists() && resource.isReadable()) {
				// Determine content type based on file extension
				String contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
				String lowerFilename = filename.toLowerCase();
				if (lowerFilename.endsWith(".jpg") || lowerFilename.endsWith(".jpeg")) {
					contentType = "image/jpeg";
				} else if (lowerFilename.endsWith(".png")) {
					contentType = "image/png";
				} else if (lowerFilename.endsWith(".gif")) {
					contentType = "image/gif";
				} else if (lowerFilename.endsWith(".pdf")) {
					contentType = "application/pdf";
				} else if (lowerFilename.endsWith(".doc")) {
					contentType = "application/msword";
				} else if (lowerFilename.endsWith(".docx")) {
					contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
				}
				
				return ResponseEntity.ok()
						.contentType(MediaType.parseMediaType(contentType))
						.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename.substring(filename.indexOf('_') + 1) + "\"")
						.body(resource);
			} else {
				return ResponseEntity.notFound().build();
			}
		} catch (Exception e) {
			return ResponseEntity.notFound().build();
		}
	}

	@GetMapping("/kpis/evidence/{filename}")
	public ResponseEntity<Resource> getKpiEvidence(@PathVariable String filename) {
		try {
			Path filePath = Paths.get(System.getProperty("user.dir"), "uploads", "kpi-evidence", filename).normalize();
			Path evidenceDir = Paths.get(System.getProperty("user.dir"), "uploads", "kpi-evidence").normalize();
			if (!filePath.startsWith(evidenceDir)) {
				return ResponseEntity.badRequest().build();
			}

			Resource resource = new UrlResource(Objects.requireNonNull(filePath.toUri()));
			if (resource.exists() && resource.isReadable()) {
				String contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
				String lowerFilename = filename.toLowerCase();
				if (lowerFilename.endsWith(".jpg") || lowerFilename.endsWith(".jpeg")) {
					contentType = "image/jpeg";
				} else if (lowerFilename.endsWith(".png")) {
					contentType = "image/png";
				} else if (lowerFilename.endsWith(".gif")) {
					contentType = "image/gif";
				} else if (lowerFilename.endsWith(".pdf")) {
					contentType = "application/pdf";
				} else if (lowerFilename.endsWith(".doc")) {
					contentType = "application/msword";
				} else if (lowerFilename.endsWith(".docx")) {
					contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
				}

				return ResponseEntity.ok()
						.contentType(MediaType.parseMediaType(contentType))
						.header(HttpHeaders.CONTENT_DISPOSITION,
								"inline; filename=\"" + filename.substring(filename.indexOf('_') + 1) + "\"")
						.body(resource);
			}
			return ResponseEntity.notFound().build();
		} catch (Exception ex) {
			return ResponseEntity.notFound().build();
		}
	}

	@GetMapping("/departments")
	public List<DepartmentResponse> getDepartments() {
		return departmentService.findAll();
	}

	@GetMapping("/dashboard/summary")
	public DashboardSummaryResponse getDashboardSummary(HttpServletRequest request) {
		jakarta.servlet.http.HttpSession session = request.getSession(false);
		if (session == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
		}
		Object attribute = session.getAttribute(com.example.employeemanagement.security.SessionAttributes.AUTH_USER);
		if (attribute instanceof com.example.employeemanagement.security.AuthenticatedUser user) {
			return dashboardSummaryService.getEmployeeSummary(user.userId());
		}
		throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
	}

	// Employee Reports - Only returns data for the current logged-in employee
	private com.example.employeemanagement.security.AuthenticatedUser getCurrentUser(HttpServletRequest request) {
		jakarta.servlet.http.HttpSession session = request.getSession(false);
		if (session == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
		}
		Object attribute = session.getAttribute(com.example.employeemanagement.security.SessionAttributes.AUTH_USER);
		if (attribute instanceof com.example.employeemanagement.security.AuthenticatedUser user) {
			return user;
		}
		throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
	}

	@GetMapping("/reports/attendance")
	public ResponseEntity<AttendanceReportResponse> generateMyAttendanceReport(
			@RequestParam String type,
			@RequestParam String startDate,
			@RequestParam String endDate,
			HttpServletRequest request) {
		try {
			com.example.employeemanagement.security.AuthenticatedUser currentUser = getCurrentUser(request);
			LocalDate start = LocalDate.parse(startDate);
			LocalDate end = LocalDate.parse(endDate);
			AttendanceReportResponse report = reportService.generateEmployeeAttendanceReport(
					currentUser.userId(), type, start, end);
			return ResponseEntity.ok(report);
		} catch (Exception e) {
			return ResponseEntity.badRequest().build();
		}
	}

	@GetMapping("/reports/performance")
	public ResponseEntity<?> generateMyPerformanceReport(
			@RequestParam(required = false, defaultValue = "2024") Integer year,
			HttpServletRequest request) {
		try {
			com.example.employeemanagement.security.AuthenticatedUser currentUser = getCurrentUser(request);
			org.slf4j.LoggerFactory.getLogger(getClass()).debug("Generating performance report for user: {}, year: {}", 
					currentUser.userId(), year);
			KPIPerformanceReportResponse report = reportService.generateEmployeeKPIPerformanceReport(
					currentUser.userId(), year);
			return ResponseEntity.ok(report);
		} catch (ResponseStatusException e) {
			throw e;
		} catch (Exception e) {
			org.slf4j.LoggerFactory.getLogger(getClass()).error("Error generating performance report", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new java.util.HashMap<String, String>() {{
						put("error", "Failed to generate performance report: " + e.getMessage());
					}});
		}
	}

	@GetMapping("/reports/payroll")
	public ResponseEntity<PayrollReportResponse> generateMyPayrollReport(
			@RequestParam String period,
			@RequestParam(required = false) String month,
			@RequestParam(required = false) Integer year,
			@RequestParam(defaultValue = "true") boolean includeStatutory,
			HttpServletRequest request) {
		try {
			com.example.employeemanagement.security.AuthenticatedUser currentUser = getCurrentUser(request);
			PayrollReportResponse report;
			
			if ("monthly".equals(period) && month != null) {
				YearMonth yearMonth = YearMonth.parse(month);
				report = reportService.generateEmployeePayrollReport(
						currentUser.userId(), yearMonth, includeStatutory);
			} else if ("annual".equals(period) && year != null) {
				report = reportService.generateEmployeeAnnualPayrollReport(
						currentUser.userId(), year, includeStatutory);
			} else {
				return ResponseEntity.badRequest().build();
			}
			
			return ResponseEntity.ok(report);
		} catch (Exception e) {
			return ResponseEntity.badRequest().build();
		}
	}

	// Export Endpoints
	@GetMapping("/reports/attendance/export/excel")
	public ResponseEntity<byte[]> exportMyAttendanceExcel(
			@RequestParam String type,
			@RequestParam String startDate,
			@RequestParam String endDate,
			HttpServletRequest request) {
		try {
			com.example.employeemanagement.security.AuthenticatedUser currentUser = getCurrentUser(request);
			LocalDate start = LocalDate.parse(startDate);
			LocalDate end = LocalDate.parse(endDate);
			AttendanceReportResponse report = reportService.generateEmployeeAttendanceReport(
					currentUser.userId(), type, start, end);
			byte[] excelData = reportExportService.exportAttendanceToExcel(report);
			
			String filename = "my_attendance_report_" + startDate + "_to_" + endDate + ".xlsx";
			return ResponseEntity.ok()
					.contentType(MediaType.APPLICATION_OCTET_STREAM)
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
					.body(excelData);
		} catch (Exception e) {
			return ResponseEntity.badRequest().build();
		}
	}

	@GetMapping("/reports/attendance/export/pdf")
	public ResponseEntity<byte[]> exportMyAttendancePDF(
			@RequestParam String type,
			@RequestParam String startDate,
			@RequestParam String endDate,
			HttpServletRequest request) {
		try {
			com.example.employeemanagement.security.AuthenticatedUser currentUser = getCurrentUser(request);
			LocalDate start = LocalDate.parse(startDate);
			LocalDate end = LocalDate.parse(endDate);
			AttendanceReportResponse report = reportService.generateEmployeeAttendanceReport(
					currentUser.userId(), type, start, end);
			byte[] pdfData = reportExportService.exportAttendanceToPDF(report);
			
			String filename = "my_attendance_report_" + startDate + "_to_" + endDate + ".pdf";
			return ResponseEntity.ok()
					.contentType(MediaType.APPLICATION_PDF)
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
					.body(pdfData);
		} catch (Exception e) {
			return ResponseEntity.badRequest().build();
		}
	}

	@GetMapping("/reports/performance/export/excel")
	public ResponseEntity<byte[]> exportMyPerformanceExcel(
			@RequestParam int year,
			HttpServletRequest request) {
		try {
			com.example.employeemanagement.security.AuthenticatedUser currentUser = getCurrentUser(request);
			KPIPerformanceReportResponse report = reportService.generateEmployeeKPIPerformanceReport(
					currentUser.userId(), year);
			byte[] excelData = reportExportService.exportKPIPerformanceToExcel(report);
			
			String filename = "my_performance_report_" + year + ".xlsx";
			return ResponseEntity.ok()
					.contentType(MediaType.APPLICATION_OCTET_STREAM)
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
					.body(excelData);
		} catch (Exception e) {
			return ResponseEntity.badRequest().build();
		}
	}

	@GetMapping("/reports/performance/export/pdf")
	public ResponseEntity<byte[]> exportMyPerformancePDF(
			@RequestParam int year,
			HttpServletRequest request) {
		try {
			com.example.employeemanagement.security.AuthenticatedUser currentUser = getCurrentUser(request);
			KPIPerformanceReportResponse report = reportService.generateEmployeeKPIPerformanceReport(
					currentUser.userId(), year);
			byte[] pdfData = reportExportService.exportKPIPerformanceToPDF(report);
			
			String filename = "my_performance_report_" + year + ".pdf";
			return ResponseEntity.ok()
					.contentType(MediaType.APPLICATION_PDF)
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
					.body(pdfData);
		} catch (Exception e) {
			return ResponseEntity.badRequest().build();
		}
	}

	@GetMapping("/reports/payroll/export/excel")
	public ResponseEntity<byte[]> exportMyPayrollExcel(
			@RequestParam String period,
			@RequestParam(required = false) String month,
			@RequestParam(required = false) Integer year,
			@RequestParam(defaultValue = "true") boolean includeStatutory,
			HttpServletRequest request) {
		try {
			com.example.employeemanagement.security.AuthenticatedUser currentUser = getCurrentUser(request);
			PayrollReportResponse report;
			
			if ("monthly".equals(period) && month != null) {
				YearMonth yearMonth = YearMonth.parse(month);
				report = reportService.generateEmployeePayrollReport(
						currentUser.userId(), yearMonth, includeStatutory);
				String filename = "my_payroll_report_" + month + ".xlsx";
				byte[] excelData = reportExportService.exportPayrollToExcel(report);
				return ResponseEntity.ok()
						.contentType(MediaType.APPLICATION_OCTET_STREAM)
						.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
						.body(excelData);
			} else if ("annual".equals(period) && year != null) {
				report = reportService.generateEmployeeAnnualPayrollReport(
						currentUser.userId(), year, includeStatutory);
				String filename = "my_payroll_report_" + year + ".xlsx";
				byte[] excelData = reportExportService.exportPayrollToExcel(report);
				return ResponseEntity.ok()
						.contentType(MediaType.APPLICATION_OCTET_STREAM)
						.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
						.body(excelData);
			} else {
				return ResponseEntity.badRequest().build();
			}
		} catch (Exception e) {
			return ResponseEntity.badRequest().build();
		}
	}

	@GetMapping("/reports/payroll/export/pdf")
	public ResponseEntity<byte[]> exportMyPayrollPDF(
			@RequestParam String period,
			@RequestParam(required = false) String month,
			@RequestParam(required = false) Integer year,
			@RequestParam(defaultValue = "true") boolean includeStatutory,
			HttpServletRequest request) {
		try {
			com.example.employeemanagement.security.AuthenticatedUser currentUser = getCurrentUser(request);
			PayrollReportResponse report;
			
			if ("monthly".equals(period) && month != null) {
				YearMonth yearMonth = YearMonth.parse(month);
				report = reportService.generateEmployeePayrollReport(
						currentUser.userId(), yearMonth, includeStatutory);
				String filename = "my_payroll_report_" + month + ".pdf";
				byte[] pdfData = reportExportService.exportPayrollToPDF(report);
				return ResponseEntity.ok()
						.contentType(MediaType.APPLICATION_PDF)
						.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
						.body(pdfData);
			} else if ("annual".equals(period) && year != null) {
				report = reportService.generateEmployeeAnnualPayrollReport(
						currentUser.userId(), year, includeStatutory);
				String filename = "my_payroll_report_" + year + ".pdf";
				byte[] pdfData = reportExportService.exportPayrollToPDF(report);
				return ResponseEntity.ok()
						.contentType(MediaType.APPLICATION_PDF)
						.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
						.body(pdfData);
			} else {
				return ResponseEntity.badRequest().build();
			}
		} catch (Exception e) {
			return ResponseEntity.badRequest().build();
		}
	}

	private String extractClientIp(HttpServletRequest request) {
		// Cloud Run and other load balancers may use different headers
		// Priority order:
		// 1. X-Forwarded-For (most common, may contain multiple IPs: "client, proxy1, proxy2")
		// 2. X-Real-IP (alternative header used by some proxies)
		// 3. CF-Connecting-IP (Cloudflare, if using Cloudflare in front)
		// 4. True-Client-IP (some CDNs)
		// 5. Remote address (fallback, but may be proxy IP in Cloud Run)
		
		// Check X-Forwarded-For header (for proxies/load balancers including Cloud Run)
		String forwarded = request.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) {
			// X-Forwarded-For may contain multiple IPs: "client-ip, proxy1-ip, proxy2-ip"
			// The first IP is usually the original client IP
			String ip = forwarded.split(",")[0].trim();
			org.slf4j.LoggerFactory.getLogger(getClass()).debug("Extracted IP from X-Forwarded-For: {}", ip);
			return ip;
		}
		
		// Check X-Real-IP header (alternative header)
		String realIp = request.getHeader("X-Real-IP");
		if (realIp != null && !realIp.isBlank()) {
			org.slf4j.LoggerFactory.getLogger(getClass()).debug("Extracted IP from X-Real-IP: {}", realIp);
			return realIp.trim();
		}
		
		// Check Cloudflare header (if using Cloudflare)
		String cfIp = request.getHeader("CF-Connecting-IP");
		if (cfIp != null && !cfIp.isBlank()) {
			org.slf4j.LoggerFactory.getLogger(getClass()).debug("Extracted IP from CF-Connecting-IP: {}", cfIp);
			return cfIp.trim();
		}
		
		// Check True-Client-IP header (some CDNs)
		String trueClientIp = request.getHeader("True-Client-IP");
		if (trueClientIp != null && !trueClientIp.isBlank()) {
			org.slf4j.LoggerFactory.getLogger(getClass()).debug("Extracted IP from True-Client-IP: {}", trueClientIp);
			return trueClientIp.trim();
		}
		
		// Fallback to remote address
		// Note: In Cloud Run, this will be the load balancer's IP, not the client IP
		String remoteAddr = request.getRemoteAddr();
		org.slf4j.LoggerFactory.getLogger(getClass()).debug("Using remote address (may be proxy IP): {}", remoteAddr);
		return remoteAddr;
	}
}

