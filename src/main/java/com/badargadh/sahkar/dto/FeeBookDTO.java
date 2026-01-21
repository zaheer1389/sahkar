package com.badargadh.sahkar.dto;

import java.util.Arrays;
import java.util.Objects;

public class FeeBookDTO {
	private Integer memberNo;
	private String name;
	private Double openingRunningBalance; // Total fees paid before Jan 1st of selected year
	private Double closingRunningBalance; // Total fees paid including the current year
	private Double[] monthlyFees = new Double[12];

	public FeeBookDTO() {
		Arrays.fill(monthlyFees, 0.0);
		this.openingRunningBalance = 0.0;
		this.closingRunningBalance = 0.0;
	}

	public Integer getMemberNo() {
		return memberNo;
	}

	public void setMemberNo(Integer memberNo) {
		this.memberNo = memberNo;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Double getOpeningRunningBalance() {
		return openingRunningBalance;
	}

	public void setOpeningRunningBalance(Double openingRunningBalance) {
		this.openingRunningBalance = openingRunningBalance;
	}

	public Double getClosingRunningBalance() {
		return closingRunningBalance;
	}

	public void setClosingRunningBalance(Double closingRunningBalance) {
		this.closingRunningBalance = closingRunningBalance;
	}

	public Double[] getMonthlyFees() {
		return monthlyFees;
	}

	public void setMonthlyFees(Double[] monthlyFees) {
		this.monthlyFees = monthlyFees;
	}

}