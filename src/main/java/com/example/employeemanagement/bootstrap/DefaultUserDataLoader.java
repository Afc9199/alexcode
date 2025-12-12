package com.example.employeemanagement.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.example.employeemanagement.model.Role;
import com.example.employeemanagement.service.AuthService;

@Component
public class DefaultUserDataLoader implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(DefaultUserDataLoader.class);

	private final AuthService authService;

	public DefaultUserDataLoader(AuthService authService) {
		this.authService = authService;
	}

	@Override
	public void run(String... args) {
		authService.createUserIfMissing("admin", "admin123", Role.ADMIN, "Admin User", "admin@example.com", "Management", "Administrator");
		authService.createUserIfMissing("employee", "employee123", Role.EMPLOYEE, "Employee User", "employee@example.com", "Operations", "Analyst");
		log.info("Ensured default admin and employee accounts exist");
	}
}

