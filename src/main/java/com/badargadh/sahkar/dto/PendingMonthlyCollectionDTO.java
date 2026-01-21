package com.badargadh.sahkar.dto;

import com.badargadh.sahkar.enums.MemberStatus;

public class PendingMonthlyCollectionDTO {
    private Integer memberNo;
    private String firstName;
    private String middleName;
    private String lastName;
    private String village;
    private MemberStatus status;
    private Double totalHistoricalFeesPaid;
    private Double loanPendingAmount;
    private Double emiAmountDue;
    private String fullNameGuj;
    private String branchNameGuj;
    private String fullName;
    
    // Constructor matching the JPQL projection order
    public PendingMonthlyCollectionDTO(Integer memberNo, String firstName, String middleName, String lastName, 
                                       String village, MemberStatus status, Double totalHistoricalFeesPaid, 
                                       Double loanPendingAmount, Double emiAmountDue, 
                                       String fullNameGuj, String branchNameGuj) {
        this.memberNo = memberNo;
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.village = village;
        this.status = status;
        this.totalHistoricalFeesPaid = totalHistoricalFeesPaid;
        this.loanPendingAmount = loanPendingAmount;
        this.emiAmountDue = emiAmountDue;
        this.fullNameGuj = fullNameGuj;
        this.branchNameGuj = branchNameGuj;
        
        this.fullName = (firstName + " " + middleName + " " + lastName).trim();
    }

	public Integer getMemberNo() {
		return memberNo;
	}

	public void setMemberNo(Integer memberNo) {
		this.memberNo = memberNo;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getMiddleName() {
		return middleName;
	}

	public void setMiddleName(String middleName) {
		this.middleName = middleName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getVillage() {
		return village;
	}

	public void setVillage(String village) {
		this.village = village;
	}

	public MemberStatus getStatus() {
		return status;
	}

	public void setStatus(MemberStatus status) {
		this.status = status;
	}

	public Double getTotalHistoricalFeesPaid() {
		return totalHistoricalFeesPaid;
	}

	public void setTotalHistoricalFeesPaid(Double totalHistoricalFeesPaid) {
		this.totalHistoricalFeesPaid = totalHistoricalFeesPaid;
	}

	public Double getLoanPendingAmount() {
		return loanPendingAmount;
	}

	public void setLoanPendingAmount(Double loanPendingAmount) {
		this.loanPendingAmount = loanPendingAmount;
	}

	public Double getEmiAmountDue() {
		return emiAmountDue;
	}

	public void setEmiAmountDue(Double emiAmountDue) {
		this.emiAmountDue = emiAmountDue;
	}

	public String getFullNameGuj() {
		return fullNameGuj;
	}

	public void setFullNameGuj(String fullNameGuj) {
		this.fullNameGuj = fullNameGuj;
	}

	public String getBranchNameGuj() {
		return branchNameGuj;
	}

	public void setBranchNameGuj(String branchNameGuj) {
		this.branchNameGuj = branchNameGuj;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

    
}