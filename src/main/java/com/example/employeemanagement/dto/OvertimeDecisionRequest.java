package com.example.employeemanagement.dto;

import com.example.employeemanagement.model.OvertimeStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OvertimeDecisionRequest(
		@NotNull OvertimeStatus status,
		@NotBlank(message = "Manager comment is required") String managerComment,
		@NotBlank(message = "DecidedBy is required") String decidedBy) {
}

