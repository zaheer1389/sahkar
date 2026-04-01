package com.badargadh.sahkar.dto;

public class PendingPaymentDTO {
    private Long memberId;
    private Integer memberNo;
    private String fullName;
    
    // Monthly Fee (e.g., 200)
    private Double expectedFee;
    
    // Loan details
    private Long loanAccountId;
    private Double expectedEmi;
    private Double remainingLoanBalance;
    
    private Double totalPending; // expectedFee + expectedEmi

	public Long getMemberId() {
		return memberId;
	}

	public void setMemberId(Long memberId) {
		this.memberId = memberId;
	}

	public Integer getMemberNo() {
		return memberNo;
	}

	public void setMemberNo(Integer memberNo) {
		this.memberNo = memberNo;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public Double getExpectedFee() {
		return expectedFee;
	}

	public void setExpectedFee(Double expectedFee) {
		this.expectedFee = expectedFee;
	}

	public Long getLoanAccountId() {
		return loanAccountId;
	}

	public void setLoanAccountId(Long loanAccountId) {
		this.loanAccountId = loanAccountId;
	}

	public Double getExpectedEmi() {
		return expectedEmi;
	}

	public void setExpectedEmi(Double expectedEmi) {
		this.expectedEmi = expectedEmi;
	}

	public Double getRemainingLoanBalance() {
		return remainingLoanBalance;
	}

	public void setRemainingLoanBalance(Double remainingLoanBalance) {
		this.remainingLoanBalance = remainingLoanBalance;
	}

	public Double getTotalPending() {
		return totalPending;
	}

	public void setTotalPending(Double totalPending) {
		this.totalPending = totalPending;
	}

    
}