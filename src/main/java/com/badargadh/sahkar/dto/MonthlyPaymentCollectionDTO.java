package com.badargadh.sahkar.dto;

import com.badargadh.sahkar.enums.CollectionLocation;
import com.badargadh.sahkar.enums.MemberStatus;
import java.time.LocalDateTime;

public class MonthlyPaymentCollectionDTO {
	private Integer memberNo;
	private String firstName;
	private String middleName;
	private String lastName;
	private String village;
	private MemberStatus status;
	private Double monthlyFeePaid;
	private Double loanPendingAmount;
	private Double emiPaid;
	private Double joiningFeePaid;
	private LocalDateTime latestTransactionDate;
	private String fullName;
	private CollectionLocation collectionLocation;
	private Long paymentGroupId;
	
	// Constructor for JPQL 'new' projection
	public MonthlyPaymentCollectionDTO(Integer memberNo, String firstName, String middleName, String lastName,
			String village, MemberStatus status, Double monthlyFeePaid, Double loanPendingAmount, Double emiPaid,
			Double joiningFeePaid, LocalDateTime latestTransactionDate, CollectionLocation collectionLocation,
			Long paymentGroupId) {
		this.memberNo = memberNo;
		this.firstName = firstName;
		this.middleName = middleName;
		this.lastName = lastName;
		this.village = village;
		this.status = status;
		this.monthlyFeePaid = monthlyFeePaid;
		this.loanPendingAmount = loanPendingAmount;
		this.emiPaid = emiPaid;
		this.joiningFeePaid = joiningFeePaid;
		this.latestTransactionDate = latestTransactionDate;
		this.collectionLocation = collectionLocation;
		this.paymentGroupId = paymentGroupId;
		
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

	public Double getMonthlyFeePaid() {
		return monthlyFeePaid;
	}

	public void setMonthlyFeePaid(Double monthlyFeePaid) {
		this.monthlyFeePaid = monthlyFeePaid;
	}

	public Double getLoanPendingAmount() {
		return loanPendingAmount;
	}

	public void setLoanPendingAmount(Double loanPendingAmount) {
		this.loanPendingAmount = loanPendingAmount;
	}

	public Double getEmiPaid() {
		return emiPaid;
	}

	public void setEmiPaid(Double emiPaid) {
		this.emiPaid = emiPaid;
	}

	public Double getJoiningFeePaid() {
		return joiningFeePaid;
	}

	public void setJoiningFeePaid(Double joiningFeePaid) {
		this.joiningFeePaid = joiningFeePaid;
	}

	public LocalDateTime getLatestTransactionDate() {
		return latestTransactionDate;
	}

	public void setLatestTransactionDate(LocalDateTime latestTransactionDate) {
		this.latestTransactionDate = latestTransactionDate;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getFullName() {
		return fullName;
	}
	
	public CollectionLocation getCollectionLocation() {
		return collectionLocation;
	}

	public void setCollectionLocation(CollectionLocation collectionLocation) {
		this.collectionLocation = collectionLocation;
	}
	
	public Long getPaymentGroupId() {
		return paymentGroupId;
	}

	public void setPaymentGroupId(Long paymentGroupId) {
		this.paymentGroupId = paymentGroupId;
	}

	public Double getTotalCollection() {
		return (monthlyFeePaid != null ? monthlyFeePaid : 0.0) + (emiPaid != null ? emiPaid : 0.0)
				+ (joiningFeePaid != null ? joiningFeePaid : 0.0);
	}
}