package com.example.employeemanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.example.employeemanagement.dto.LoginRequest;
import com.example.employeemanagement.model.Role;
import com.example.employeemanagement.model.UserAccount;
import com.example.employeemanagement.repository.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private UserAccountRepository userAccountRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	private AuthService authService;

	@BeforeEach
	void setUp() {
		authService = new AuthService(userAccountRepository, passwordEncoder);
	}

	@Test
	void loginReturnsResponseForValidCredentials() {
		UserAccount account = UserAccount.of("employee", "hash", Role.EMPLOYEE);
		when(userAccountRepository.findByUsernameIgnoreCase("employee")).thenReturn(Optional.of(account));
		when(passwordEncoder.matches("password", account.getPasswordHash())).thenReturn(true);

		var response = authService.login(new LoginRequest("employee", "password"));

		assertThat(response.username()).isEqualTo("employee");
		assertThat(response.role()).isEqualTo("EMPLOYEE");
	}

	@Test
	void loginThrowsForUnknownUser() {
		when(userAccountRepository.findByUsernameIgnoreCase(anyString())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.login(new LoginRequest("missing", "pw")))
				.isInstanceOf(ResponseStatusException.class)
				.extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
				.isEqualTo(HttpStatus.UNAUTHORIZED);
	}
}

