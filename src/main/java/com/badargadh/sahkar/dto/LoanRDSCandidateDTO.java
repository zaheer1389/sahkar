package com.badargadh.sahkar.dto;

import com.badargadh.sahkar.data.LoanApplication;
import com.badargadh.sahkar.data.Member;

public class LoanRDSCandidateDTO {

	private Member member;
	private LoanApplication application;
	
	public LoanRDSCandidateDTO(Member member, LoanApplication application) {
		this.member = member;
		this.application = application;
	}

	public Member getMember() {
		return member;
	}

	public void setMember(Member member) {
		this.member = member;
	}

	public LoanApplication getApplication() {
		return application;
	}

	public void setApplication(LoanApplication application) {
		this.application = application;
	}

}
