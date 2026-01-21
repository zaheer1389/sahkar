package com.badargadh.sahkar.data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.badargadh.sahkar.enums.CollectionType;
import com.badargadh.sahkar.enums.LoanApplicationStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "loan_applications")
public class LoanApplication {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "MemberId")
	private Member member;
	
	@ManyToOne
	@JoinColumn(name = "FinancialMonthId")
	private FinancialMonth financialMonth;

	private Double appliedAmount;

	private String applicationNumber;
	
	private String rejectionReason;
	
	@Column(name = "is_surplus_notice_app")
	private boolean surplusNoticeApp = false; // Default is false (Regular)
	
	@Column(name = "draw_rank", length = 10)
	private String drawRank; // Stores "SL-01", "WL-05", etc.

	@Enumerated(EnumType.STRING)
	@Column(name = "LoanApplicationStatus", length = 50) // Explicitly set length
	private LoanApplicationStatus status; // APPLIED, SELECTED_IN_DRAW, DISBURSED

	// Relationship to the new Witness table
	@OneToMany(mappedBy = "loanApplication", cascade = CascadeType.ALL)
	private List<LoanWitness> witnesses = new ArrayList<>();

	private LocalDateTime applicationDateTime;
	
	private Double prevLoanFullAmountDeduction;
	
	private Double feesDeduction;

	@ManyToOne
	@JoinColumn(name = "disbursed_by_id")
	private Member disbursedBy; // The Staff who disbursed cash

	@Enumerated(EnumType.STRING)
	private CollectionType collectionType; // SELF, RELATIVE

	@ManyToOne
	@JoinColumn(name = "received_by_id")
	private Member receivedBy; // The person who took the cash

	private String collectionRemarks;
	private LocalDateTime disbursementDateTime;

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

	public Double getAppliedAmount() {
		return appliedAmount;
	}

	public void setAppliedAmount(Double appliedAmount) {
		this.appliedAmount = appliedAmount;
	}

	public String getApplicationNumber() {
		return applicationNumber;
	}

	public void setApplicationNumber(String applicationNumber) {
		this.applicationNumber = applicationNumber;
	}

	public LoanApplicationStatus getStatus() {
		return status;
	}

	public void setStatus(LoanApplicationStatus status) {
		this.status = status;
	}

	public List<LoanWitness> getWitnesses() {
		return witnesses;
	}

	public void setWitnesses(List<LoanWitness> witnesses) {
		this.witnesses = witnesses;
	}

	public LocalDateTime getApplicationDateTime() {
		return applicationDateTime;
	}

	public void setApplicationDateTime(LocalDateTime applicationDateTime) {
		this.applicationDateTime = applicationDateTime;
	}

	public Member getDisbursedBy() {
		return disbursedBy;
	}

	public void setDisbursedBy(Member disbursedBy) {
		this.disbursedBy = disbursedBy;
	}

	public CollectionType getCollectionType() {
		return collectionType;
	}

	public void setCollectionType(CollectionType collectionType) {
		this.collectionType = collectionType;
	}

	public Member getReceivedBy() {
		return receivedBy;
	}

	public void setReceivedBy(Member receivedBy) {
		this.receivedBy = receivedBy;
	}

	public String getCollectionRemarks() {
		return collectionRemarks;
	}

	public void setCollectionRemarks(String collectionRemarks) {
		this.collectionRemarks = collectionRemarks;
	}

	public LocalDateTime getDisbursementDateTime() {
		return disbursementDateTime;
	}

	public void setDisbursementDateTime(LocalDateTime disbursementDateTime) {
		this.disbursementDateTime = disbursementDateTime;
	}

	public FinancialMonth getFinancialMonth() {
		return financialMonth;
	}

	public void setFinancialMonth(FinancialMonth financialMonth) {
		this.financialMonth = financialMonth;
	}

	public String getRejectionReason() {
		return rejectionReason;
	}

	public void setRejectionReason(String rejectionReason) {
		this.rejectionReason = rejectionReason;
	}

	public String getDrawRank() {
		return drawRank;
	}

	public void setDrawRank(String drawRank) {
		this.drawRank = drawRank;
	}

	public Double getPrevLoanFullAmountDeduction() {
		return prevLoanFullAmountDeduction != null ? prevLoanFullAmountDeduction : 0;
	}

	public void setPrevLoanFullAmountDeduction(Double prevLoanFullAmountDeduction) {
		this.prevLoanFullAmountDeduction = prevLoanFullAmountDeduction;
	}

	public Double getFeesDeduction() {
		return feesDeduction != null ? feesDeduction : 0;
	}

	public void setFeesDeduction(Double feesDeduction) {
		this.feesDeduction = feesDeduction;
	}

	public boolean isSurplusNoticeApp() {
		return surplusNoticeApp;
	}

	public void setSurplusNoticeApp(boolean surplusNoticeApp) {
		this.surplusNoticeApp = surplusNoticeApp;
	}
	
	
}