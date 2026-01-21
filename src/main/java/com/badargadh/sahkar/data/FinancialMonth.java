package com.badargadh.sahkar.data;

import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;

import com.badargadh.sahkar.enums.MonthStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "financial_months")
public class FinancialMonth {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true, nullable = true)
	private String monthId;

	private String monthName; // e.g., "MARCH"
	private int year;

	private Double openingBalance = 0.0;
	private Double closingBalance = 0.0;

	@Enumerated(EnumType.STRING)
	private MonthStatus status; // OPEN, CLOSED, BLANK

	private LocalDate startDate;
	private LocalDate endDate;
	
	@Column(length = 5000) // Allow for long descriptions
    private String closingRemarks;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getMonthId() {
		return monthId;
	}

	public void setMonthId(String monthId) {
		this.monthId = monthId;
	}

	public String getMonthName() {
		return monthName;
	}

	public void setMonthName(String monthName) {
		this.monthName = monthName;
	}

	public int getYear() {
		return year;
	}

	public void setYear(int year) {
		this.year = year;
	}

	public Double getOpeningBalance() {
		return openingBalance;// > 0 ? openingBalance : 0;
	}

	public void setOpeningBalance(Double openingBalance) {
		this.openingBalance = openingBalance;
	}

	public Double getClosingBalance() {
		return closingBalance;// > 0 ? closingBalance : 0;
	}

	public void setClosingBalance(Double closingBalance) {
		this.closingBalance = closingBalance;
	}

	public MonthStatus getStatus() {
		return status;
	}

	public void setStatus(MonthStatus status) {
		this.status = status;
	}

	public LocalDate getStartDate() {
		return LocalDate.of(year, Month.valueOf(monthName), 1);
	}

	public void setStartDate(LocalDate startDate) {
		this.startDate = startDate;
	}

	public LocalDate getEndDate() {
		return YearMonth.from(startDate).atEndOfMonth();
	}

	public void setEndDate(LocalDate endDate) {
		this.endDate = endDate;
	}

	public String getClosingRemarks() {
		return closingRemarks;
	}

	public void setClosingRemarks(String closingRemarks) {
		this.closingRemarks = closingRemarks;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return monthId;
	}
	
}
