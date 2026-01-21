package com.badargadh.sahkar.data;

import java.time.LocalDate;

import com.badargadh.sahkar.enums.RemarkType;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class PaymentRemark {
	
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    private Member member;
    
    @Enumerated(EnumType.STRING)
    private RemarkType remarkType; // "LATE_FEE" or "LATE_EMI"
    
    private LocalDate issuedDate;
    
    private boolean isCleared = false; // Becomes true after Loan/Refund
    
    @ManyToOne
    private FinancialMonth financialMonth;
    
    private String clearingReason;
    
    private LocalDate clearedDate;

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

	public RemarkType getRemarkType() {
		return remarkType;
	}

	public void setRemarkType(RemarkType remarkType) {
		this.remarkType = remarkType;
	}

	public LocalDate getIssuedDate() {
		return issuedDate;
	}

	public void setIssuedDate(LocalDate issuedDate) {
		this.issuedDate = issuedDate;
	}

	public boolean isCleared() {
		return isCleared;
	}

	public void setCleared(boolean isCleared) {
		this.isCleared = isCleared;
	}

	public FinancialMonth getFinancialMonth() {
		return financialMonth;
	}

	public void setFinancialMonth(FinancialMonth financialMonth) {
		this.financialMonth = financialMonth;
	}

	public String getClearingReason() {
		return clearingReason;
	}

	public void setClearingReason(String clearingReason) {
		this.clearingReason = clearingReason;
	}

	public LocalDate getClearedDate() {
		return clearedDate;
	}

	public void setClearedDate(LocalDate clearedDate) {
		this.clearedDate = clearedDate;
	}
    
    
}