package com.badargadh.sahkar.data;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "fees_refunds")
public class FeesRefund {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "MemberId")
	private Member member;

	// The person who took refund cash
	@ManyToOne
	@JoinColumn(name = "ProcessedByMemberId", nullable = true)
	private Member processedBy;

	private Double amount;

	@ManyToOne
	@JoinColumn(name = "FinancialMonthId", nullable = false)
	private FinancialMonth financialMonth;

	private LocalDateTime refundDateTime;

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

	public Member getProcessedBy() {
		return processedBy;
	}

	public void setProcessedBy(Member processedBy) {
		this.processedBy = processedBy;
	}

	public Double getAmount() {
		return amount;
	}

	public void setAmount(Double amount) {
		this.amount = amount;
	}

	public FinancialMonth getFinancialMonth() {
		return financialMonth;
	}

	public void setFinancialMonth(FinancialMonth financialMonth) {
		this.financialMonth = financialMonth;
	}

	public LocalDateTime getRefundDateTime() {
		return refundDateTime;
	}

	public void setRefundDateTime(LocalDateTime refundDateTime) {
		this.refundDateTime = refundDateTime;
	}

	public String getRemarks() {
		return remarks;
	}

	public void setRemarks(String remarks) {
		this.remarks = remarks;
	}

}