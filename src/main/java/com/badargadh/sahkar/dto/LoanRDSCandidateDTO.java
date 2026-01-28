package com.badargadh.sahkar.dto;

import java.time.LocalDateTime;

import com.badargadh.sahkar.data.LoanApplication;
import com.badargadh.sahkar.data.Member;

public class LoanRDSCandidateDTO {

	private Member member;
	private LoanApplication application;
	private LocalDateTime selectionDateTime;
	
	public LoanRDSCandidateDTO(Member member, LoanApplication application, LocalDateTime selectionDateTime) {
		this.member = member;
		this.application = application;
		this.selectionDateTime = selectionDateTime;
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

	public LocalDateTime getSelectionDateTime() {
		return selectionDateTime;
	}

	public void setSelectionDateTime(LocalDateTime selectionDateTime) {
		this.selectionDateTime = selectionDateTime;
	}

	
}
