package com.badargadh.sahkar.data;

import java.time.LocalDateTime;

import com.badargadh.sahkar.enums.ProofType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "authorization_proofs")
public class AuthorizationProof {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Integer memberNo;
	private String filePath;
	private LocalDateTime uploadDate;
	private String checksum;

	@Enumerated(EnumType.STRING)
	@Column(name = "ProofType", length = 100) // Explicitly set length
	private ProofType proofType;
	
	@ManyToOne
	@JoinColumn(name = "FinancialMonthId")
	private FinancialMonth financialMonth;


	public AuthorizationProof(Integer memberNo, String filePath, ProofType proofType) {
		this.memberNo = memberNo;
		this.filePath = filePath;
		this.proofType = proofType;
		this.uploadDate = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Integer getMemberNo() {
		return memberNo;
	}

	public void setMemberNo(Integer memberNo) {
		this.memberNo = memberNo;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public LocalDateTime getUploadDate() {
		return uploadDate;
	}

	public void setUploadDate(LocalDateTime uploadDate) {
		this.uploadDate = uploadDate;
	}

	public String getChecksum() {
		return checksum;
	}

	public void setChecksum(String checksum) {
		this.checksum = checksum;
	}

	public ProofType getProofType() {
		return proofType;
	}

	public void setProofType(ProofType proofType) {
		this.proofType = proofType;
	}

	public FinancialMonth getFinancialMonth() {
		return financialMonth;
	}

	public void setFinancialMonth(FinancialMonth financialMonth) {
		this.financialMonth = financialMonth;
	}

	
}
