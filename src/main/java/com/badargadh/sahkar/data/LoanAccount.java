package com.badargadh.sahkar.data;

import java.time.LocalDate;

import com.badargadh.sahkar.enums.LoanStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "loan_accounts")
public class LoanAccount {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "MemberId")
	private Member member;
	
	@OneToOne
	@JoinColumn(name = "LoanApplicationId")
	private LoanApplication loanApplication;
	
	@ManyToOne
	@JoinColumn(name = "FinancialMonthId")
	private FinancialMonth financialMonth;

	private Double grantedAmount; // The 100,000 limit
	private Double pendingAmount; // Remaining balance
	private Double emiAmount; // 1000, 2000, 3000, or 4000
	private LocalDate startDate; // Disbursement month + 1
	private LocalDate endDate; // Calculated based on EMI
	private LocalDate createdDate; // Disbursement month date
	
	@Enumerated(EnumType.STRING)
	@Column(name = "LoanStatus", length = 50) // Explicitly set length
	private LoanStatus loanStatus;

	public LoanAccount() {
	}
	
	// Logic: Is the EMI locked?
    public boolean isEmiLocked() {
        return this.emiAmount != null && this.emiAmount > 0;
    }

	// Getters and Setters ...
	public Double getGrantedAmount() {
		return grantedAmount;
	}

	public void setGrantedAmount(Double grantedAmount) {
		this.grantedAmount = grantedAmount;
	}

	public Double getPendingAmount() {
		return pendingAmount;
	}

	public void setPendingAmount(Double pendingAmount) {
		this.pendingAmount = pendingAmount;
	}

	public Double getEmiAmount() {
		return emiAmount;
	}

	public void setEmiAmount(Double emiAmount) {
		this.emiAmount = emiAmount;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public void setStartDate(LocalDate startDate) {
		this.startDate = startDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public void setEndDate(LocalDate endDate) {
		this.endDate = endDate;
	}

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

	public LoanStatus getLoanStatus() {
		return loanStatus;
	}

	public void setLoanStatus(LoanStatus loanStatus) {
		this.loanStatus = loanStatus;
	}

	public LoanApplication getLoanApplication() {
		return loanApplication;
	}

	public void setLoanApplication(LoanApplication loanApplication) {
		this.loanApplication = loanApplication;
	}

	public FinancialMonth getFinancialMonth() {
		return financialMonth;
	}

	public void setFinancialMonth(FinancialMonth financialMonth) {
		this.financialMonth = financialMonth;
	}

	public LocalDate getCreatedDate() {
		return createdDate != null ? createdDate : 
			(getStartDate() != null 
				? getStartDate().minusMonths(1) 
				: getFinancialMonth().getStartDate().plusDays(10));
	}

	public void setCreatedDate(LocalDate createdDate) {
		this.createdDate = createdDate;
	}

	
}