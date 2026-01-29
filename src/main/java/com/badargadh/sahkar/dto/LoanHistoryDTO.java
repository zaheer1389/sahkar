package com.badargadh.sahkar.dto;

public class LoanHistoryDTO {
	private String loanDate;
	private Long amount;
	private String status;
	private String collectionType;
	private String witnessName;
	private String authorityName;

	public LoanHistoryDTO(String loanDate, Long amount, String status, String collectionType, String witnessName, String authorityName) {
		this.loanDate = loanDate;
		this.amount = amount;
		this.status = status;
		this.collectionType = collectionType;
		this.witnessName = witnessName;
		this.authorityName = authorityName;
	}

	public String getLoanDate() {
		return loanDate;
	}

	public void setLoanDate(String loanDate) {
		this.loanDate = loanDate;
	}

	public Long getAmount() {
		return amount;
	}

	public void setAmount(Long amount) {
		this.amount = amount;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getCollectionType() {
		return collectionType;
	}

	public void setCollectionType(String collectionType) {
		this.collectionType = collectionType;
	}

	public String getWitnessName() {
		return witnessName;
	}

	public void setWitnessName(String witnessName) {
		this.witnessName = witnessName;
	}

	public String getAuthorityName() {
		return authorityName;
	}

	public void setAuthorityName(String authorityName) {
		this.authorityName = authorityName;
	}

	
}