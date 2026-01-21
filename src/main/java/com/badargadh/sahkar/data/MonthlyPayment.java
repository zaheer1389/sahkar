package com.badargadh.sahkar.data;

import java.time.LocalDateTime;

public class MonthlyPayment {

	private Long id;
	private Member member;
	private Integer emiAmount = 0;
	private Integer fullAmount = 0;
	private Integer monthlyFees = 0;
	private boolean fullPayment;
	private boolean remarkAdded;
	private LocalDateTime emiDate;
	private Integer balanceAmount = 0;
	private String name;
	private String memberNo;

	public Member getMember() {
		return member;
	}

	public void setMember(Member member) {
		this.member = member;
	}

	public Integer getEmiAmount() {
		return emiAmount != null ? emiAmount : 0;
	}

	public void setEmiAmount(Integer emiAmount) {
		this.emiAmount = emiAmount;
	}

	public Integer getFullAmount() {
		return fullAmount != null ? fullAmount : 0;
	}

	public void setFullAmount(Integer fullAmount) {
		this.fullAmount = fullAmount;
	}

	public Integer getMonthlyFees() {
		return monthlyFees != null ? monthlyFees : 0;
	}

	public void setMonthlyFees(Integer monthlyFees) {
		this.monthlyFees = monthlyFees;
	}

	public boolean isFullPayment() {
		return fullPayment;
	}

	public void setFullPayment(boolean fullPayment) {
		this.fullPayment = fullPayment;
	}

	public boolean isRemarkAdded() {
		return remarkAdded;
	}

	public void setRemarkAdded(boolean remarkAdded) {
		this.remarkAdded = remarkAdded;
	}

	public LocalDateTime getEmiDate() {
		return emiDate;
	}

	public void setEmiDate(LocalDateTime emiDate) {
		this.emiDate = emiDate;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Integer getBalanceAmount() {
		return balanceAmount;
	}

	public void setBalanceAmount(Integer balanceAmount) {
		this.balanceAmount = balanceAmount;
	}

	public String getName() {
		return member.getFullname();
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getMemberNo() {
		return member.getMemberNo()+"";
	}

	public void setMemberNo(String memberNo) {
		this.memberNo = memberNo;
	}

	
	
}
