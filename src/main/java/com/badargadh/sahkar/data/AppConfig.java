package com.badargadh.sahkar.data;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_settings")
public class AppConfig {

	@Id
	private Long id = 1L; // Singleton record

	private Long monthlyFees = 20L;
	private Long newMemberFees = 300L;
	private Long loanAmount = 10000L;
	private Integer newMemberCoolingPeriod = 30; // months
	private Integer feesRefundCoolingPeriod = 3; // months
	private Double openingBal;
	private Double openingBalExpenses;
	private Double openingBalJammat;
	private boolean autoRemark;
	
	private int remarkDateOfMonth = 11; // Default 11th
    private int remarkTimeHour = 17; // Default 5 PM
    private int remarkTimeMinute = 0; // Default :00
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getMonthlyFees() {
		return monthlyFees;
	}

	public void setMonthlyFees(Long monthlyFees) {
		this.monthlyFees = monthlyFees;
	}

	public Long getNewMemberFees() {
		return newMemberFees;
	}

	public void setNewMemberFees(Long newMemberFees) {
		this.newMemberFees = newMemberFees;
	}

	public Long getLoanAmount() {
		return loanAmount;
	}

	public void setLoanAmount(Long loanAmount) {
		this.loanAmount = loanAmount;
	}

	public Integer getNewMemberCoolingPeriod() {
		return newMemberCoolingPeriod;
	}

	public void setNewMemberCoolingPeriod(Integer newMemberCoolingPeriod) {
		this.newMemberCoolingPeriod = newMemberCoolingPeriod;
	}

	public Integer getFeesRefundCoolingPeriod() {
		return feesRefundCoolingPeriod;
	}

	public void setFeesRefundCoolingPeriod(Integer feesRefundCoolingPeriod) {
		this.feesRefundCoolingPeriod = feesRefundCoolingPeriod;
	}

	public Double getOpeningBal() {
		return openingBal;
	}

	public void setOpeningBal(Double openingBal) {
		this.openingBal = openingBal;
	}

	public boolean isAutoRemark() {
		return autoRemark;
	}

	public void setAutoRemark(boolean autoRemark) {
		this.autoRemark = autoRemark;
	}

	public int getRemarkDateOfMonth() {
		return remarkDateOfMonth;
	}

	public void setRemarkDateOfMonth(int remarkDateOfMonth) {
		this.remarkDateOfMonth = remarkDateOfMonth;
	}

	public int getRemarkTimeHour() {
		return remarkTimeHour;
	}

	public void setRemarkTimeHour(int remarkTimeHour) {
		this.remarkTimeHour = remarkTimeHour;
	}

	public int getRemarkTimeMinute() {
		return remarkTimeMinute;
	}

	public void setRemarkTimeMinute(int remarkTimeMinute) {
		this.remarkTimeMinute = remarkTimeMinute;
	}

	public Double getOpeningBalExpenses() {
		return openingBalExpenses;
	}

	public void setOpeningBalExpenses(Double openingBalExpenses) {
		this.openingBalExpenses = openingBalExpenses;
	}

	public Double getOpeningBalJammat() {
		return openingBalJammat;
	}

	public void setOpeningBalJammat(Double openingBalJammat) {
		this.openingBalJammat = openingBalJammat;
	}
}