package com.example.employeemanagement.util;

import java.util.regex.Pattern;

public class PasswordValidator {

	private static final int MIN_LENGTH = 8;
	private static final int MAX_LENGTH = 15;
	private static final Pattern DIGIT_PATTERN = Pattern.compile(".*\\d.*");
	private static final Pattern LETTER_PATTERN = Pattern.compile(".*[a-zA-Z].*");
	private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");

	private PasswordValidator() {
		// Utility class
	}

	/**
	 * Validates password against strong password rules:
	 * - Length between 8-15 characters
	 * - At least one digit
	 * - At least one letter (alphabet)
	 * - At least one special character
	 * 
	 * @param password the password to validate
	 * @return validation result with success flag and error message
	 */
	public static ValidationResult validate(String password) {
		if (password == null || password.isEmpty()) {
			return ValidationResult.failure("Password is required");
		}

		if (password.length() < MIN_LENGTH) {
			return ValidationResult.failure("Password must be at least " + MIN_LENGTH + " characters long");
		}

		if (password.length() > MAX_LENGTH) {
			return ValidationResult.failure("Password must not exceed " + MAX_LENGTH + " characters");
		}

		if (!LETTER_PATTERN.matcher(password).matches()) {
			return ValidationResult.failure("Password must contain at least one letter");
		}

		if (!DIGIT_PATTERN.matcher(password).matches()) {
			return ValidationResult.failure("Password must contain at least one digit");
		}

		if (!SPECIAL_CHAR_PATTERN.matcher(password).matches()) {
			return ValidationResult.failure("Password must contain at least one special character (!@#$%^&*()_+-=[]{};':\"\\|,.<>/?)");
		}

		return ValidationResult.success();
	}

	public static class ValidationResult {
		private final boolean valid;
		private final String errorMessage;

		private ValidationResult(boolean valid, String errorMessage) {
			this.valid = valid;
			this.errorMessage = errorMessage;
		}

		public boolean isValid() {
			return valid;
		}

		public String getErrorMessage() {
			return errorMessage;
		}

		public static ValidationResult success() {
			return new ValidationResult(true, null);
		}

		public static ValidationResult failure(String errorMessage) {
			return new ValidationResult(false, errorMessage);
		}
	}
}

