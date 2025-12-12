package com.example.employeemanagement.util;

/**
 * Malaysian Income Tax Calculator (PCB / LHDN)
 * Implements the Computerized Calculation Method (CCM) for residents
 * and flat 30% rate for non-residents
 */
public class TaxCalculator {

	// MALAYSIAN INCOME TAX RATES (RESIDENT) - YA 2024/2025
	private static final TaxBracket[] TAX_BRACKETS = {
			new TaxBracket(0, 5000, 0.00, 0),
			new TaxBracket(5001, 20000, 0.01, 0), // Max tax: 150
			new TaxBracket(20001, 35000, 0.03, 150), // Max tax: 450 + 150 = 600
			new TaxBracket(35001, 50000, 0.06, 600), // Max tax: 900 + 600 = 1500
			new TaxBracket(50001, 70000, 0.11, 1500), // Max tax: 2200 + 1500 = 3700
			new TaxBracket(70001, 100000, 0.19, 3700), // Max tax: 5700 + 3700 = 9400
			new TaxBracket(100001, 400000, 0.25, 9400), // Max tax: 75000 + 9400 = 84400
			new TaxBracket(400001, 600000, 0.26, 84400), // Max tax: 52000 + 84400 = 136400
			new TaxBracket(600001, 2000000, 0.28, 136400), // Max tax: 392000 + 136400 = 528400
			new TaxBracket(2000001, Integer.MAX_VALUE, 0.30, 528400)
	};

	private static class TaxBracket {
		final int min;
		final int max;
		final double rate;
		final int baseTax;

		TaxBracket(int min, int max, double rate, int baseTax) {
			this.min = min;
			this.max = max;
			this.rate = rate;
			this.baseTax = baseTax;
		}
	}

	/**
	 * Calculate monthly PCB (Monthly Tax Deduction) for an employee
	 * 
	 * @param monthlyTaxableIncome Monthly taxable income
	 * @param isResident Whether employee is a resident
	 * @param totalReliefs Total annual reliefs (Individual + Spouse + Children + EPF)
	 * @return Monthly PCB amount
	 */
	public static double calculateMonthlyPCB(double monthlyTaxableIncome, boolean isResident, double totalReliefs) {
		if (!isResident) {
			// Non-Residents: Flat 30% tax rate on all Taxable Income
			return monthlyTaxableIncome * 0.30;
		}

		// Residents: Calculate using CCM formula
		// Step 1: Calculate Annualized Taxable Income
		double annualTaxableIncome = (monthlyTaxableIncome * 12) - totalReliefs;

		if (annualTaxableIncome <= 0) {
			return 0.0;
		}

		// Step 2: Calculate annual tax using progressive rates
		double annualTax = calculateAnnualTax(annualTaxableIncome);

		// Step 3: Divide by 12 to get monthly PCB
		return annualTax / 12.0;
	}

	/**
	 * Calculate annual tax based on progressive tax brackets
	 */
	private static double calculateAnnualTax(double annualTaxableIncome) {
		for (TaxBracket bracket : TAX_BRACKETS) {
			if (annualTaxableIncome >= bracket.min && annualTaxableIncome <= bracket.max) {
				double taxableInBracket = annualTaxableIncome - bracket.min;
				return bracket.baseTax + (taxableInBracket * bracket.rate);
			}
		}
		// Should not reach here, but return 0 if somehow it does
		return 0.0;
	}

	/**
	 * Calculate total reliefs for tax calculation
	 * 
	 * @param numberOfChildren Number of children
	 * @param spouseWorking Whether spouse is working (affects spouse relief eligibility)
	 * @param annualEpfContribution Annual EPF contribution (capped at 4000)
	 * @return Total annual reliefs
	 */
	public static double calculateTotalReliefs(int numberOfChildren, boolean spouseWorking,
			double annualEpfContribution) {
		double total = 9000; // Individual relief

		// Spouse relief: 4000 if spouse is not working
		if (!spouseWorking) {
			total += 4000;
		}

		// Child relief: 2000 per child
		total += numberOfChildren * 2000;

		// EPF relief: Capped at 4000
		total += Math.min(annualEpfContribution, 4000);

		return total;
	}

	/**
	 * Round up to nearest Ringgit (for EPF, SOCSO contributions)
	 */
	public static double roundUpToRinggit(double amount) {
		return Math.ceil(amount);
	}
}

