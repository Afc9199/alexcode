package com.example.employeemanagement.dto;

import java.util.List;

public record LeaveBalanceResponse(
		String employeeId,
		List<LeaveBalanceDto> balances) {
	
	public record LeaveBalanceDto(
			String leaveType,
			int totalDays,
			int usedDays,
			int remainingDays,
			boolean isUnlimited) {
	}
}

