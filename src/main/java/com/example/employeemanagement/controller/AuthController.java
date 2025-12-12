package com.example.employeemanagement.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.employeemanagement.dto.LoginRequest;
import com.example.employeemanagement.dto.LoginResponse;
import com.example.employeemanagement.model.Role;
import com.example.employeemanagement.security.AuthenticatedUser;
import com.example.employeemanagement.security.SessionAttributes;
import com.example.employeemanagement.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
			HttpServletRequest httpRequest) {
		LoginResponse response = authService.login(request);
		renewSession(httpRequest, response);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/me")
	public ResponseEntity<LoginResponse> getCurrentUser(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session == null) {
			return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
		}
		Object attribute = session.getAttribute(SessionAttributes.AUTH_USER);
		if (attribute instanceof AuthenticatedUser user) {
			LoginResponse response = new LoginResponse(user.userId(), user.username(), user.role().name(),
					"Authenticated");
			return ResponseEntity.ok(response);
		}
		return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
		return ResponseEntity.noContent().build();
	}

	private void renewSession(HttpServletRequest request, LoginResponse response) {
		HttpSession existing = request.getSession(false);
		if (existing != null) {
			existing.invalidate();
		}
		HttpSession session = request.getSession(true);
		session.setAttribute(SessionAttributes.AUTH_USER,
				new AuthenticatedUser(response.userId(), response.username(), Role.valueOf(response.role())));
	}
}

