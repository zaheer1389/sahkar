package com.badargadh.sahkar.data;

import java.time.LocalDateTime;

import com.badargadh.sahkar.enums.CancellationReason;
import com.badargadh.sahkar.enums.MemberStatus;

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
@Table(name = "members")
public class Member {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// Set nullable = true so we can clear it to "free" the number
    @Column(unique = true, nullable = true) 
    private Integer memberNo;

	private String firstName;
	private String middleName;
	private String lastName;
	private String village;
	
	private String firstNameGuj;
	private String middleNameGuj;
	private String lastNameGuj;
	
	@Column(length = 500)
	private String branchNameGuj;

	@Enumerated(EnumType.STRING)
	@Column(name = "MemberStatus", length = 50) // Explicitly set length
	private MemberStatus status;
	
	@Enumerated(EnumType.STRING)
	private CancellationReason cancellationReason;
	
	private LocalDateTime joiningDateTime;

	private LocalDateTime cancellationDateTime;
	
	@ManyToOne
	@JoinColumn(name = "FinancialMonthId")
	private FinancialMonth financialMonth;
	
	@Column(name = "CancellationRemarks", length = 5000) // Explicitly set length
	private String cancellationRemarks;

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

	public String getVillage() {
		return village;
	}

	public void setVillage(String village) {
		this.village = village;
	}

	public MemberStatus getStatus() {
		return status;
	}

	public void setStatus(MemberStatus status) {
		this.status = status;
	}

	public LocalDateTime getJoiningDateTime() {
		return joiningDateTime;
	}

	public void setJoiningDateTime(LocalDateTime joiningDateTime) {
		this.joiningDateTime = joiningDateTime;
	}

	public LocalDateTime getCancellationDateTime() {
		return cancellationDateTime;
	}

	public void setCancellationDateTime(LocalDateTime cancellationDateTime) {
		this.cancellationDateTime = cancellationDateTime;
	}
	
	public String getFullname() {
		return firstName+" "+middleName+" "+lastName;
	}

	public String getGujFullname() {
		return firstNameGuj + " " + middleNameGuj + " " + lastNameGuj  + (branchNameGuj != null && branchNameGuj.length() > 0 ? " ("+ branchNameGuj+")" : "");
	}

	public FinancialMonth getFinancialMonth() {
		return financialMonth;
	}

	public void setFinancialMonth(FinancialMonth financialMonth) {
		this.financialMonth = financialMonth;
	}
	
	public CancellationReason getCancellationReason() {
	    return cancellationReason;
	}

	public void setCancellationReason(CancellationReason cancellationReason) {
	    this.cancellationReason = cancellationReason;
	}

	public String getFirstNameGuj() {
		return firstNameGuj;
	}

	public void setFirstNameGuj(String firstNameGuj) {
		this.firstNameGuj = firstNameGuj;
	}

	public String getMiddleNameGuj() {
		return middleNameGuj;
	}

	public void setMiddleNameGuj(String middleNameGuj) {
		this.middleNameGuj = middleNameGuj;
	}

	public String getLastNameGuj() {
		return lastNameGuj;
	}

	public void setLastNameGuj(String lastNameGuj) {
		this.lastNameGuj = lastNameGuj;
	}

	public String getBranchNameGuj() {
		return branchNameGuj;
	}

	public void setBranchNameGuj(String branchNameGuj) {
		this.branchNameGuj = branchNameGuj;
	}

	public String getCancellationRemarks() {
		return cancellationRemarks;
	}

	public void setCancellationRemarks(String cancellationRemarks) {
		this.cancellationRemarks = cancellationRemarks;
	}
	
	
}