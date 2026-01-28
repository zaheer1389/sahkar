package com.badargadh.sahkar.dto;

import java.time.LocalDate;

public class LoanWitnessBookDTO {

	private Integer memberNo;
	private String fullName;
	private String fullNameGuj;
	private Long loanAmount;
	private LocalDate loanDate;

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

	public String getFullNameGuj() {
		return fullNameGuj;
	}

	public void setFullNameGuj(String fullNameGuj) {
		this.fullNameGuj = fullNameGuj;
	}

	public Long getLoanAmount() {
		return loanAmount;
	}

	public void setLoanAmount(Long loanAmount) {
		this.loanAmount = loanAmount;
	}

	public LocalDate getLoanDate() {
		return loanDate;
	}

	public void setLoanDate(LocalDate loanDate) {
		this.loanDate = loanDate;
	}

}
