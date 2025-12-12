package com.example.employeemanagement.security;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class SessionAuthenticationFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(SessionAuthenticationFilter.class);

	private static final Set<String> PROTECTED_PATHS = Set.of("/dashboard.html");
	private static final List<String> PROTECTED_API_PREFIXES = List.of("/api/employee", "/api/admin");
	private static final Set<String> PUBLIC_HTML_PATHS = Set.of("/login.html", "/");

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain) throws ServletException, IOException {

		String path = request.getRequestURI();

		if (!requiresAuthentication(path)) {
			filterChain.doFilter(request, response);
			return;
		}

		HttpSession session = request.getSession(false);
		AuthenticatedUser authenticatedUser = resolveUser(session);

		if (authenticatedUser == null) {
			log.debug("Unauthenticated access attempt to {}", path);
			handleUnauthenticated(path, response);
			return;
		}

		if (path.startsWith("/api/admin") && !authenticatedUser.isAdmin()) {
			log.warn("User {} attempted to access admin resource {}", authenticatedUser.username(), path);
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "Admin role required");
			return;
		}

		if (path.startsWith("/admin-") && !authenticatedUser.isAdmin()) {
			log.warn("User {} attempted to access admin page {}", authenticatedUser.username(), path);
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "Admin role required");
			return;
		}

		request.setAttribute(SessionAttributes.AUTH_USER, authenticatedUser);
		filterChain.doFilter(request, response);
	}

	private boolean requiresAuthentication(String path) {
		if (PROTECTED_PATHS.contains(path)) {
			return true;
		}
		if (PROTECTED_API_PREFIXES.stream().anyMatch(path::startsWith)) {
			return true;
		}
		if (path.endsWith(".html") && !PUBLIC_HTML_PATHS.contains(path)) {
			return true;
		}
		return false;
	}

	private AuthenticatedUser resolveUser(HttpSession session) {
		if (session == null) {
			return null;
		}
		Object attribute = session.getAttribute(SessionAttributes.AUTH_USER);
		if (attribute instanceof AuthenticatedUser user) {
			return user;
		}
		return null;
	}

	private void handleUnauthenticated(String path, HttpServletResponse response) throws IOException {
		if (path.endsWith(".html")) {
			response.sendRedirect("/login.html");
		} else {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
		}
	}
}

