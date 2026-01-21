package com.badargadh.sahkar.data;

import java.util.List;
import java.util.Map;

public class MonthlyStatementDTO {

	private Double openingBal = 0.0;
	
	// --- Income Section (Cash Inflow) ---
	private Double newMemberFee = 0.0; // JOINING_FEE
	private Double monthlyFee = 0.0; // MONTHLY_FEE
	private Double loanDeduction = 0.0; // LOAN_DEDUCTION
	private Double totalEmi = 0.0; // Total EMI collection
	private Double fullPaymentAmount = 0.0; // Total from closed loans
	private Double expenseCredit = 0.0; // Jammat Recovery / Misc Credits

	// --- Outgoing Section (Cash Outflow) ---
	private Double totalLoanGranted = 0.0; // Principal disbursed
	private Double totalFeeRefund = 0.0; // REFUND type fees
	private Double expenseDebit = 0.0; // Monthly Expenses / Jammat Lending
	
	private Map<Double, Long> emiBreakdown;
	
	private List<List<String>> newMembersData;
	private List<List<String>> cancelledMembersData;
	private List<List<String>> refundMembersData;
	private List<List<String>> newLoanListMembersData;
	private List<List<String>> fullpaymentsData;
	private List<List<String>> prevMonthNewLoansEMIAmtData;

	/**
	 * Calculates the total income for the month.
	 */
	public Double getTotalIncome() {
		return  (openingBal != null ? openingBal : 0.0) 
				+ (newMemberFee != null ? newMemberFee : 0.0) + (monthlyFee != null ? monthlyFee : 0.0)
				+ (loanDeduction != null ? loanDeduction : 0.0) + (totalEmi != null ? totalEmi : 0.0)
				+ (fullPaymentAmount != null ? fullPaymentAmount : 0.0) + (expenseCredit != null ? expenseCredit : 0.0);
	}

	/**
	 * Calculates the total outgoing payments for the month.
	 */
	public Double getTotalOutgoing() {
		return (totalLoanGranted != null ? totalLoanGranted : 0.0) + (totalFeeRefund != null ? totalFeeRefund : 0.0)
				+ (expenseDebit != null ? expenseDebit : 0.0);
	}
	
	public Double getClosingBalance() {
		return getTotalIncome() - getTotalOutgoing();
	}

	/**
	 * Calculates the net cash flow (Net Profit/Loss for the month).
	 */
	public Double getNetCashFlow() {
		return getTotalIncome() - getTotalOutgoing();
	}

	public Double getNewMemberFee() {
		return newMemberFee;
	}

	public void setNewMemberFee(Double newMemberFee) {
		this.newMemberFee = newMemberFee;
	}

	public Double getMonthlyFee() {
		return monthlyFee;
	}

	public void setMonthlyFee(Double monthlyFee) {
		this.monthlyFee = monthlyFee;
	}

	public Double getLoanDeduction() {
		return loanDeduction;
	}

	public void setLoanDeduction(Double loanDeduction) {
		this.loanDeduction = loanDeduction;
	}

	public Double getTotalEmi() {
		return totalEmi;
	}

	public void setTotalEmi(Double totalEmi) {
		this.totalEmi = totalEmi;
	}

	public Double getFullPaymentAmount() {
		return fullPaymentAmount;
	}

	public void setFullPaymentAmount(Double fullPaymentAmount) {
		this.fullPaymentAmount = fullPaymentAmount;
	}

	public Double getExpenseCredit() {
		return expenseCredit;
	}

	public void setExpenseCredit(Double expenseCredit) {
		this.expenseCredit = expenseCredit;
	}

	public Double getTotalLoanGranted() {
		return totalLoanGranted;
	}

	public void setTotalLoanGranted(Double totalLoanGranted) {
		this.totalLoanGranted = totalLoanGranted;
	}

	public Double getTotalFeeRefund() {
		return totalFeeRefund;
	}

	public void setTotalFeeRefund(Double totalFeeRefund) {
		this.totalFeeRefund = totalFeeRefund;
	}

	public Double getExpenseDebit() {
		return expenseDebit;
	}

	public void setExpenseDebit(Double expenseDebit) {
		this.expenseDebit = expenseDebit;
	}

	public Double getOpeningBal() {
		return openingBal;
	}

	public void setOpeningBal(Double openingBal) {
		this.openingBal = openingBal;
	}

	public Map<Double, Long> getEmiBreakdown() {
		return emiBreakdown;
	}

	public void setEmiBreakdown(Map<Double, Long> emiBreakdown) {
		this.emiBreakdown = emiBreakdown;
	}

	public List<List<String>> getNewMembersData() {
		return newMembersData;
	}

	public void setNewMembersData(List<List<String>> newMembersData) {
		this.newMembersData = newMembersData;
	}

	public List<List<String>> getCancelledMembersData() {
		return cancelledMembersData;
	}

	public void setCancelledMembersData(List<List<String>> cancelledMembersData) {
		this.cancelledMembersData = cancelledMembersData;
	}

	public List<List<String>> getRefundMembersData() {
		return refundMembersData;
	}

	public void setRefundMembersData(List<List<String>> refundMembersData) {
		this.refundMembersData = refundMembersData;
	}

	public List<List<String>> getNewLoanListMembersData() {
		return newLoanListMembersData;
	}

	public void setNewLoanListMembersData(List<List<String>> newLoanListMembersData) {
		this.newLoanListMembersData = newLoanListMembersData;
	}

	public List<List<String>> getFullpaymentsData() {
		return fullpaymentsData;
	}

	public void setFullpaymentsData(List<List<String>> fullpaymentsData) {
		this.fullpaymentsData = fullpaymentsData;
	}

	public List<List<String>> getPrevMonthNewLoansEMIAmtData() {
		return prevMonthNewLoansEMIAmtData;
	}

	public void setPrevMonthNewLoansEMIAmtData(List<List<String>> prevMonthNewLoansEMIAmtData) {
		this.prevMonthNewLoansEMIAmtData = prevMonthNewLoansEMIAmtData;
	}

}
