package com.badargadh.sahkar.data;

import java.time.LocalDate;

public class MemberFeesRefundDTO {
	
	private Member member;
	private Double feesRefundAmount;
	private boolean refundEligible;
	private String refundBlockedReason;
	private LocalDate finalRefundDate;
	private Integer memberNo;
	
	public MemberFeesRefundDTO() {
		// TODO Auto-generated constructor stub
	}

	public Member getMember() {
		return member;
	}

	public void setMember(Member member) {
		this.member = member;
	}

	public Double getFeesRefundAmount() {
		return feesRefundAmount;
	}

	public void setFeesRefundAmount(Double feesRefundAmount) {
		this.feesRefundAmount = feesRefundAmount;
	}

	public boolean isRefundEligible() {
		return refundEligible;
	}

	public void setRefundEligible(boolean refundEligible) {
		this.refundEligible = refundEligible;
	}

	public String getRefundBlockedReason() {
		return refundBlockedReason;
	}

	public void setRefundBlockedReason(String refundBlockedReason) {
		this.refundBlockedReason = refundBlockedReason;
	}

	public LocalDate getFinalRefundDate() {
		return finalRefundDate;
	}

	public void setFinalRefundDate(LocalDate finalRefundDate) {
		this.finalRefundDate = finalRefundDate;
	}

	public Integer getMemberNo() {
		return memberNo;
	}

	public void setMemberNo(Integer memberNo) {
		this.memberNo = memberNo;
	}
	
	

}
