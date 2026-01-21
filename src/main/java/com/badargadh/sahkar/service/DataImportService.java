package com.badargadh.sahkar.service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.badargadh.sahkar.data.FeePayment;
import com.badargadh.sahkar.data.FinancialMonth;
import com.badargadh.sahkar.data.LoanAccount;
import com.badargadh.sahkar.data.LoanApplication;
import com.badargadh.sahkar.data.Member;
import com.badargadh.sahkar.data.MemberHistory;
import com.badargadh.sahkar.data.MonthlyFees;
import com.badargadh.sahkar.enums.FeeType;
import com.badargadh.sahkar.enums.LoanApplicationStatus;
import com.badargadh.sahkar.enums.LoanStatus;
import com.badargadh.sahkar.enums.MemberStatus;
import com.badargadh.sahkar.enums.MonthStatus;
import com.badargadh.sahkar.repository.FeePaymentRepository;
import com.badargadh.sahkar.repository.LoanAccountRepository;
import com.badargadh.sahkar.repository.LoanApplicationRepository;
import com.badargadh.sahkar.repository.MemberHistoryRepository;
import com.badargadh.sahkar.repository.MemberRepository;
import com.badargadh.sahkar.repository.MonthlyFeesRepository;

import jakarta.transaction.Transactional;

@Service
public class DataImportService {

	@Autowired
    private MemberRepository memberRepo;
	
	@Autowired
    private FeePaymentRepository feeRepo;
	
	@Autowired
    private LoanAccountRepository loanRepo;
	
	@Autowired
    private MemberHistoryRepository historyRepo;
	
	@Autowired
    private LoanApplicationRepository loanAppRepo;
	
	@Autowired private FinancialMonthService financialMonthService;
	@Autowired private FeeService feeService;
    
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    

    Long monthlyFeesTillFeb_25 = 4800L;
    
    @Transactional
    public void importMembers(String csvPath, Member operator) throws Exception {
        try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {
            String line;
            br.readLine(); // Skip Header

            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");
                
                Integer mNo = Integer.parseInt(getVal(data, 0));
                String fName = getVal(data, 1);
                String mName = getVal(data, 2);
                String lName = getVal(data, 3);
                String village = getVal(data, 4);
                Double fees = parseDouble(getVal(data, 5));
                Double loanPending = parseDouble(getVal(data, 6));
                Double emi = parseDouble(getVal(data, 7));
                String status = getVal(data, 8);
                String cancelledDate = getVal(data, 9);
                Double refundFeesAmount = getVal(data, 10).length() > 0 ? parseDouble(getVal(data, 10)) : 0d;
                MemberStatus memberStatus = MemberStatus.valueOf(status);
                
                Member member = createMember(mNo, fName, mName, lName, village, cancelledDate, memberStatus, fees);
                
                if(memberStatus == MemberStatus.REFUNDED) {
                	MemberHistory history = new MemberHistory();
                	history.setFirstName(fName);
                	history.setMiddleName(mName);
                	history.setLastName(lName);
                	history.setOriginalMemberId(member.getId());
                	history.setMemberNo(mNo);
                	history.setTotalFeesRefunded(refundFeesAmount);
                	historyRepo.save(history);
                	
                	member.setMemberNo(null);
                	memberRepo.save(member);
                }
                
                addFees(member, fees);
                
                if(loanPending > 0) {
                	createApplication(member, loanPending, emi);
                }
            }
        }
    }
    
    private Member createMember(Integer mNo, String fName, String mName, String lName, String village, 
    		String cancelledDate, MemberStatus memberStatus, Double fees) {
    	Member member = new Member();
    	member.setMemberNo(mNo);
    	member.setFirstName(fName);
    	member.setMiddleName(mName);
    	member.setLastName(lName);
    	member.setVillage(village);
    	
    	int totalMonth = (int)((fees-300)/20);
    	
    	LocalDateTime joiningDate = LocalDateTime.of(2025, 2, 10, 13, 10).minusMonths(totalMonth);
    	
    	member.setJoiningDateTime(joiningDate);
    	
    	if(cancelledDate != null && cancelledDate.length() > 0) {
    		member.setCancellationDateTime(LocalDateTime.of(LocalDate.parse(cancelledDate, formatter), LocalTime.of(15, 30)));
        	member.setStatus(memberStatus);
    	}
    	return memberRepo.save(member);
    }
    
    private LoanApplication createApplication(Member member, Double pendingAmout, Double emiAmount) {
    	
    	Double loanAmount = 10000d;
    	
    	LocalDate loanApplicationDate = LocalDate.of(2025,2,10);
    	
    	if(emiAmount != null && emiAmount > 0) {   		
        	int totalMonthsPaid =  (int) ((loanAmount - pendingAmout) / emiAmount);
        	loanApplicationDate = loanApplicationDate.minusMonths(totalMonthsPaid-1);
    	}
    	
    	LocalDateTime loanApplicationDateTime = LocalDateTime.of(loanApplicationDate, LocalTime.of(13, 35));
    	
    	LoanApplication loanApplication = new LoanApplication();
    	loanApplication.setApplicationDateTime(loanApplicationDateTime);
    	loanApplication.setAppliedAmount(loanAmount);
    	loanApplication.setMember(member);
    	loanApplication.setWitnesses(null);
    	loanApplication.setStatus(LoanApplicationStatus.DISBURSED);
    	loanApplication = loanAppRepo.save(loanApplication);
    	
    	LoanStatus loanStatus = pendingAmout > 0 ? LoanStatus.ACTIVE : LoanStatus.PAID;
    	
    	LoanAccount loanAccount = new LoanAccount();
    	loanAccount.setGrantedAmount(loanAmount);
    	loanAccount.setLoanStatus(loanStatus);
    	loanAccount.setStartDate(loanApplicationDate.plusMonths(1));
    	loanAccount.setEndDate(loanStatus == LoanStatus.PAID ? LocalDate.of(2025, 2, 10).minusMonths(1) : null);
    	loanAccount.setPendingAmount(pendingAmout);
    	loanAccount.setEmiAmount(emiAmount);
    	loanAccount.setMember(member);
    	loanAccount.setLoanApplication(loanApplication);
    	loanAccount = loanRepo.save(loanAccount);
    	
    	return loanApplication;
    }
    
    private void addFees(Member member, Double fees) {
    	
    	FinancialMonth financialMonth = null;
    	Optional<FinancialMonth> optional = financialMonthService.getMonthFromMonthAndYear("FEBRUARY", 2025);
    	if(optional.isPresent()) {
    		financialMonth = optional.get();
    	}
    	else {
    		financialMonth = financialMonthService.openMonthByDetails("FEBRUARY", 2025);
    	}
    	feeService.addMonthlyFees(financialMonth, monthlyFeesTillFeb_25, financialMonth.getStartDate().plusDays(10));
    	
    	FeePayment fp = new FeePayment();
        fp.setMember(member);
        fp.setAmount(fees);
        fp.setFeeType(FeeType.OPENING_BALANCE);
        fp.setRunningBalance(fees);
        fp.setTransactionDateTime(LocalDateTime.of(2025, 02, 10, 13, 50));
        fp.setFinancialMonth(financialMonth);
        feeRepo.save(fp);
    }

    // --- HELPER METHODS TO PREVENT OUT OF BOUNDS ---

    private String getVal(String[] data, int index) {
        if (index < data.length) {
            return data[index] != null ? data[index].trim() : "";
        }
        return "";
    }

    private Double parseDouble(String val) {
        if (val == null || val.isEmpty() || val.equalsIgnoreCase("NaN")) return 0.0;
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}