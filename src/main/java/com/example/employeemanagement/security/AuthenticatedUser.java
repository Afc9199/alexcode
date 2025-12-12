package com.example.employeemanagement.security;

import com.example.employeemanagement.model.Role;

public record AuthenticatedUser(String userId, String username, Role role) {
	public boolean isAdmin() {
		return Role.ADMIN.equals(role);
	}
}

