package com.example.employeemanagement.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException ex) {
		Map<String, Object> errorResponse = new HashMap<>();
		errorResponse.put("timestamp", Instant.now().toString());
		errorResponse.put("status", ex.getStatusCode().value());
		
		// Get reason phrase from HttpStatus if available
		String reasonPhrase = "Error";
		if (ex.getStatusCode() instanceof HttpStatus httpStatus) {
			reasonPhrase = httpStatus.getReasonPhrase();
		}
		
		errorResponse.put("error", reasonPhrase);
		errorResponse.put("message", ex.getReason() != null ? ex.getReason() : reasonPhrase);
		errorResponse.put("path", ""); // Can be populated if needed
		
		return ResponseEntity.status(ex.getStatusCode()).body(errorResponse);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
		Map<String, Object> errorResponse = new HashMap<>();
		errorResponse.put("timestamp", Instant.now().toString());
		errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
		errorResponse.put("error", "Validation Failed");
		
		String errorMessage = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.collect(Collectors.joining(", "));
		
		errorResponse.put("message", errorMessage);
		
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
	}
}

