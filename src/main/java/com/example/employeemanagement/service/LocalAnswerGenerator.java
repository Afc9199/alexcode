package com.example.employeemanagement.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.example.employeemanagement.dto.ChatContextItem;
import com.example.employeemanagement.service.EmployeeChatContextService.EmployeeContextSummary;

/**
 * Local answer generator that provides responses based on employee data
 * without requiring external AI APIs.
 */
@Service
public class LocalAnswerGenerator {

	private static final String MODEL_NAME = "Local Assistant";

	/**
	 * Generates an answer based on the user's question and employee context.
	 */
	public AnswerResult generateAnswer(String userQuestion, EmployeeContextSummary context) {
		String normalizedQuestion = userQuestion.toLowerCase().trim();
		
		// Extract relevant context items based on question keywords
		List<ChatContextItem> relevantContext = extractRelevantContext(normalizedQuestion, context);
		
		// Generate answer based on question type
		String answer = generateResponse(normalizedQuestion, context, relevantContext);
		
		return new AnswerResult(answer, relevantContext);
	}

	public String getModel() {
		return MODEL_NAME;
	}

	private List<ChatContextItem> extractRelevantContext(String question, EmployeeContextSummary context) {
		List<ChatContextItem> relevant = new ArrayList<>();
		List<ChatContextItem> allItems = context.items() != null ? context.items() : List.of();
		
		// Keywords for different context types
		if (containsKeywords(question, "salary", "pay", "payroll", "wage", "income", "earnings", "net pay")) {
			relevant.addAll(filterByType(allItems, "PAYROLL", "PROFILE"));
		}
		
		if (containsKeywords(question, "leave", "vacation", "holiday", "time off", "sick", "annual")) {
			relevant.addAll(filterByType(allItems, "LEAVE", "LEAVE_BALANCE"));
		}
		
		if (containsKeywords(question, "attendance", "check in", "check out", "present", "absent", "late")) {
			relevant.addAll(filterByType(allItems, "ATTENDANCE"));
		}
		
		if (containsKeywords(question, "kpi", "performance", "objective", "target", "goal", "bonus")) {
			relevant.addAll(filterByType(allItems, "KPI"));
		}
		
		if (containsKeywords(question, "benefit", "benefits", "allowance", "perk")) {
			relevant.addAll(filterByType(allItems, "BENEFITS"));
		}
		
		if (containsKeywords(question, "profile", "information", "details", "department", "title", "position")) {
			relevant.addAll(filterByType(allItems, "PROFILE"));
		}
		
		// If no specific context found, return all items
		if (relevant.isEmpty() && !allItems.isEmpty()) {
			relevant.addAll(allItems);
		}
		
		return relevant;
	}

	private List<ChatContextItem> filterByType(List<ChatContextItem> items, String... types) {
		List<ChatContextItem> filtered = new ArrayList<>();
		for (ChatContextItem item : items) {
			for (String type : types) {
				if (item.type().equals(type)) {
					filtered.add(item);
					break;
				}
			}
		}
		return filtered;
	}

	private boolean containsKeywords(String text, String... keywords) {
		for (String keyword : keywords) {
			if (text.contains(keyword)) {
				return true;
			}
		}
		return false;
	}

	private String generateResponse(String question, EmployeeContextSummary context, 
			List<ChatContextItem> relevantContext) {
		
		// Handle payroll questions
		if (containsKeywords(question, "salary", "pay", "payroll", "wage", "income", "earnings", "net pay")) {
			return generatePayrollAnswer(context, relevantContext);
		}
		
		// Handle leave questions
		if (containsKeywords(question, "leave", "vacation", "holiday", "time off", "sick", "annual")) {
			return generateLeaveAnswer(context, relevantContext, question);
		}
		
		// Handle attendance questions
		if (containsKeywords(question, "attendance", "check in", "check out", "present", "absent", "late")) {
			return generateAttendanceAnswer(context, relevantContext);
		}
		
		// Handle KPI questions
		if (containsKeywords(question, "kpi", "performance", "objective", "target", "goal", "bonus")) {
			return generateKPIAnswer(context, relevantContext);
		}
		
		// Handle benefits questions
		if (containsKeywords(question, "benefit", "benefits", "allowance", "perk")) {
			return generateBenefitsAnswer(context, relevantContext);
		}
		
		// Handle profile questions
		if (containsKeywords(question, "profile", "information", "details", "department", "title", "position")) {
			return generateProfileAnswer(context, relevantContext);
		}
		
		// Handle greeting questions
		if (containsKeywords(question, "hello", "hi", "hey", "greeting", "help")) {
			return generateGreetingAnswer(context);
		}
		
		// Default response
		return generateDefaultAnswer(context, relevantContext);
	}

	private String generatePayrollAnswer(EmployeeContextSummary context, List<ChatContextItem> relevantContext) {
		ChatContextItem payrollItem = findItemByType(relevantContext, "PAYROLL");
		ChatContextItem profileItem = findItemByType(relevantContext, "PROFILE");
		
		if (payrollItem != null) {
			return String.format(
				"Based on your records:\n\n%s\n\n%s\n\nIf you need more detailed payroll information, please check the Payroll section in your dashboard or contact HR for assistance.",
				payrollItem.details(),
				profileItem != null ? "Your basic salary: " + extractSalary(profileItem.details()) : ""
			);
		}
		
		if (profileItem != null) {
			String salary = extractSalary(profileItem.details());
			if (StringUtils.hasText(salary)) {
				return String.format(
					"Your basic salary is %s. For detailed payroll information including deductions and net pay, please check the Payroll section in your dashboard.",
					salary
				);
			}
		}
		
		return "I couldn't find payroll information in your records. Please check the Payroll section in your dashboard or contact HR for assistance.";
	}

	private String generateLeaveAnswer(EmployeeContextSummary context, List<ChatContextItem> relevantContext, 
			String question) {
		ChatContextItem leaveBalanceItem = findItemByType(relevantContext, "LEAVE_BALANCE");
		ChatContextItem leaveItem = findItemByType(relevantContext, "LEAVE");
		
		StringBuilder answer = new StringBuilder();
		
		if (leaveBalanceItem != null) {
			answer.append("Your leave balances:\n").append(leaveBalanceItem.details()).append("\n\n");
		}
		
		if (leaveItem != null) {
			answer.append("Recent leave requests:\n").append(leaveItem.details()).append("\n\n");
		}
		
		if (answer.length() > 0) {
			answer.append("To submit a new leave request, please go to the Leave section in your dashboard.");
		} else {
			answer.append("I couldn't find leave information in your records. To check your leave balance or submit a leave request, please go to the Leave section in your dashboard.");
		}
		
		return answer.toString();
	}

	private String generateAttendanceAnswer(EmployeeContextSummary context, List<ChatContextItem> relevantContext) {
		ChatContextItem attendanceItem = findItemByType(relevantContext, "ATTENDANCE");
		
		if (attendanceItem != null) {
			return String.format(
				"Your recent attendance records:\n\n%s\n\nTo check in or view more attendance details, please go to the Attendance section in your dashboard.",
				attendanceItem.details()
			);
		}
		
		return "I couldn't find attendance records. To check in or view your attendance, please go to the Attendance section in your dashboard.";
	}

	private String generateKPIAnswer(EmployeeContextSummary context, List<ChatContextItem> relevantContext) {
		ChatContextItem kpiItem = findItemByType(relevantContext, "KPI");
		
		if (kpiItem != null) {
			return String.format(
				"Your performance objectives (KPIs):\n\n%s\n\nTo update your KPI progress or view more details, please go to the Performance section in your dashboard.",
				kpiItem.details()
			);
		}
		
		return "I couldn't find any KPI assignments. To view or update your performance objectives, please go to the Performance section in your dashboard.";
	}

	private String generateBenefitsAnswer(EmployeeContextSummary context, List<ChatContextItem> relevantContext) {
		ChatContextItem benefitsItem = findItemByType(relevantContext, "BENEFITS");
		
		if (benefitsItem != null) {
			return String.format(
				"Your active benefits:\n\n%s\n\nFor more details about your benefits, please go to the Benefits section in your dashboard or contact HR.",
				benefitsItem.details()
			);
		}
		
		return "I couldn't find benefits information. To view your benefits, please go to the Benefits section in your dashboard or contact HR for assistance.";
	}

	private String generateProfileAnswer(EmployeeContextSummary context, List<ChatContextItem> relevantContext) {
		ChatContextItem profileItem = findItemByType(relevantContext, "PROFILE");
		
		if (profileItem != null) {
			return String.format(
				"Your profile information:\n\n%s\n\nTo update your profile, please go to the Profile section in your dashboard.",
				profileItem.details()
			);
		}
		
		return "I couldn't find your profile information. Please go to the Profile section in your dashboard to view or update your details.";
	}

	private String generateGreetingAnswer(EmployeeContextSummary context) {
		ChatContextItem profileItem = findItemByType(context.items(), "PROFILE");
		String name = profileItem != null ? extractName(profileItem.details()) : "there";
		
		return String.format(
			"Hello %s! I'm your employee assistant. I can help you with:\n\n" +
			"• Payroll and salary information\n" +
			"• Leave balances and requests\n" +
			"• Attendance records\n" +
			"• Performance objectives (KPIs)\n" +
			"• Benefits information\n" +
			"• Profile details\n\n" +
			"Just ask me a question about any of these topics, and I'll provide information based on your records.",
			name
		);
	}

	private String generateDefaultAnswer(EmployeeContextSummary context, List<ChatContextItem> relevantContext) {
		if (!relevantContext.isEmpty()) {
			StringBuilder answer = new StringBuilder("Based on your records:\n\n");
			for (ChatContextItem item : relevantContext) {
				answer.append("• ").append(item.label()).append(": ").append(item.details()).append("\n");
			}
			answer.append("\nIf you need more specific information, please ask about payroll, leave, attendance, KPIs, benefits, or your profile.");
			return answer.toString();
		}
		
		return "I'm here to help you with information about your payroll, leave, attendance, KPIs, benefits, and profile. " +
			   "Please ask a specific question, and I'll provide information based on your records. " +
			   "If I don't have the information, I'll guide you on where to find it in your dashboard.";
	}

	private ChatContextItem findItemByType(List<ChatContextItem> items, String type) {
		if (items == null) {
			return null;
		}
		for (ChatContextItem item : items) {
			if (item.type().equals(type)) {
				return item;
			}
		}
		return null;
	}

	private String extractSalary(String profileDetails) {
		if (profileDetails == null) {
			return "";
		}
		// Look for "Basic salary: RM X" pattern
		Pattern pattern = Pattern.compile("Basic salary:\\s*(RM[\\d,]+(?:\\.\\d{2})?)", Pattern.CASE_INSENSITIVE);
		java.util.regex.Matcher matcher = pattern.matcher(profileDetails);
		if (matcher.find()) {
			return matcher.group(1);
		}
		return "";
	}

	private String extractName(String profileDetails) {
		if (profileDetails == null) {
			return "there";
		}
		// Extract name before " (ID:" or first comma
		int idIndex = profileDetails.indexOf(" (ID:");
		int commaIndex = profileDetails.indexOf(",");
		int endIndex = idIndex > 0 ? idIndex : (commaIndex > 0 ? commaIndex : profileDetails.length());
		return profileDetails.substring(0, endIndex).trim();
	}

	public record AnswerResult(String answer, List<ChatContextItem> contextItems) {
	}
}
