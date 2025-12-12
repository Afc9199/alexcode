package com.example.employeemanagement.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.example.employeemanagement.dto.ChatContextItem;
import com.example.employeemanagement.dto.ChatMessageResponse;
import com.example.employeemanagement.service.EmployeeChatContextService.EmployeeContextSummary;
import com.example.employeemanagement.service.LocalAnswerGenerator.AnswerResult;

@Service
public class EmployeeChatbotService {

	private static final int MAX_PROMPT_LENGTH = 1200;

	private final EmployeeChatContextService contextService;
	private final LocalAnswerGenerator answerGenerator;

	public EmployeeChatbotService(EmployeeChatContextService contextService, LocalAnswerGenerator answerGenerator) {
		this.contextService = contextService;
		this.answerGenerator = answerGenerator;
	}

	public ChatMessageResponse answer(String userId, String rawMessage) {
		String message = normalizeMessage(rawMessage);
		EmployeeContextSummary summary = contextService.prepareContext(userId);

		Instant start = Instant.now();
		AnswerResult result = answerGenerator.generateAnswer(message, summary);
		long latency = Duration.between(start, Instant.now()).toMillis();

		List<ChatContextItem> contextItems = result.contextItems() != null && !result.contextItems().isEmpty()
				? result.contextItems()
				: (summary.items() != null ? summary.items() : List.of());
		
		return new ChatMessageResponse(result.answer(), contextItems, answerGenerator.getModel(), latency);
	}

	private String normalizeMessage(String rawMessage) {
		if (!StringUtils.hasText(rawMessage)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message cannot be empty");
		}
		String message = rawMessage.trim();
		if (message.length() > MAX_PROMPT_LENGTH) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Message is too long (max %d characters)".formatted(MAX_PROMPT_LENGTH));
		}
		return message;
	}
}

