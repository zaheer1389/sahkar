package com.badargadh.sahkar.dto;

import java.time.LocalDateTime;
import java.util.Map;

import com.badargadh.sahkar.enums.MemberStatus;

public class MemberSummaryDTO {
	private Integer memberNo;
	private String fullName; // Single field for name
	private String village;
	private MemberStatus status;
	
	private Integer totalFees;
    private Integer pendingLoan;
    private Integer emiAmount;

	private String firstName;
	private String lastName;
	private String middleName;
	private String branchName;
	
	private Integer joiningFees;
	
	private LocalDateTime cancelledDate;
	
	private LocalDateTime transactionDate;
	
	private String gujaratiName;
	private String firstNameGuj;
	private String lastNameGuj;
	private String middleNameGuj;
	private String branchNameGuj;
	
	private String rowColor = "#FFFFFF"; // Default White
	
	public MemberSummaryDTO(
            Integer memberNo, 
            String firstName, 
            String middleName, 
            String lastName, 
            String branchName,   // Argument 5
            String village,      // Argument 6
            MemberStatus status, // Argument 7
            Double totalFees,    // Argument 8 (Subquery result)
            Double pendingLoan,  // Argument 9 (Subquery result)
            Double emiAmount,    // Argument 10 (Subquery result)
            LocalDateTime transactionDate // Argument 11
    ) {
        this.memberNo = memberNo;
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.branchName = branchName;
        this.village = village;
        this.status = status;
        
        // Handle Nulls and Conversions
        this.totalFees = totalFees != null ? totalFees.intValue() : 0;
        this.pendingLoan = pendingLoan != null ? pendingLoan.intValue() : 0;
        this.emiAmount = emiAmount != null ? emiAmount.intValue() : 0;
        this.transactionDate = transactionDate;

        // Logic for Full Name with Branch
        String baseName = (firstName + " " + (middleName != null ? middleName + " " : "") + lastName).trim();
        if (branchName != null && !branchName.trim().isEmpty()) {
            this.fullName = baseName + " (" + branchName.trim() + ")";
        } else {
            this.fullName = baseName;
        }
    }
	
	public MemberSummaryDTO(Integer memberNo, String firstName, String middleName, String lastName, String branchName, 
            String village, MemberStatus status, Double totalFees, 
            Double pendingLoan, Double emiAmount, Double joiningFees, LocalDateTime transactionDate) {
		
		this.transactionDate = transactionDate;
		this.memberNo = memberNo;
		this.firstName = firstName;
		this.middleName = middleName;
		this.lastName = lastName;
		this.branchName = branchName;
		this.branchName = branchName;

		// Create the base name
		String name = (firstName + " " + (middleName != null ? middleName + " " : "") + lastName).trim();

		// Add branch name in parentheses if not null or empty
		if (branchName != null && !branchName.trim().isEmpty()) {
		    this.fullName = name + " (" + branchName.trim() + ")";
		} else {
		    this.fullName = name;
		}
		
		this.village = village;
		this.status = status;
		this.totalFees = totalFees != null ? totalFees.intValue() : 0;
		this.pendingLoan = pendingLoan != null ? pendingLoan.intValue() : 0;
		this.emiAmount = emiAmount != null ? emiAmount.intValue() : 0;
		this.joiningFees = joiningFees != null ? joiningFees.intValue() : 0;
		
	}

	public MemberSummaryDTO(Integer memberNo, String firstName, String middleName, String lastName, 
            String village, MemberStatus status, Double totalFees, 
            Double pendingLoan, Double emiAmount, Double joiningFees) {
		
		this.memberNo = memberNo;
		this.firstName = firstName;
		this.middleName = middleName;
		this.lastName = lastName;
		this.fullName = (firstName + " " + (middleName != null ? middleName + " " : "") + lastName).trim();
		this.village = village;
		this.status = status;
		this.totalFees = totalFees != null ? totalFees.intValue() : 0;
		this.pendingLoan = pendingLoan != null ? pendingLoan.intValue() : 0;
		this.emiAmount = emiAmount != null ? emiAmount.intValue() : 0;
		this.joiningFees = joiningFees != null ? joiningFees.intValue() : 0;
		
	}
	
	public MemberSummaryDTO(Integer memberNo, String firstName, String middleName, String lastName, 
            String village, MemberStatus status, Double totalFees, 
            Double pendingLoan, Double emiAmount, String gujName,String branchNameGuj) {
		
		this.memberNo = memberNo;
		this.firstName = firstName;
		this.middleName = middleName;
		this.lastName = lastName;
		this.fullName = (firstName + " " + (middleName != null ? middleName + " " : "") + lastName).trim();
		this.village = village;
		this.status = status;
		this.totalFees = totalFees != null ? totalFees.intValue() : 0;
		this.pendingLoan = pendingLoan != null ? pendingLoan.intValue() : 0;
		this.emiAmount = emiAmount != null ? emiAmount.intValue() : 0;
		this.gujaratiName = gujName;
		this.branchNameGuj = branchNameGuj;
	}
	
	public MemberSummaryDTO(Integer memberNo, String firstName, String middleName, String lastName, 
            String village, MemberStatus status, Double totalFees, 
            Double pendingLoan, Double emiAmount, LocalDateTime cancelledDate) {
		
		this.memberNo = memberNo;
		this.firstName = firstName;
		this.middleName = middleName;
		this.lastName = lastName;
		this.fullName = (firstName + " " + (middleName != null ? middleName + " " : "") + lastName).trim();
		this.village = village;
		this.status = status;
		this.totalFees = totalFees != null ? totalFees.intValue() : 0;
		this.pendingLoan = pendingLoan != null ? pendingLoan.intValue() : 0;
		this.emiAmount = emiAmount != null ? emiAmount.intValue() : 0;
		this.cancelledDate = cancelledDate;
		
	}
	
    public MemberSummaryDTO(Long memberNo, String firstName, String middleName, String lastName, 
                            String gujaratiName, String branchNameGuj, String village, 
                            MemberStatus status, Double totalFeesPaid, 
                            Double pendingLoanAmount, Double emiAmount) {
        this.memberNo = memberNo.intValue();
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.fullName = (firstName + " " + (middleName != null ? middleName + " " : "") + lastName).trim();
        this.gujaratiName = gujaratiName;
        this.branchNameGuj = branchNameGuj;
        this.village = village;
        this.status = status;
        this.totalFees = totalFeesPaid.intValue();
        this.pendingLoan = pendingLoanAmount.intValue();
        this.emiAmount = emiAmount.intValue();
    }
    
    public MemberSummaryDTO(Integer memberNo, String firstNameGuj, String middleNameGuj, 
            String lastNameGuj, String branchNameGuj, String lastName) {
		this.memberNo = memberNo;
		this.gujaratiName = (firstNameGuj != null ? firstNameGuj : "") + " " +
	               (middleNameGuj != null ? middleNameGuj : "") + " " +
	               (lastNameGuj != null ? lastNameGuj : "") + " " +
	               (branchNameGuj != null && branchNameGuj.length() > 0 ? "( "+branchNameGuj +" )" : "");
		this.lastName = lastName;
	}

	public LocalDateTime getCancelledDate() {
		return cancelledDate;
	}

	public void setCancelledDate(LocalDateTime cancelledDate) {
		this.cancelledDate = cancelledDate;
	}

	public Integer getMemberNo() {
		return memberNo;
	}

	public void setMemberNo(Integer memberNo) {
		this.memberNo = memberNo;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
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

	public Integer getTotalFees() {
		return totalFees;
	}

	public void setTotalFees(Integer totalFees) {
		this.totalFees = totalFees;
	}

	public Integer getPendingLoan() {
		return pendingLoan;
	}

	public void setPendingLoan(Integer pendingLoan) {
		this.pendingLoan = pendingLoan;
	}

	public Integer getEmiAmount() {
		return emiAmount;
	}

	public void setEmiAmount(Integer emiAmount) {
		this.emiAmount = emiAmount;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getMiddleName() {
		return middleName;
	}

	public void setMiddleName(String middleName) {
		this.middleName = middleName;
	}
	
	public String getBranchName() {
		return branchName;
	}

	public void setBranchName(String branchName) {
		this.branchName = branchName;
	}

	public String getFirstNameGuj() {
		return firstNameGuj;
	}

	public void setFirstNameGuj(String firstNameGuj) {
		this.firstNameGuj = firstNameGuj;
	}

	public String getLastNameGuj() {
		return lastNameGuj;
	}

	public void setLastNameGuj(String lastNameGuj) {
		this.lastNameGuj = lastNameGuj;
	}

	public String getMiddleNameGuj() {
		return middleNameGuj;
	}

	public void setMiddleNameGuj(String middleNameGuj) {
		this.middleNameGuj = middleNameGuj;
	}

	public Integer getJoiningFees() {
		return joiningFees;
	}

	public void setJoiningFees(Integer joiningFees) {
		this.joiningFees = joiningFees;
	}

	public LocalDateTime getTransactionDate() {
		return transactionDate;
	}

	public void setTransactionDate(LocalDateTime transactionDate) {
		this.transactionDate = transactionDate;
	}

	public String getGujaratiName() {
		return gujaratiName;
	}

	public void setGujaratiName(String gujaratiName) {
		this.gujaratiName = gujaratiName;
	}

	public String getBranchNameGuj() {
		return branchNameGuj;
	}

	public void setBranchNameGuj(String branchNameGuj) {
		this.branchNameGuj = branchNameGuj;
	}

	public String getFullGujName() {
		return gujaratiName + (branchNameGuj != null && !branchNameGuj.isEmpty() ? "("+branchNameGuj+")" : "");
	}
	
	// Logic to match surname with legend colors
    public void assignColor(Map<String, String> colorMap) {
        if (this.lastName == null) return;
        String upperLast = this.lastName.toUpperCase().trim();
        
        for (Map.Entry<String, String> entry : colorMap.entrySet()) {
            if (upperLast.contains(entry.getKey())) {
                this.rowColor = entry.getValue();
                break;
            }
        }
    }
    public String getRowColor() { return rowColor; }
    public void setRowColor(String rowColor) { this.rowColor = rowColor; }
}