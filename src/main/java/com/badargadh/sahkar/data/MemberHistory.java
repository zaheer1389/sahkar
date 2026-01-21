package com.badargadh.sahkar.data;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "member_history")
public class MemberHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long originalMemberId; // Link to the actual member record
	private Integer memberNo;
	private String firstName;
	private String middleName;
	private String lastName;
	private Double totalFeesRefunded;
	private LocalDateTime refundDateTime;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getOriginalMemberId() {
		return originalMemberId;
	}

	public void setOriginalMemberId(Long originalMemberId) {
		this.originalMemberId = originalMemberId;
	}

	public Integer getMemberNo() {
		return memberNo;
	}

	public void setMemberNo(Integer memberNo) {
		this.memberNo = memberNo;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getMiddleName() {
		return middleName;
	}

	public void setMiddleName(String middleName) {
		this.middleName = middleName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public Double getTotalFeesRefunded() {
		return totalFeesRefunded;
	}

	public void setTotalFeesRefunded(Double totalFeesRefunded) {
		this.totalFeesRefunded = totalFeesRefunded;
	}

	public LocalDateTime getRefundDateTime() {
		return refundDateTime;
	}

	public void setRefundDateTime(LocalDateTime refundDateTime) {
		this.refundDateTime = refundDateTime;
	}

}