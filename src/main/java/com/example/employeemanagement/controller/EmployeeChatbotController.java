package com.example.employeemanagement.controller;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.employeemanagement.dto.ChatMessageRequest;
import com.example.employeemanagement.dto.ChatMessageResponse;
import com.example.employeemanagement.model.Role;
import com.example.employeemanagement.security.AuthenticatedUser;
import com.example.employeemanagement.security.SessionAttributes;
import com.example.employeemanagement.service.EmployeeChatbotService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/employee/chatbot")
@Validated
public class EmployeeChatbotController {

	private final EmployeeChatbotService chatbotService;

	public EmployeeChatbotController(EmployeeChatbotService chatbotService) {
		this.chatbotService = chatbotService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.OK)
	public ChatMessageResponse chat(@RequestAttribute(SessionAttributes.AUTH_USER) AuthenticatedUser user,
			@Valid @RequestBody ChatMessageRequest request) {
		if (user.role() != Role.EMPLOYEE) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN,
					"Only employees can access the chatbot");
		}
		return chatbotService.answer(user.userId(), request.message());
	}
}

