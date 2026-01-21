package com.badargadh.sahkar.data;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "loan_witnesses")
public class LoanWitness {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "LoanApplicationId")
	private LoanApplication loanApplication;

	@ManyToOne
	@JoinColumn(name = "WitnessMemberId")
	private Member witnessMember; // Must be an active member
	
	@Column(length = 5000)
	private String wintessName;

	private String witnessType; // e.g., "PRIMARY", "SECONDARY"
	
	private LocalDate signedDate;
	
	private String signatureStatus; // PENDING, SIGNED

	public LoanWitness() {
	}

	// Getters and Setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public LoanApplication getLoanApplication() {
		return loanApplication;
	}

	public void setLoanApplication(LoanApplication loanApplication) {
		this.loanApplication = loanApplication;
	}

	public Member getWitnessMember() {
		return witnessMember;
	}

	public void setWitnessMember(Member witnessMember) {
		this.witnessMember = witnessMember;
	}

	public String getWitnessType() {
		return witnessType;
	}

	public void setWitnessType(String witnessType) {
		this.witnessType = witnessType;
	}

	public LocalDate getSignedDate() {
		return signedDate;
	}

	public void setSignedDate(LocalDate signedDate) {
		this.signedDate = signedDate;
	}

	public String getSignatureStatus() {
		return signatureStatus;
	}

	public void setSignatureStatus(String signatureStatus) {
		this.signatureStatus = signatureStatus;
	}

	public String getWintessName() {
		return wintessName;
	}

	public void setWintessName(String wintessName) {
		this.wintessName = wintessName;
	}
	
	
}