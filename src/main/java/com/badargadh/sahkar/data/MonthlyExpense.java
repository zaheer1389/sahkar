package com.badargadh.sahkar.data;

import java.time.LocalDate;

import com.badargadh.sahkar.enums.ExpenseCategory;
import com.badargadh.sahkar.enums.ExpenseType;

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
@Table(name = "monthly_expenses")
public class MonthlyExpense {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	private LocalDate date;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "category", length = 5000) // Match this with your ALTER TABLE command
	private ExpenseCategory category; // e.g., Stationery, Tea, Interest Income
	
	private Double amount;
	
	@Enumerated(EnumType.STRING)
	private ExpenseType type;
	
	@Column(name = "remarks", length = 5000) // Match this with your ALTER TABLE command
	private String remarks;

	@ManyToOne
	@JoinColumn(name = "FinancialMonthId")
	private FinancialMonth financialMonth;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public LocalDate getDate() {
		return date;
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

	public ExpenseCategory getCategory() {
		return category;
	}

	public void setCategory(ExpenseCategory category) {
		this.category = category;
	}

	public Double getAmount() {
		return amount;
	}

	public void setAmount(Double amount) {
		this.amount = amount;
	}

	public ExpenseType getType() {
		return type;
	}

	public void setType(ExpenseType type) {
		this.type = type;
	}

	public String getRemarks() {
		return remarks;
	}

	public void setRemarks(String remarks) {
		this.remarks = remarks;
	}

	public FinancialMonth getFinancialMonth() {
		return financialMonth;
	}

	public void setFinancialMonth(FinancialMonth financialMonth) {
		this.financialMonth = financialMonth;
	}

	
	
}