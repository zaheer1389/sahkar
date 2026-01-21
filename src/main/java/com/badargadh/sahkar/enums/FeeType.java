package com.badargadh.sahkar.enums;

public enum FeeType {
	OPENING_BALANCE,
    MONTHLY_FEE,      // The 200 contribution (Refundable)
    JOINING_FEE,      // The 3000 fee (Non-Refundable)
    RE_JOINING_FEE, //50% of total society fee
    LOAN_DEDUCTION,   // Catch-up fee deducted from first loan (Refundable)
    REFUND            // Money given back on exit (Reduces Balance)
}