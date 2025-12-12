package com.example.employeemanagement.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates the SOCSO contribution rate table dynamically.
 * Based on official Oct 2024 statutory pattern.
 */
public class SocsoTableGenerator {

	public static class SocsoRateRow {
		private final double min;
		private final double max;
		private final double type1Employee; // Age < 60
		private final double type1Employer; // Age < 60
		private final double type2Employer; // Age >= 60 (employee pays 0)
		private final double eisEmployee; // Age < 60
		private final double eisEmployer; // Age < 60

		public SocsoRateRow(double min, double max, double type1Employee, double type1Employer,
				double type2Employer, double eisEmployee, double eisEmployer) {
			this.min = min;
			this.max = max;
			this.type1Employee = type1Employee;
			this.type1Employer = type1Employer;
			this.type2Employer = type2Employer;
			this.eisEmployee = eisEmployee;
			this.eisEmployer = eisEmployer;
		}

		public double getMin() {
			return min;
		}

		public double getMax() {
			return max;
		}

		public double getType1Employee() {
			return type1Employee;
		}

		public double getType1Employer() {
			return type1Employer;
		}

		public double getType2Employer() {
			return type2Employer;
		}

		public double getEisEmployee() {
			return eisEmployee;
		}

		public double getEisEmployer() {
			return eisEmployer;
		}
	}

	/**
	 * Generates the full SOCSO rate table for 2025/2026
	 */
	public static List<SocsoRateRow> generateSocsoTable() {
		List<SocsoRateRow> rates = new ArrayList<>();

		// PART A: Low Income (Irregular Steps below RM 140)
		rates.add(new SocsoRateRow(0, 30, 0.10, 0.40, 0.30, 0.05, 0.05));
		rates.add(new SocsoRateRow(30, 50, 0.20, 0.70, 0.50, 0.05, 0.05));
		rates.add(new SocsoRateRow(50, 70, 0.30, 1.10, 0.80, 0.10, 0.10));
		rates.add(new SocsoRateRow(70, 100, 0.40, 1.50, 1.10, 0.15, 0.15));
		rates.add(new SocsoRateRow(100, 140, 0.60, 2.10, 1.50, 0.20, 0.20));

		// PART B: Standard Calculation (RM 140 to RM 6,000)
		// Hardcode the start of the regular pattern (140-200)
		rates.add(new SocsoRateRow(140, 200, 0.85, 2.95, 2.10, 0.30, 0.30));

		// Generate regular pattern from 200 to 6000 (increments of 100)
		for (int i = 200; i < 6000; i += 100) {
			int steps = (i - 200) / 100;
			double type1Employee = 1.25 + (steps * 0.50);
			double type1Employer = 4.35 + (steps * 1.75);
			double type2Employer = 3.10 + (steps * 1.25);
			double eisAmount = 0.50 + (steps * 0.20);

			rates.add(new SocsoRateRow(i, i + 100,
					Math.round(type1Employee * 100.0) / 100.0,
					Math.round(type1Employer * 100.0) / 100.0,
					Math.round(type2Employer * 100.0) / 100.0,
					Math.round(eisAmount * 100.0) / 100.0,
					Math.round(eisAmount * 100.0) / 100.0));
		}

		// PART C: The Maximum Cap (Wages > RM 6,000)
		rates.add(new SocsoRateRow(6000, Double.MAX_VALUE, 29.75, 104.15, 74.40, 11.90, 11.90));

		return rates;
	}

	/**
	 * Calculate SOCSO and EIS contributions based on wages and age
	 * 
	 * @param socsoWages The SOCSO wages (MUST EXCLUDE Overtime)
	 * @param age Employee age
	 * @return Object containing employee and employer shares
	 */
	public static SocsoEisResult calculateSocsoAndEis(double socsoWages, int age) {
		List<SocsoRateRow> table = generateSocsoTable();

		// Find the matching row
		SocsoRateRow matchingRow = null;
		for (SocsoRateRow row : table) {
			if (socsoWages > row.getMin() && socsoWages <= row.getMax()) {
				matchingRow = row;
				break;
			}
		}

		// If no match found (shouldn't happen), use the last row (max cap)
		if (matchingRow == null) {
			matchingRow = table.get(table.size() - 1);
		}

		// Age-based selection
		if (age < 60) {
			// Type 1 (Category 1): Under 60
			return new SocsoEisResult(
					matchingRow.getType1Employee(),
					matchingRow.getType1Employer(),
					matchingRow.getEisEmployee(),
					matchingRow.getEisEmployer());
		} else {
			// Type 2 (Category 2): 60 and above
			return new SocsoEisResult(
					0.00, // Employee pays 0
					matchingRow.getType2Employer(),
					0.00, // EIS employee pays 0
					0.00); // EIS employer pays 0
		}
	}

	public static class SocsoEisResult {
		private final double socsoEmployee;
		private final double socsoEmployer;
		private final double eisEmployee;
		private final double eisEmployer;

		public SocsoEisResult(double socsoEmployee, double socsoEmployer, double eisEmployee, double eisEmployer) {
			this.socsoEmployee = socsoEmployee;
			this.socsoEmployer = socsoEmployer;
			this.eisEmployee = eisEmployee;
			this.eisEmployer = eisEmployer;
		}

		public double getSocsoEmployee() {
			return socsoEmployee;
		}

		public double getSocsoEmployer() {
			return socsoEmployer;
		}

		public double getEisEmployee() {
			return eisEmployee;
		}

		public double getEisEmployer() {
			return eisEmployer;
		}
	}
}

