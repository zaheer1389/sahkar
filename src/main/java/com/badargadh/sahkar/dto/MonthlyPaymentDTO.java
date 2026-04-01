package com.badargadh.sahkar.dto;

import java.time.LocalDateTime;

public class MonthlyPaymentDTO {
	private Long memberId;
	private String memberName;
	private Integer memberNo;

	// Fee Details
	private Double monthlyFee = 0.0;
	private Double joiningFee = 0.0;
	private Double loanDeduction = 0.0;

	// EMI Details
	private Double emiAmount = 0.0;
	private boolean isFullPayment = false;
	private Double fullPaymentExtra = 0.0;

	// Metadata
	private Double totalPaid = 0.0;
	private LocalDateTime lastTransactionDate;

	public Long getMemberId() {
		return memberId;
	}

	public void setMemberId(Long memberId) {
		this.memberId = memberId;
	}

	public String getMemberName() {
		return memberName;
	}

	public void setMemberName(String memberName) {
		this.memberName = memberName;
	}

	public Integer getMemberNo() {
		return memberNo;
	}

	public void setMemberNo(Integer memberNo) {
		this.memberNo = memberNo;
	}

	public Double getMonthlyFee() {
		return monthlyFee;
	}

	public void setMonthlyFee(Double monthlyFee) {
		this.monthlyFee = monthlyFee;
	}

	public Double getJoiningFee() {
		return joiningFee;
	}

	public void setJoiningFee(Double joiningFee) {
		this.joiningFee = joiningFee;
	}

	public Double getLoanDeduction() {
		return loanDeduction;
	}

	public void setLoanDeduction(Double loanDeduction) {
		this.loanDeduction = loanDeduction;
	}

	public Double getEmiAmount() {
		return emiAmount;
	}

	public void setEmiAmount(Double emiAmount) {
		this.emiAmount = emiAmount;
	}

	public boolean isFullPayment() {
		return isFullPayment;
	}

	public void setFullPayment(boolean isFullPayment) {
		this.isFullPayment = isFullPayment;
	}

	public Double getFullPaymentExtra() {
		return fullPaymentExtra;
	}

	public void setFullPaymentExtra(Double fullPaymentExtra) {
		this.fullPaymentExtra = fullPaymentExtra;
	}

	public Double getTotalPaid() {
		return totalPaid;
	}

	public void setTotalPaid(Double totalPaid) {
		this.totalPaid = totalPaid;
	}

	public LocalDateTime getLastTransactionDate() {
		return lastTransactionDate;
	}

	public void setLastTransactionDate(LocalDateTime lastTransactionDate) {
		this.lastTransactionDate = lastTransactionDate;
	}

}