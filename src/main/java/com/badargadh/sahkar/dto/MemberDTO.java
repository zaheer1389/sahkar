package com.badargadh.sahkar.dto;

import java.time.LocalDateTime;

import com.badargadh.sahkar.enums.MemberStatus;

public class MemberDTO {
    private Integer memberNo;
    private String fullName;
    private String fullNameGuj;
    private String branchName;
    private String branchNameGuj;
    private Double totalFees;
    private Double pendingLoan;
    private Double emiAmount;;

    public MemberDTO(
            Integer memberNo, 
            String fName, String mName, String lName,
            String fNameGuj, String mNameGuj, String lNameGuj,
            String branchName, 
            String branchNameGuj, 
            Double totalFees, 
            Double pendingLoan, 
            Double emiAmount) {
        
        this.memberNo = memberNo;
        this.fullName = fName + " " + mName + " " + lName + (branchName != null && !branchName.isEmpty() ? " ("+branchName+")" : "");;
        this.fullNameGuj = fNameGuj + " " + mNameGuj + " " + lNameGuj + (branchNameGuj != null && !branchNameGuj.isEmpty() ? " ("+branchNameGuj+")" : "");
        this.branchName = branchName;
        this.branchNameGuj = branchNameGuj;
        this.totalFees = totalFees != null ? totalFees.doubleValue() : 0.0;
        this.pendingLoan = pendingLoan != null ? pendingLoan : 0.0;
        this.emiAmount = emiAmount != null ? emiAmount : 0.0;
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

	public String getFullNameGuj() {
		return fullNameGuj;
	}

	public void setFullNameGuj(String fullNameGuj) {
		this.fullNameGuj = fullNameGuj;
	}

	public String getBranchName() {
		return branchName;
	}

	public void setBranchName(String branchName) {
		this.branchName = branchName;
	}

	public String getBranchNameGuj() {
		return branchNameGuj;
	}

	public void setBranchNameGuj(String branchNameGuj) {
		this.branchNameGuj = branchNameGuj;
	}

	public Double getTotalFees() {
		return totalFees;
	}

	public void setTotalFees(Double totalFees) {
		this.totalFees = totalFees;
	}

	public Double getPendingLoan() {
		return pendingLoan;
	}

	public void setPendingLoan(Double pendingLoan) {
		this.pendingLoan = pendingLoan;
	}

	public Double getEmiAmount() {
		return emiAmount;
	}

	public void setEmiAmount(Double emiAmount) {
		this.emiAmount = emiAmount;
	}

	
}