package com.badargadh.sahkar.dto;

import java.time.LocalDate;
import java.util.List;

public class LoanWitnessRemarkDTO {
	int memberNo;
	int witnessNo;
	String type;
	List<LocalDate> remarks;

	public int getMemberNo() {
		return memberNo;
	}

	public void setMemberNo(int memberNo) {
		this.memberNo = memberNo;
	}

	public int getWitnessNo() {
		return witnessNo;
	}

	public void setWitnessNo(int witnessNo) {
		this.witnessNo = witnessNo;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public List<LocalDate> getRemarks() {
		return remarks;
	}

	public void setRemarks(List<LocalDate> remarks) {
		this.remarks = remarks;
	}

}
