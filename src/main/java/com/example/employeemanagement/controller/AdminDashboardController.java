package com.example.employeemanagement.controller;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.employeemanagement.dto.AdminAttendanceRequest;
import com.example.employeemanagement.dto.AnnouncementRequest;
import com.example.employeemanagement.dto.AnnouncementResponse;
import com.example.employeemanagement.dto.AttendanceResponse;
import com.example.employeemanagement.dto.AssignBenefitRequest;
import com.example.employeemanagement.dto.BenefitCategoryRequest;
import com.example.employeemanagement.dto.BenefitCategoryResponse;
import com.example.employeemanagement.dto.UpdateBenefitStatusRequest;
import com.example.employeemanagement.dto.EmployeeBenefitResponse;
import com.example.employeemanagement.dto.CompanyLeaveSettingsRequest;
import com.example.employeemanagement.dto.CompanyLeaveSettingsResponse;
import com.example.employeemanagement.dto.CompanySettingsRequest;
import com.example.employeemanagement.dto.CompanySettingsResponse;
import com.example.employeemanagement.dto.LeaveDecisionRequest;
import com.example.employeemanagement.dto.LeaveResponse;
import com.example.employeemanagement.dto.OvertimeResponse;
import com.example.employeemanagement.dto.OvertimeDecisionRequest;
import com.example.employeemanagement.dto.PayrollCalculationRequest;
import com.example.employeemanagement.dto.PayrollResponse;
import com.example.employeemanagement.dto.PayrollUpsertRequest;
import com.example.employeemanagement.dto.PayslipResponse;
import com.example.employeemanagement.dto.UserAccountResponse;
import com.example.employeemanagement.dto.UserAccountUpsertRequest;
import com.example.employeemanagement.dto.UpdateAttendanceStatusRequest;
import com.example.employeemanagement.model.AnnouncementAudience;
import com.example.employeemanagement.service.AnnouncementService;
import com.example.employeemanagement.service.AttendanceSchedulerService;
import com.example.employeemanagement.service.AttendanceService;
import com.example.employeemanagement.dto.LeaveBalanceResponse;
import com.example.employeemanagement.service.BenefitCategoryService;
import com.example.employeemanagement.service.EmployeeBenefitService;
import com.example.employeemanagement.service.CompanyLeaveSettingsService;
import com.example.employeemanagement.service.CompanySettingsService;
import com.example.employeemanagement.service.LeaveBalanceService;
import com.example.employeemanagement.service.LeaveService;
import com.example.employeemanagement.service.OvertimeService;
import com.example.employeemanagement.service.PayrollService;
import com.example.employeemanagement.service.AdminUserService;
import com.example.employeemanagement.service.DepartmentService;
import com.example.employeemanagement.dto.DepartmentRequest;
import com.example.employeemanagement.dto.DepartmentResponse;
import com.example.employeemanagement.service.KPIService;
import com.example.employeemanagement.dto.KPIRequest;
import com.example.employeemanagement.dto.KPIResponse;
import com.example.employeemanagement.service.EmployeeKPIService;
import com.example.employeemanagement.dto.AssignKPIRequest;
import com.example.employeemanagement.dto.EmployeeKPIResponse;
import com.example.employeemanagement.dto.UpdateEmployeeKPIStatusRequest;
import com.example.employeemanagement.dto.UpdateKPIStatusRequest;
import com.example.employeemanagement.dto.JobPostingRequest;
import com.example.employeemanagement.dto.JobPostingResponse;
import com.example.employeemanagement.dto.ResumeRequest;
import com.example.employeemanagement.dto.ResumeResponse;
import com.example.employeemanagement.dto.DashboardSummaryResponse;
import com.example.employeemanagement.service.JobPostingService;
import com.example.employeemanagement.service.ResumeService;
import com.example.employeemanagement.service.ReportService;
import com.example.employeemanagement.service.ReportExportService;
import com.example.employeemanagement.dto.AttendanceReportResponse;
import com.example.employeemanagement.dto.PayrollReportResponse;
import com.example.employeemanagement.dto.BenefitsReportResponse;
import com.example.employeemanagement.dto.KPIPerformanceReportResponse;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.YearMonth;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin")
@Validated
public class AdminDashboardController {

	private final AdminUserService adminUserService;
	private final AttendanceService attendanceService;
	private final LeaveService leaveService;
	private final OvertimeService overtimeService;
	private final PayrollService payrollService;
	private final AnnouncementService announcementService;
	private final CompanySettingsService companySettingsService;
	private final CompanyLeaveSettingsService companyLeaveSettingsService;
	private final AttendanceSchedulerService attendanceSchedulerService;
	private final LeaveBalanceService leaveBalanceService;
	private final BenefitCategoryService benefitCategoryService;
	private final EmployeeBenefitService employeeBenefitService;
	private final DepartmentService departmentService;
	private final KPIService kpiService;
	private final EmployeeKPIService employeeKPIService;
	private final JobPostingService jobPostingService;
	private final ResumeService resumeService;
	private final com.example.employeemanagement.service.DashboardSummaryService dashboardSummaryService;
	private final ReportService reportService;
	private final ReportExportService reportExportService;

	public AdminDashboardController(AdminUserService adminUserService, AttendanceService attendanceService,
			LeaveService leaveService, OvertimeService overtimeService, PayrollService payrollService, AnnouncementService announcementService,
			CompanySettingsService companySettingsService, CompanyLeaveSettingsService companyLeaveSettingsService,
			AttendanceSchedulerService attendanceSchedulerService, LeaveBalanceService leaveBalanceService,
			BenefitCategoryService benefitCategoryService, EmployeeBenefitService employeeBenefitService,
			DepartmentService departmentService, KPIService kpiService, EmployeeKPIService employeeKPIService,
			JobPostingService jobPostingService, ResumeService resumeService,
			com.example.employeemanagement.service.DashboardSummaryService dashboardSummaryService,
			ReportService reportService, ReportExportService reportExportService) {
		this.adminUserService = adminUserService;
		this.attendanceService = attendanceService;
		this.leaveService = leaveService;
		this.overtimeService = overtimeService;
		this.payrollService = payrollService;
		this.announcementService = announcementService;
		this.companySettingsService = companySettingsService;
		this.companyLeaveSettingsService = companyLeaveSettingsService;
		this.attendanceSchedulerService = attendanceSchedulerService;
		this.leaveBalanceService = leaveBalanceService;
		this.benefitCategoryService = benefitCategoryService;
		this.employeeBenefitService = employeeBenefitService;
		this.departmentService = departmentService;
		this.kpiService = kpiService;
		this.employeeKPIService = employeeKPIService;
		this.jobPostingService = jobPostingService;
		this.resumeService = resumeService;
		this.dashboardSummaryService = dashboardSummaryService;
		this.reportService = reportService;
		this.reportExportService = reportExportService;
	}

	@GetMapping("/users")
	public List<UserAccountResponse> listUsers() {
		return adminUserService.listUsers();
	}

	@GetMapping("/next-employee-id")
	public ResponseEntity<String> getNextEmployeeId() {
		return ResponseEntity.ok(adminUserService.getNextEmployeeId());
	}

	@GetMapping("/users/{userId}")
	public UserAccountResponse getUser(@PathVariable String userId) {
		return adminUserService.getUser(userId);
	}

	@GetMapping("/users/by-employee-id/{employeeId}")
	public UserAccountResponse getUserByEmployeeId(@PathVariable String employeeId) {
		return adminUserService.getUserByEmployeeId(employeeId);
	}

	@GetMapping("/users/{userId}/password-default")
	public ResponseEntity<Boolean> isPasswordDefault(@PathVariable String userId) {
		return ResponseEntity.ok(adminUserService.isPasswordDefault(userId));
	}

	@PostMapping("/users")
	public ResponseEntity<UserAccountResponse> createUser(@Valid @RequestBody UserAccountUpsertRequest request) {
		return ResponseEntity.ok(adminUserService.createUser(request));
	}

	@PutMapping("/users/{userId}")
	public UserAccountResponse updateUser(@PathVariable String userId,
			@Valid @RequestBody UserAccountUpsertRequest request) {
		return adminUserService.updateUser(userId, request);
	}

	@DeleteMapping("/users/{userId}")
	public ResponseEntity<Void> deleteUser(@PathVariable String userId) {
		adminUserService.deleteUser(userId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/attendance")
	public List<AttendanceResponse> allAttendance() {
		return attendanceService.findAll();
	}

	@GetMapping("/attendance/next-attendance-id")
	public ResponseEntity<String> getNextAttendanceId() {
		return ResponseEntity.ok(attendanceService.getNextAttendanceId());
	}

	@GetMapping("/attendance/{attendanceId}")
	public AttendanceResponse getAttendance(@PathVariable String attendanceId) {
		return attendanceService.getAttendanceById(attendanceId);
	}

	@PostMapping("/attendance")
	public ResponseEntity<AttendanceResponse> createAttendance(@Valid @RequestBody AdminAttendanceRequest request) {
		return ResponseEntity.ok(attendanceService.adminCreateAttendance(request));
	}

	@PutMapping("/attendance/{attendanceId}")
	public AttendanceResponse updateAttendance(@PathVariable String attendanceId,
			@Valid @RequestBody AdminAttendanceRequest request) {
		return attendanceService.adminUpdateAttendance(attendanceId, request);
	}

	@DeleteMapping("/attendance/{attendanceId}")
	public ResponseEntity<Void> deleteAttendance(@PathVariable String attendanceId) {
		attendanceService.deleteAttendance(attendanceId);
		return ResponseEntity.noContent().build();
	}

	@PutMapping("/attendance/{attendanceId}/status")
	public AttendanceResponse updateAttendanceStatus(@PathVariable String attendanceId,
			@Valid @RequestBody UpdateAttendanceStatusRequest request) {
		return attendanceService.updateStatus(attendanceId, request);
	}

	@GetMapping("/leave")
	public List<LeaveResponse> allLeave() {
		return leaveService.findAll();
	}

	@GetMapping("/leave/next-leave-id")
	public String getNextLeaveId() {
		return leaveService.getNextLeaveId();
	}

	@PutMapping("/leave/{leaveId}/decision")
	public LeaveResponse decideLeave(@PathVariable String leaveId, @Valid @RequestBody LeaveDecisionRequest request) {
		return leaveService.decideLeave(leaveId, request);
	}

	@DeleteMapping("/leave/{leaveId}")
	public ResponseEntity<Void> deleteLeaveRequest(@PathVariable String leaveId) {
		leaveService.deleteLeaveRequest(leaveId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/leave-balance/{userId}")
	public LeaveBalanceResponse getEmployeeLeaveBalance(@PathVariable String userId) {
		return leaveBalanceService.getLeaveBalances(userId);
	}

	@GetMapping("/leave-balance/search/{searchTerm}")
	public LeaveBalanceResponse getEmployeeLeaveBalanceBySearch(@PathVariable String searchTerm) {
		return leaveBalanceService.getLeaveBalancesBySearch(searchTerm);
	}

	@GetMapping("/overtime")
	public List<OvertimeResponse> allOvertime() {
		return overtimeService.findAll();
	}

	@GetMapping("/overtime/next-overtime-id")
	public String getNextOvertimeId() {
		return overtimeService.getNextOvertimeId();
	}

	@PutMapping("/overtime/{overtimeId}/decision")
	public OvertimeResponse decideOvertime(@PathVariable String overtimeId, @Valid @RequestBody OvertimeDecisionRequest request) {
		return overtimeService.decideOvertime(overtimeId, request);
	}

	@DeleteMapping("/overtime/{overtimeId}")
	public ResponseEntity<Void> deleteOvertimeRequest(@PathVariable String overtimeId) {
		overtimeService.deleteOvertimeRequest(overtimeId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/leave-settings")
	public CompanyLeaveSettingsResponse getLeaveSettings() {
		return companyLeaveSettingsService.getSettings();
	}

	@PutMapping("/leave-settings")
	public CompanyLeaveSettingsResponse updateLeaveSettings(@Valid @RequestBody CompanyLeaveSettingsRequest request) {
		return companyLeaveSettingsService.updateSettings(request);
	}

	@GetMapping("/payroll")
	public List<PayrollResponse> allPayroll() {
		return payrollService.findAll();
	}

	@PostMapping("/payroll")
	public PayrollResponse upsertPayroll(@Valid @RequestBody PayrollUpsertRequest request) {
		return payrollService.upsertPayroll(request);
	}

	@PostMapping("/payroll/calculate")
	public ResponseEntity<?> calculatePayroll(@Valid @RequestBody PayrollCalculationRequest request) {
		try {
			PayslipResponse response = payrollService.calculateAndGeneratePayroll(request.userId(), request.month());
			return ResponseEntity.ok(response);
		} catch (org.springframework.web.server.ResponseStatusException e) {
			return ResponseEntity.status(e.getStatusCode())
					.body(java.util.Map.of("error", e.getReason(), "message", e.getReason()));
		} catch (Exception e) {
			return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
					.body(java.util.Map.of("error", "Failed to calculate payroll", "message", e.getMessage()));
		}
	}

	@GetMapping("/payroll/payslip/{userId}/{year}/{month}")
	public ResponseEntity<?> getPayslip(@PathVariable String userId, @PathVariable int year, @PathVariable int month) {
		try {
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

	@GetMapping("/payroll/{payrollId}")
	public ResponseEntity<?> getPayrollById(@PathVariable String payrollId) {
		try {
			PayrollResponse response = payrollService.findById(payrollId);
			return ResponseEntity.ok(response);
		} catch (org.springframework.web.server.ResponseStatusException e) {
			return ResponseEntity.status(e.getStatusCode())
					.body(java.util.Map.of("error", e.getReason(), "message", e.getReason()));
		} catch (Exception e) {
			return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
					.body(java.util.Map.of("error", "Failed to load payroll", "message", e.getMessage()));
		}
	}

	@DeleteMapping("/payroll/{payrollId}")
	public ResponseEntity<?> deletePayroll(@PathVariable String payrollId) {
		try {
			payrollService.deletePayroll(payrollId);
			return ResponseEntity.noContent().build();
		} catch (org.springframework.web.server.ResponseStatusException e) {
			return ResponseEntity.status(e.getStatusCode())
					.body(java.util.Map.of("error", e.getReason(), "message", e.getReason()));
		} catch (Exception e) {
			return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
					.body(java.util.Map.of("error", "Failed to delete payroll", "message", e.getMessage()));
		}
	}

	@GetMapping("/announcements")
	public List<AnnouncementResponse> allAnnouncements() {
		return announcementService.listAll();
	}

	@PostMapping("/announcements")
	public ResponseEntity<AnnouncementResponse> createAnnouncement(@Valid @RequestBody AnnouncementRequest request) {
		return ResponseEntity.ok(announcementService.upsertAnnouncement(request));
	}

	@GetMapping("/announcements/employee")
	public List<AnnouncementResponse> employeeAnnouncements() {
		return announcementService.listForAudience(AnnouncementAudience.EMPLOYEE);
	}

	@GetMapping("/announcements/admin")
	public List<AnnouncementResponse> adminAnnouncements() {
		return announcementService.listForAudience(AnnouncementAudience.ADMIN);
	}

	@DeleteMapping("/announcements/{announcementId}")
	public ResponseEntity<Void> deleteAnnouncement(@PathVariable String announcementId) {
		announcementService.deleteAnnouncement(announcementId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/settings")
	public CompanySettingsResponse getSettings() {
		return companySettingsService.getSettings();
	}

	@PutMapping("/settings")
	public CompanySettingsResponse updateSettings(@Valid @RequestBody CompanySettingsRequest request) {
		return companySettingsService.updateSettings(request);
	}

	@PostMapping("/attendance/mark-absent-today")
	public ResponseEntity<String> markAbsentToday() {
		java.time.LocalDate today = java.time.LocalDate.now();
		attendanceSchedulerService.markAbsentForDate(today);
		return ResponseEntity.ok("Absent marking completed for " + today);
	}

	@GetMapping("/benefit-categories")
	public List<BenefitCategoryResponse> getAllBenefitCategories(
			@org.springframework.web.bind.annotation.RequestParam(required = false) String search) {
		if (search != null && !search.trim().isEmpty()) {
			return benefitCategoryService.search(search.trim());
		}
		return benefitCategoryService.findAll();
	}

	@GetMapping("/benefit-categories/{id}")
	public BenefitCategoryResponse getBenefitCategory(@PathVariable String id) {
		return benefitCategoryService.findById(id);
	}

	@GetMapping("/benefit-categories/by-benefit-id/{benefitId}")
	public BenefitCategoryResponse getBenefitCategoryByBenefitId(@PathVariable String benefitId) {
		return benefitCategoryService.findByBenefitId(benefitId);
	}

	@GetMapping("/benefit-categories/next-benefit-id")
	public ResponseEntity<String> getNextBenefitId() {
		return ResponseEntity.ok(benefitCategoryService.getNextBenefitId());
	}

	@PostMapping("/benefit-categories")
	public ResponseEntity<BenefitCategoryResponse> createBenefitCategory(
			@Valid @RequestBody BenefitCategoryRequest request) {
		return ResponseEntity.ok(benefitCategoryService.create(request));
	}

	@PutMapping("/benefit-categories/{id}")
	public BenefitCategoryResponse updateBenefitCategory(@PathVariable String id,
			@Valid @RequestBody BenefitCategoryRequest request) {
		return benefitCategoryService.update(id, request);
	}

	@DeleteMapping("/benefit-categories/{id}")
	public ResponseEntity<Void> deleteBenefitCategory(@PathVariable String id) {
		benefitCategoryService.delete(id);
		return ResponseEntity.noContent().build();
	}

	@PatchMapping("/benefit-categories/{id}/status")
	public BenefitCategoryResponse updateBenefitCategoryStatus(@PathVariable String id,
			@Valid @RequestBody UpdateBenefitStatusRequest request) {
		return benefitCategoryService.updateStatus(id, request.active());
	}

	// Employee Benefit Assignment Endpoints
	@PostMapping("/employee-benefits/assign")
	public ResponseEntity<EmployeeBenefitResponse> assignBenefit(
			@Valid @RequestBody AssignBenefitRequest request) {
		return ResponseEntity.ok(employeeBenefitService.assignBenefit(request));
	}

	@DeleteMapping("/employee-benefits/{id}")
	public ResponseEntity<Void> unassignBenefit(@PathVariable String id) {
		employeeBenefitService.unassignBenefit(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/employee-benefits")
	public List<EmployeeBenefitResponse> getAllAssignments() {
		return employeeBenefitService.getAllAssignments();
	}

	@GetMapping("/employee-benefits/employee/{employeeId}")
	public List<EmployeeBenefitResponse> getBenefitsByEmployeeId(@PathVariable String employeeId) {
		return employeeBenefitService.getBenefitsByEmployeeId(employeeId);
	}

	@GetMapping("/employee-benefits/benefit-category/{benefitCategoryId}")
	public List<EmployeeBenefitResponse> getAssignmentsByBenefitCategoryId(@PathVariable String benefitCategoryId) {
		return employeeBenefitService.getAssignmentsByBenefitCategoryId(benefitCategoryId);
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

	// Department Management Endpoints
	@GetMapping("/departments")
	public List<DepartmentResponse> getAllDepartments() {
		return departmentService.findAll();
	}

	@GetMapping("/departments/search")
	public List<DepartmentResponse> searchDepartments(@org.springframework.web.bind.annotation.RequestParam(required = false) String search) {
		return departmentService.search(search != null ? search : "");
	}

	@GetMapping("/departments/{id}")
	public DepartmentResponse getDepartment(@PathVariable String id) {
		return departmentService.findById(id);
	}

	@PostMapping("/departments")
	public ResponseEntity<DepartmentResponse> createDepartment(@Valid @RequestBody DepartmentRequest request) {
		return ResponseEntity.ok(departmentService.create(request));
	}

	@PutMapping("/departments/{id}")
	public DepartmentResponse updateDepartment(@PathVariable String id, @Valid @RequestBody DepartmentRequest request) {
		return departmentService.update(id, request);
	}

	@DeleteMapping("/departments/{id}")
	public ResponseEntity<Void> deleteDepartment(@PathVariable String id) {
		departmentService.delete(id);
		return ResponseEntity.noContent().build();
	}

	// KPI Management Endpoints
	@GetMapping("/kpis")
	public List<KPIResponse> getAllKPIs() {
		return kpiService.findAll();
	}

	@GetMapping("/kpis/search")
	public List<KPIResponse> searchKPIs(@org.springframework.web.bind.annotation.RequestParam(required = false) String search) {
		return kpiService.search(search != null ? search : "");
	}

	@GetMapping("/kpis/next-kpi-id")
	public String getNextKpiId() {
		return kpiService.getNextKpiId();
	}

	@GetMapping("/kpis/{id}")
	public KPIResponse getKPI(@PathVariable String id) {
		return kpiService.findById(id);
	}

	@PostMapping("/kpis")
	public ResponseEntity<KPIResponse> createKPI(@Valid @RequestBody KPIRequest request) {
		return ResponseEntity.ok(kpiService.create(request));
	}

	@PutMapping("/kpis/{id}")
	public KPIResponse updateKPI(@PathVariable String id, @Valid @RequestBody KPIRequest request) {
		return kpiService.update(id, request);
	}

	@PatchMapping("/kpis/{id}/status")
	public KPIResponse updateKpiStatus(@PathVariable String id,
			@Valid @RequestBody UpdateKPIStatusRequest request) {
		return kpiService.updateStatus(id, request.active());
	}

	@DeleteMapping("/kpis/{id}")
	public ResponseEntity<Void> deleteKPI(@PathVariable String id) {
		kpiService.delete(id);
		return ResponseEntity.noContent().build();
	}

	// KPI Assignment Endpoints
	@PostMapping("/kpi-assignments")
	public ResponseEntity<EmployeeKPIResponse> assignKPI(@Valid @RequestBody AssignKPIRequest request) {
		return ResponseEntity.ok(employeeKPIService.assignKPI(request));
	}

	@DeleteMapping("/kpi-assignments/{id}")
	public ResponseEntity<Void> unassignKPI(@PathVariable String id) {
		employeeKPIService.unassignKPI(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/kpi-assignments")
	public List<EmployeeKPIResponse> getAllKPIAssignments() {
		return employeeKPIService.getAllAssignments();
	}

	@GetMapping("/kpi-assignments/employee/{employeeId}")
	public List<EmployeeKPIResponse> getKPIsByEmployeeId(@PathVariable String employeeId) {
		return employeeKPIService.getKPIsByEmployeeId(employeeId);
	}

	@GetMapping("/kpi-assignments/kpi-category/{kpiCategoryId}")
	public List<EmployeeKPIResponse> getAssignmentsByKpiCategoryId(@PathVariable String kpiCategoryId) {
		return employeeKPIService.getAssignmentsByKpiCategoryId(kpiCategoryId);
	}

	@GetMapping("/kpi-assignments/kpi-id/{kpiId}")
	public List<EmployeeKPIResponse> getAssignmentsByKpiId(@PathVariable String kpiId) {
		return employeeKPIService.getAssignmentsByKpiId(kpiId);
	}

	@PatchMapping("/kpi-assignments/{id}/status")
	public EmployeeKPIResponse updateKpiAssignmentStatus(@PathVariable String id,
			@Valid @RequestBody UpdateEmployeeKPIStatusRequest request) {
		return employeeKPIService.updateAssignmentStatus(id, request);
	}

	// Job Posting Management Endpoints
	@GetMapping("/job-postings")
	public List<JobPostingResponse> getAllJobPostings() {
		return jobPostingService.findAll();
	}

	@GetMapping("/job-postings/search")
	public List<JobPostingResponse> searchJobPostings(
			@org.springframework.web.bind.annotation.RequestParam(required = false) String search,
			@org.springframework.web.bind.annotation.RequestParam(required = false) String status) {
		return jobPostingService.search(search, status);
	}

	@GetMapping("/job-postings/next-job-posting-id")
	public ResponseEntity<String> getNextJobPostingId() {
		return ResponseEntity.ok(jobPostingService.getNextJobPostingId());
	}

	@GetMapping("/job-postings/{id}")
	public JobPostingResponse getJobPosting(@PathVariable String id) {
		return jobPostingService.findById(id);
	}

	@PostMapping("/job-postings")
	public ResponseEntity<JobPostingResponse> createJobPosting(@Valid @RequestBody JobPostingRequest request,
			@RequestParam("createdBy") String createdBy) {
		return ResponseEntity.ok(jobPostingService.create(request, createdBy));
	}

	@PutMapping("/job-postings/{id}")
	public JobPostingResponse updateJobPosting(@PathVariable String id, @Valid @RequestBody JobPostingRequest request) {
		return jobPostingService.update(id, request);
	}

	@DeleteMapping("/job-postings/{id}")
	public ResponseEntity<Void> deleteJobPosting(@PathVariable String id) {
		jobPostingService.delete(id);
		return ResponseEntity.noContent().build();
	}

	// Resume Management Endpoints
	@GetMapping("/resumes")
	public List<ResumeResponse> getAllResumes() {
		return resumeService.findAll();
	}

	@GetMapping("/resumes/job-posting/{jobPostingId}")
	public List<ResumeResponse> getResumesByJobPostingId(@PathVariable String jobPostingId) {
		return resumeService.findByJobPostingId(jobPostingId);
	}

	@GetMapping("/resumes/next-resume-id")
	public ResponseEntity<String> getNextResumeId() {
		return ResponseEntity.ok(resumeService.getNextResumeId());
	}

	@GetMapping("/resumes/{id}")
	public ResumeResponse getResume(@PathVariable String id) {
		return resumeService.findById(id);
	}

	@PostMapping(value = "/resumes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ResumeResponse> createResume(
			@RequestParam("jobPostingId") String jobPostingId,
			@RequestParam("candidateName") String candidateName,
			@RequestParam("candidateEmail") String candidateEmail,
			@RequestParam("candidateContactNumber") String candidateContactNumber,
			@RequestPart("resumeFile") MultipartFile resumeFile) {
		ResumeRequest request = new ResumeRequest(jobPostingId, candidateName, candidateEmail, candidateContactNumber);
		return ResponseEntity.ok(resumeService.create(request, resumeFile));
	}

	@PutMapping(value = "/resumes/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ResumeResponse> updateResume(
			@PathVariable String id,
			@RequestParam("jobPostingId") String jobPostingId,
			@RequestParam("candidateName") String candidateName,
			@RequestParam("candidateEmail") String candidateEmail,
			@RequestParam("candidateContactNumber") String candidateContactNumber,
			@RequestPart(value = "resumeFile", required = false) MultipartFile resumeFile) {
		ResumeRequest request = new ResumeRequest(jobPostingId, candidateName, candidateEmail, candidateContactNumber);
		return ResponseEntity.ok(resumeService.update(id, request, resumeFile));
	}

	@DeleteMapping("/resumes/{id}")
	public ResponseEntity<Void> deleteResume(@PathVariable String id) {
		resumeService.delete(id);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/resumes/resume-id/{resumeId}")
	public ResponseEntity<Void> deleteResumeByResumeId(@PathVariable String resumeId) {
		resumeService.deleteByResumeId(resumeId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/dashboard/summary")
	public DashboardSummaryResponse getDashboardSummary() {
		return dashboardSummaryService.getAdminSummary();
	}

	@GetMapping("/resumes/file/{filename}")
	public ResponseEntity<Resource> getResumeFile(
			@PathVariable String filename,
			@RequestParam(name = "download", required = false, defaultValue = "false") boolean download) {
		try {
			Path filePath = Paths.get(System.getProperty("user.dir"), "uploads", "resumes", filename).normalize();
			Path resumesDir = Paths.get(System.getProperty("user.dir"), "uploads", "resumes").normalize();
			if (!filePath.startsWith(resumesDir)) {
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

				String originalName = resumeService.resolveOriginalFilename(filename);
				if (originalName == null) {
					originalName = filename;
				}
				originalName = originalName.replace("\"", "");
				String dispositionType = download ? "attachment" : "inline";

				return ResponseEntity.ok()
						.contentType(MediaType.parseMediaType(contentType))
						.header(HttpHeaders.CONTENT_DISPOSITION, dispositionType + "; filename=\"" + originalName + "\"")
						.body(resource);
			}
			return ResponseEntity.notFound().build();
		} catch (Exception e) {
			return ResponseEntity.notFound().build();
		}
	}

	// Report Generation Endpoints
	@GetMapping("/reports/attendance")
	public ResponseEntity<AttendanceReportResponse> generateAttendanceReport(
			@RequestParam String type,
			@RequestParam String startDate,
			@RequestParam String endDate,
			@RequestParam(required = false) String department) {
		try {
			LocalDate start = LocalDate.parse(startDate);
			LocalDate end = LocalDate.parse(endDate);
			AttendanceReportResponse report = reportService.generateAttendanceReport(type, start, end, department);
			return ResponseEntity.ok(report);
		} catch (Exception e) {
			return ResponseEntity.badRequest().build();
		}
	}

	@GetMapping("/reports/payroll")
	public ResponseEntity<PayrollReportResponse> generatePayrollReport(
			@RequestParam String type,
			@RequestParam String month,
			@RequestParam(required = false) String employeeId,
			@RequestParam(required = false) String department,
			@RequestParam(defaultValue = "true") boolean includeStatutory) {
		try {
			YearMonth yearMonth = YearMonth.parse(month);
			PayrollReportResponse report = reportService.generatePayrollReport(type, yearMonth, employeeId, department, includeStatutory);
			return ResponseEntity.ok(report);
		} catch (Exception e) {
			return ResponseEntity.badRequest().build();
		}
	}

	@GetMapping("/reports/benefits")
	public ResponseEntity<?> generateBenefitsReport(
			@RequestParam String type,
			@RequestParam(required = false) String benefitCategoryId,
			@RequestParam(required = false) String department,
			@RequestParam(required = false) String year) {
		try {
			if ("benefits".equals(type)) {
				BenefitsReportResponse report = reportService.generateBenefitsReport(benefitCategoryId, department);
				return ResponseEntity.ok(report);
			} else if ("performance".equals(type)) {
				int yearInt = year != null ? Integer.parseInt(year) : java.time.Year.now().getValue();
				KPIPerformanceReportResponse report = reportService.generateKPIPerformanceReport(department, yearInt);
				return ResponseEntity.ok(report);
			} else {
				return ResponseEntity.badRequest().build();
			}
		} catch (Exception e) {
			return ResponseEntity.badRequest().build();
		}
	}

	// Report Export Endpoints
	@GetMapping("/reports/attendance/export/excel")
	public ResponseEntity<byte[]> exportAttendanceExcel(
			@RequestParam String type,
			@RequestParam String startDate,
			@RequestParam String endDate,
			@RequestParam(required = false) String department) {
		try {
			LocalDate start = LocalDate.parse(startDate);
			LocalDate end = LocalDate.parse(endDate);
			AttendanceReportResponse report = reportService.generateAttendanceReport(type, start, end, department);
			byte[] excelData = reportExportService.exportAttendanceToExcel(report);
			
			String filename = "attendance_report_" + startDate + "_to_" + endDate + ".xlsx";
			return ResponseEntity.ok()
					.contentType(MediaType.APPLICATION_OCTET_STREAM)
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
					.body(excelData);
		} catch (Exception e) {
			return ResponseEntity.badRequest().build();
		}
	}

	@GetMapping("/reports/attendance/export/pdf")
	public ResponseEntity<byte[]> exportAttendancePDF(
			@RequestParam String type,
			@RequestParam String startDate,
			@RequestParam String endDate,
			@RequestParam(required = false) String department) {
		try {
			LocalDate start = LocalDate.parse(startDate);
			LocalDate end = LocalDate.parse(endDate);
			AttendanceReportResponse report = reportService.generateAttendanceReport(type, start, end, department);
			byte[] pdfData = reportExportService.exportAttendanceToPDF(report);
			
			String filename = "attendance_report_" + startDate + "_to_" + endDate + ".pdf";
			return ResponseEntity.ok()
					.contentType(MediaType.APPLICATION_PDF)
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
					.body(pdfData);
		} catch (Exception e) {
			return ResponseEntity.badRequest().build();
		}
	}

	@GetMapping("/reports/payroll/export/excel")
	public ResponseEntity<byte[]> exportPayrollExcel(
			@RequestParam String type,
			@RequestParam String month,
			@RequestParam(required = false) String employeeId,
			@RequestParam(required = false) String department,
			@RequestParam(defaultValue = "true") boolean includeStatutory) {
		try {
			YearMonth yearMonth = YearMonth.parse(month);
			PayrollReportResponse report = reportService.generatePayrollReport(type, yearMonth, employeeId, department, includeStatutory);
			byte[] excelData = reportExportService.exportPayrollToExcel(report);
			
			String filename = "payroll_report_" + month + ".xlsx";
			return ResponseEntity.ok()
					.contentType(MediaType.APPLICATION_OCTET_STREAM)
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
					.body(excelData);
		} catch (Exception e) {
			return ResponseEntity.badRequest().build();
		}
	}

	@GetMapping("/reports/payroll/export/pdf")
	public ResponseEntity<byte[]> exportPayrollPDF(
			@RequestParam String type,
			@RequestParam String month,
			@RequestParam(required = false) String employeeId,
			@RequestParam(required = false) String department,
			@RequestParam(defaultValue = "true") boolean includeStatutory) {
		try {
			YearMonth yearMonth = YearMonth.parse(month);
			PayrollReportResponse report = reportService.generatePayrollReport(type, yearMonth, employeeId, department, includeStatutory);
			byte[] pdfData = reportExportService.exportPayrollToPDF(report);
			
			String filename = "payroll_report_" + month + ".pdf";
			return ResponseEntity.ok()
					.contentType(MediaType.APPLICATION_PDF)
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
					.body(pdfData);
		} catch (Exception e) {
			return ResponseEntity.badRequest().build();
		}
	}

	@GetMapping("/reports/benefits/export/excel")
	public ResponseEntity<byte[]> exportBenefitsExcel(
			@RequestParam String type,
			@RequestParam(required = false) String benefitCategoryId,
			@RequestParam(required = false) String department,
			@RequestParam(required = false) String year) {
		try {
			if ("benefits".equals(type)) {
				BenefitsReportResponse report = reportService.generateBenefitsReport(benefitCategoryId, department);
				byte[] excelData = reportExportService.exportBenefitsToExcel(report);
				
				String filename = "benefits_report.xlsx";
				return ResponseEntity.ok()
						.contentType(MediaType.APPLICATION_OCTET_STREAM)
						.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
						.body(excelData);
			} else if ("performance".equals(type)) {
				int yearInt = year != null ? Integer.parseInt(year) : java.time.Year.now().getValue();
				KPIPerformanceReportResponse report = reportService.generateKPIPerformanceReport(department, yearInt);
				byte[] excelData = reportExportService.exportKPIPerformanceToExcel(report);
				
				String filename = "kpi_performance_report_" + year + ".xlsx";
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

	@GetMapping("/reports/benefits/export/pdf")
	public ResponseEntity<byte[]> exportBenefitsPDF(
			@RequestParam String type,
			@RequestParam(required = false) String benefitCategoryId,
			@RequestParam(required = false) String department,
			@RequestParam(required = false) String year) {
		try {
			if ("benefits".equals(type)) {
				BenefitsReportResponse report = reportService.generateBenefitsReport(benefitCategoryId, department);
				byte[] pdfData = reportExportService.exportBenefitsToPDF(report);
				
				String filename = "benefits_report.pdf";
				return ResponseEntity.ok()
						.contentType(MediaType.APPLICATION_PDF)
						.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
						.body(pdfData);
			} else if ("performance".equals(type)) {
				int yearInt = year != null ? Integer.parseInt(year) : java.time.Year.now().getValue();
				KPIPerformanceReportResponse report = reportService.generateKPIPerformanceReport(department, yearInt);
				byte[] pdfData = reportExportService.exportKPIPerformanceToPDF(report);
				
				String filename = "kpi_performance_report_" + year + ".pdf";
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
}

