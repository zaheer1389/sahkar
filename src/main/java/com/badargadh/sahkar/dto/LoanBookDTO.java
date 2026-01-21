package com.badargadh.sahkar.dto;

import java.time.LocalDate;
import java.util.Arrays;

public class LoanBookDTO {
	private Integer memberNo;
	private String name;
	private int disbursementMonth; // 1-12
	private Double loanAmount; // The original loan
	private Double[] monthlyPayments = new Double[12]; // Index 0-11
	private Double[] monthlyPaymentsDr = new Double[12]; // Index 0-11
	private Double closingBalance; // Used for Year-End Running Balance
	private Double openingBalance;
	private LocalDate createdDate;
	
	public LoanBookDTO() {
		Arrays.fill(monthlyPayments, 0.0);
		Arrays.fill(monthlyPaymentsDr, 0.0);
	}

	public Integer getMemberNo() {
		return memberNo;
	}

	public void setMemberNo(Integer memberNo) {
		this.memberNo = memberNo;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getDisbursementMonth() {
		return disbursementMonth;
	}

	public void setDisbursementMonth(int disbursementMonth) {
		this.disbursementMonth = disbursementMonth;
	}

	public Double getLoanAmount() {
		return loanAmount;
	}

	public void setLoanAmount(Double loanAmount) {
		this.loanAmount = loanAmount;
	}

	public Double[] getMonthlyPayments() {
		return monthlyPayments;
	}

	public void setMonthlyPayments(Double[] monthlyPayments) {
		this.monthlyPayments = monthlyPayments;
	}

	public Double getClosingBalance() {
		return closingBalance;
	}

	public void setClosingBalance(Double closingBalance) {
		this.closingBalance = closingBalance;
	}

	public Double getOpeningBalance() {
		return openingBalance;
	}

	public void setOpeningBalance(Double openingBalance) {
		this.openingBalance = openingBalance;
	}

	public LocalDate getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(LocalDate createdDate) {
		this.createdDate = createdDate;
	}

	public Double[] getMonthlyPaymentsDr() {
		return monthlyPaymentsDr;
	}

	public void setMonthlyPaymentsDr(Double[] monthlyPaymentsCr) {
		this.monthlyPaymentsDr = monthlyPaymentsCr;
	}

	
}