package com.badargadh.sahkar.dto;

import java.time.LocalDateTime;

import com.badargadh.sahkar.enums.MemberStatus;

public class MemberSummaryDTO {
	private Integer memberNo;
	private String fullName; // Single field for name
	private String village;
	private MemberStatus status;
	
	private Integer totalFees;
    private Integer pendingLoan;
    private Integer emiAmount;

	private String firstName;
	private String lastName;
	private String middleName;
	
	private Integer joiningFees;
	
	private LocalDateTime cancelledDate;
	
	private LocalDateTime transactionDate;
	
	private String gujaratiName;
	
	private String branchNameGuj;
	
	public MemberSummaryDTO(Integer memberNo, String firstName, String middleName, String lastName, 
            String village, MemberStatus status, Double totalFees, 
            Double pendingLoan, Double emiAmount, Double joiningFees, LocalDateTime transactionDate) {
		
		this.transactionDate = transactionDate;
		this.memberNo = memberNo;
		this.firstName = firstName;
		this.middleName = middleName;
		this.lastName = lastName;
		this.fullName = (firstName + " " + (middleName != null ? middleName + " " : "") + lastName).trim();
		this.village = village;
		this.status = status;
		this.totalFees = totalFees != null ? totalFees.intValue() : 0;
		this.pendingLoan = pendingLoan != null ? pendingLoan.intValue() : 0;
		this.emiAmount = emiAmount != null ? emiAmount.intValue() : 0;
		this.joiningFees = joiningFees != null ? joiningFees.intValue() : 0;
		
	}

	public MemberSummaryDTO(Integer memberNo, String firstName, String middleName, String lastName, 
            String village, MemberStatus status, Double totalFees, 
            Double pendingLoan, Double emiAmount, Double joiningFees) {
		
		this.memberNo = memberNo;
		this.firstName = firstName;
		this.middleName = middleName;
		this.lastName = lastName;
		this.fullName = (firstName + " " + (middleName != null ? middleName + " " : "") + lastName).trim();
		this.village = village;
		this.status = status;
		this.totalFees = totalFees != null ? totalFees.intValue() : 0;
		this.pendingLoan = pendingLoan != null ? pendingLoan.intValue() : 0;
		this.emiAmount = emiAmount != null ? emiAmount.intValue() : 0;
		this.joiningFees = joiningFees != null ? joiningFees.intValue() : 0;
		
	}
	
	public MemberSummaryDTO(Integer memberNo, String firstName, String middleName, String lastName, 
            String village, MemberStatus status, Double totalFees, 
            Double pendingLoan, Double emiAmount, String gujName,String branchNameGuj) {
		
		this.memberNo = memberNo;
		this.firstName = firstName;
		this.middleName = middleName;
		this.lastName = lastName;
		this.fullName = (firstName + " " + (middleName != null ? middleName + " " : "") + lastName).trim();
		this.village = village;
		this.status = status;
		this.totalFees = totalFees != null ? totalFees.intValue() : 0;
		this.pendingLoan = pendingLoan != null ? pendingLoan.intValue() : 0;
		this.emiAmount = emiAmount != null ? emiAmount.intValue() : 0;
		this.gujaratiName = gujName;
		this.branchNameGuj = branchNameGuj;
	}
	
	public MemberSummaryDTO(Integer memberNo, String firstName, String middleName, String lastName, 
            String village, MemberStatus status, Double totalFees, 
            Double pendingLoan, Double emiAmount, LocalDateTime cancelledDate) {
		
		this.memberNo = memberNo;
		this.firstName = firstName;
		this.middleName = middleName;
		this.lastName = lastName;
		this.fullName = (firstName + " " + (middleName != null ? middleName + " " : "") + lastName).trim();
		this.village = village;
		this.status = status;
		this.totalFees = totalFees != null ? totalFees.intValue() : 0;
		this.pendingLoan = pendingLoan != null ? pendingLoan.intValue() : 0;
		this.emiAmount = emiAmount != null ? emiAmount.intValue() : 0;
		this.cancelledDate = cancelledDate;
		
	}
	
    public MemberSummaryDTO(Long memberNo, String firstName, String middleName, String lastName, 
                            String gujaratiName, String branchNameGuj, String village, 
                            MemberStatus status, Double totalFeesPaid, 
                            Double pendingLoanAmount, Double emiAmount) {
        this.memberNo = memberNo.intValue();
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.fullName = (firstName + " " + (middleName != null ? middleName + " " : "") + lastName).trim();
        this.gujaratiName = gujaratiName;
        this.branchNameGuj = branchNameGuj;
        this.village = village;
        this.status = status;
        this.totalFees = totalFeesPaid.intValue();
        this.pendingLoan = pendingLoanAmount.intValue();
        this.emiAmount = emiAmount.intValue();
    }

	public LocalDateTime getCancelledDate() {
		return cancelledDate;
	}

	public void setCancelledDate(LocalDateTime cancelledDate) {
		this.cancelledDate = cancelledDate;
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

	public Integer getTotalFees() {
		return totalFees;
	}

	public void setTotalFees(Integer totalFees) {
		this.totalFees = totalFees;
	}

	public Integer getPendingLoan() {
		return pendingLoan;
	}

	public void setPendingLoan(Integer pendingLoan) {
		this.pendingLoan = pendingLoan;
	}

	public Integer getEmiAmount() {
		return emiAmount;
	}

	public void setEmiAmount(Integer emiAmount) {
		this.emiAmount = emiAmount;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getMiddleName() {
		return middleName;
	}

	public void setMiddleName(String middleName) {
		this.middleName = middleName;
	}

	public Integer getJoiningFees() {
		return joiningFees;
	}

	public void setJoiningFees(Integer joiningFees) {
		this.joiningFees = joiningFees;
	}

	public LocalDateTime getTransactionDate() {
		return transactionDate;
	}

	public void setTransactionDate(LocalDateTime transactionDate) {
		this.transactionDate = transactionDate;
	}

	public String getGujaratiName() {
		return gujaratiName;
	}

	public void setGujaratiName(String gujaratiName) {
		this.gujaratiName = gujaratiName;
	}

	public String getBranchNameGuj() {
		return branchNameGuj;
	}

	public void setBranchNameGuj(String branchNameGuj) {
		this.branchNameGuj = branchNameGuj;
	}

	public String getFullGujName() {
		return gujaratiName + (branchNameGuj != null && !branchNameGuj.isEmpty() ? "("+branchNameGuj+")" : "");
	}
}