package com.badargadh.sahkar.data;

import java.time.LocalDateTime;

import org.springframework.boot.autoconfigure.security.SecurityProperties.User;

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
@Table(name = "emi_payments")
public class EmiPayment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "LoanAccountId", nullable = false)
	private LoanAccount loanAccount;
	
	@ManyToOne
    @JoinColumn(name = "MemberId", nullable = false)
    private Member member;

	@ManyToOne
	@JoinColumn(name = "FinancialMonthId", nullable = false)
	private FinancialMonth financialMonth;
	
	//The person who deposited
	@ManyToOne
    @JoinColumn(name = "DipositedByMemberId", nullable = true)
    private Member dipositedBy;
	
	@ManyToOne
    @JoinColumn(name = "AddedByUserId", nullable = true)
    private AppUser addedBy;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "CollectionLocation", length = 5000) // Explicitly set length
	private CollectionLocation collectionLocation;

	@ManyToOne
    @JoinColumn(name = "payment_group_id")
    private EmiPaymentGroup paymentGroup;
	
	private Double amountPaid;
	
	private boolean fullPayment;
	
	private Double fullPaymentAmount;
	
	private LocalDateTime paymentDateTime;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public LoanAccount getLoanAccount() {
		return loanAccount;
	}

	public void setLoanAccount(LoanAccount loanAccount) {
		this.loanAccount = loanAccount;
	}

	public Member getMember() {
		return member;
	}

	public void setMember(Member member) {
		this.member = member;
	}

	public FinancialMonth getFinancialMonth() {
		return financialMonth;
	}

	public void setFinancialMonth(FinancialMonth financialMonth) {
		this.financialMonth = financialMonth;
	}

	public Member getDipositedBy() {
		return dipositedBy;
	}

	public void setDipositedBy(Member dipositedBy) {
		this.dipositedBy = dipositedBy;
	}

	public Double getAmountPaid() {
		return amountPaid;
	}

	public void setAmountPaid(Double amountPaid) {
		this.amountPaid = amountPaid;
	}

	public boolean isFullPayment() {
		return fullPayment;
	}

	public void setFullPayment(boolean fullPayment) {
		this.fullPayment = fullPayment;
	}

	public Double getFullPaymentAmount() {
		return fullPaymentAmount;
	}

	public void setFullPaymentAmount(Double fullPaymentAmount) {
		this.fullPaymentAmount = fullPaymentAmount;
	}

	public LocalDateTime getPaymentDateTime() {
		return paymentDateTime;
	}

	public void setPaymentDateTime(LocalDateTime paymentDateTime) {
		this.paymentDateTime = paymentDateTime;
	}

	public AppUser getAddedBy() {
		return addedBy;
	}

	public void setAddedBy(AppUser addedBy) {
		this.addedBy = addedBy;
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