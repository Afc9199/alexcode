package com.example.employeemanagement.service;

import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.employeemanagement.dto.LoginRequest;
import com.example.employeemanagement.dto.LoginResponse;
import com.example.employeemanagement.model.Role;
import com.example.employeemanagement.model.UserAccount;
import com.example.employeemanagement.repository.UserAccountRepository;

@Service
public class AuthService {

	private static final Logger log = LoggerFactory.getLogger(AuthService.class);

	private final UserAccountRepository userAccountRepository;
	private final PasswordEncoder passwordEncoder;

	public AuthService(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder) {
		this.userAccountRepository = userAccountRepository;
		this.passwordEncoder = passwordEncoder;
	}

	public LoginResponse login(LoginRequest request) {
		UserAccount account = userAccountRepository.findByUsernameIgnoreCase(request.username())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

		if (account.getRole() == Role.EMPLOYEE && !account.isActive()) {
			log.warn("Blocked login attempt for inactive employee {}", account.getUsername());
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
					"Your Employee Account is inactive. Contact admin to reactivate your employee account");
		}

		if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
			log.warn("Invalid password for user {}", account.getUsername());
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
		}

		log.info("Successful login for user {} with role {}", account.getUsername(), account.getRole());
		return new LoginResponse(account.getId(), account.getUsername(), account.getRole().name(), "Login successful");
	}

	public UserAccount createUserIfMissing(String username, String rawPassword,
			com.example.employeemanagement.model.Role role, String fullName, String email, String department,
			String jobTitle) {
		String safeUsername = Objects.requireNonNull(username, "Username is required").trim();
		String safePassword = Objects.requireNonNull(rawPassword, "Password is required");
		Role safeRole = Objects.requireNonNull(role, "Role is required");
		Optional<UserAccount> existing = userAccountRepository.findByUsernameIgnoreCase(safeUsername);
		if (existing.isEmpty()) {
			UserAccount newAccount = UserAccount.of(safeUsername, passwordEncoder.encode(safePassword), safeRole);
			newAccount.setEmployeeId(generateNextEmployeeId());
			newAccount.setFullName(fullName);
			newAccount.setEmail(email);
			newAccount.setDepartment(department);
			newAccount.setJobTitle(jobTitle);
			return userAccountRepository.save(newAccount);
		}

		return existing.orElseThrow();
	}

	private String generateNextEmployeeId() {
		return userAccountRepository.findFirstByOrderByEmployeeIdDesc()
				.map(lastUser -> {
					String lastId = lastUser.getEmployeeId();
					if (lastId != null && lastId.startsWith("E")) {
						try {
							int number = Integer.parseInt(lastId.substring(1));
							return String.format("E%03d", number + 1);
						} catch (NumberFormatException e) {
							return "E001";
						}
					}
					return "E001";
				})
				.orElse("E001");
	}
}

