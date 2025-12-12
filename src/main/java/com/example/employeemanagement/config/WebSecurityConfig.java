package com.example.employeemanagement.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import com.example.employeemanagement.security.SessionAuthenticationFilter;

@Configuration
public class WebSecurityConfig {

	@Bean
	public SessionAuthenticationFilter sessionAuthenticationFilter() {
		return new SessionAuthenticationFilter();
	}

	@Bean
	public FilterRegistrationBean<SessionAuthenticationFilter> sessionAuthenticationFilterRegistration(
			SessionAuthenticationFilter filter) {
		FilterRegistrationBean<SessionAuthenticationFilter> registration = new FilterRegistrationBean<>();
		registration.setFilter(filter);
		registration.addUrlPatterns("/*");
		registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
		return registration;
	}

	@Bean
	public CorsFilter corsFilter() {
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		CorsConfiguration config = new CorsConfiguration();
		
		// Allow credentials (cookies, authorization headers)
		config.setAllowCredentials(true);
		
		// Allow all origins (for ngrok and local development)
		// In production, you should restrict this to specific domains
		config.addAllowedOriginPattern("*");
		
		// Allow all headers
		config.addAllowedHeader("*");
		
		// Allow all HTTP methods
		config.addAllowedMethod("*");
		
		// Expose headers that might be needed
		config.addExposedHeader("*");
		
		// Set max age for preflight requests
		config.setMaxAge(3600L);
		
		source.registerCorsConfiguration("/**", config);
		return new CorsFilter(source);
	}
}

