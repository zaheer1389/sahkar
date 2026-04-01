package com.badargadh.sahkar.dto;

import com.badargadh.sahkar.data.FinancialMonth;

public class MonthlyFundSummaryDTO {
	// Inflows
	private double openingBal;
	private double newMemberJoiningFees;
	private double totalTargetEmi;
	private double totalFreshLoansAmount;
	private double totalFeesDeductions; // From new member loans
	private double expectedMonthlyFees; // Regular monthly 200/300
	private double totalFullPayment; // loan closure pending amount
	private double otherMiscCredit; // payment credit from JAMMAT_BADARGADH

	// Outflows
	private double totalRefundLiability;
	private double otherMiscDebit; // payment debit to JAMMAT_BADARGADH

	// Metadata
	private FinancialMonth financialMonth;
	private int freshLoansEmiCounter;
	private double defaultEmiamount;

	public double getOpeningBal() {
		return openingBal;
	}

	public void setOpeningBal(double openingBal) {
		this.openingBal = openingBal;
	}

	public double getNewMemberJoiningFees() {
		return newMemberJoiningFees;
	}

	public void setNewMemberJoiningFees(double newMemberJoiningFees) {
		this.newMemberJoiningFees = newMemberJoiningFees;
	}

	public double getTotalTargetEmi() {
		return totalTargetEmi;
	}

	public void setTotalTargetEmi(double totalTargetEmi) {
		this.totalTargetEmi = totalTargetEmi;
	}

	public double getTotalFreshLoansAmount() {
		return defaultEmiamount * freshLoansEmiCounter;
	}

	/*public void setTotalFreshLoansAmount(double totalFreshLoansAmount) {
		this.totalFreshLoansAmount = totalFreshLoansAmount;
	}*/

	public double getTotalFeesDeductions() {
		return totalFeesDeductions;
	}

	public void setTotalFeesDeductions(double totalFeesDeductions) {
		this.totalFeesDeductions = totalFeesDeductions;
	}

	public double getExpectedMonthlyFees() {
		return expectedMonthlyFees;
	}

	public void setExpectedMonthlyFees(double expectedMonthlyFees) {
		this.expectedMonthlyFees = expectedMonthlyFees;
	}

	public double getTotalFullPayment() {
		return totalFullPayment;
	}

	public void setTotalFullPayment(double totalFullPayment) {
		this.totalFullPayment = totalFullPayment;
	}

	public double getOtherMiscCredit() {
		return otherMiscCredit;
	}

	public void setOtherMiscCredit(double otherMiscCredit) {
		this.otherMiscCredit = otherMiscCredit;
	}

	public double getTotalRefundLiability() {
		return totalRefundLiability;
	}

	public void setTotalRefundLiability(double totalRefundLiability) {
		this.totalRefundLiability = totalRefundLiability;
	}

	public double getOtherMiscDebit() {
		return otherMiscDebit;
	}

	public void setOtherMiscDebit(double otherMiscDebit) {
		this.otherMiscDebit = otherMiscDebit;
	}

	public FinancialMonth getFinancialMonth() {
		return financialMonth;
	}

	public void setFinancialMonth(FinancialMonth financialMonth) {
		this.financialMonth = financialMonth;
	}

	public int getFreshLoansEmiCounter() {
		return freshLoansEmiCounter;
	}

	public void setFreshLoansEmiCounter(int freshLoansEmiCounter) {
		this.freshLoansEmiCounter = freshLoansEmiCounter;
	}

	public double getDefaultEmiamount() {
		return defaultEmiamount;
	}

	public void setDefaultEmiamount(double defaultEmiamount) {
		this.defaultEmiamount = defaultEmiamount;
	}

	public double getExpectedNet() {
		return (openingBal + expectedMonthlyFees + totalTargetEmi + totalFreshLoansAmount + totalFullPayment
				+ totalFeesDeductions + otherMiscCredit) - totalRefundLiability;
	}

}
