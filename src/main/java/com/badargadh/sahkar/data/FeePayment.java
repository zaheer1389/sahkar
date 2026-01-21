package com.badargadh.sahkar.data;

import java.time.LocalDateTime;

import com.badargadh.sahkar.enums.CollectionLocation;
import com.badargadh.sahkar.enums.FeeType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "fee_payments")
public class FeePayment {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "MemberId")
	private Member member;

	@ManyToOne
    @JoinColumn(name = "DipositedByMemberId", nullable = true)
    private Member dipositedBy;
	
	@ManyToOne
    @JoinColumn(name = "AddedByUserId", nullable = true)
    private AppUser addedBy;

	private Double amount;

	@Enumerated(EnumType.STRING)
	@Column(name = "FeeType", length = 50) // Explicitly set length
	private FeeType feeType; // JOINING_FEE, MONTHLY_FEE, LOAN_DEDUCTION, REFUND
	
	@Enumerated(EnumType.STRING)
	@Column(name = "CollectionLocation", length = 5000) // Explicitly set length
	private CollectionLocation collectionLocation;
	
	@ManyToOne
    @JoinColumn(name = "payment_group_id")
    private EmiPaymentGroup paymentGroup;

	private Double runningBalance; // Cumulative total of all fees paid to date

	@ManyToOne
	@JoinColumn(name = "FinancialMonthId")
	private FinancialMonth financialMonth;
	
	private LocalDateTime transactionDateTime;
	
	private String remarks;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Member getMember() {
		return member;
	}

	public void setMember(Member member) {
		this.member = member;
	}

	public Member getDipositedBy() {
		return dipositedBy;
	}

	public void setDipositedBy(Member dipositedBy) {
		this.dipositedBy = dipositedBy;
	}

	public AppUser getAddedBy() {
		return addedBy;
	}

	public void setAddedBy(AppUser addedBy) {
		this.addedBy = addedBy;
	}

	public Double getAmount() {
		return amount;
	}

	public void setAmount(Double amount) {
		this.amount = amount;
	}

	public FeeType getFeeType() {
		return feeType;
	}

	public void setFeeType(FeeType feeType) {
		this.feeType = feeType;
	}

	public Double getRunningBalance() {
		return runningBalance;
	}

	public void setRunningBalance(Double runningBalance) {
		this.runningBalance = runningBalance;
	}

	public FinancialMonth getFinancialMonth() {
		return financialMonth;
	}

	public void setFinancialMonth(FinancialMonth financialMonth) {
		this.financialMonth = financialMonth;
	}

	public LocalDateTime getTransactionDateTime() {
		return transactionDateTime;
	}

	public void setTransactionDateTime(LocalDateTime transactionDateTime) {
		this.transactionDateTime = transactionDateTime;
	}

	public String getRemarks() {
		return remarks;
	}

	public void setRemarks(String remarks) {
		this.remarks = remarks;
	}

	public CollectionLocation getCollectionLocation() {
		return collectionLocation;
	}

	public void setCollectionLocation(CollectionLocation collectionLocation) {
		this.collectionLocation = collectionLocation;
	}

	public EmiPaymentGroup getPaymentGroup() {
		return paymentGroup;
	}

	public void setPaymentGroup(EmiPaymentGroup paymentGroup) {
		this.paymentGroup = paymentGroup;
	}

	
	
}