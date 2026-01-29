package com.badargadh.sahkar.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.badargadh.sahkar.data.FinancialMonth;
import com.badargadh.sahkar.data.LoanAccount;
import com.badargadh.sahkar.data.LoanApplication;
import com.badargadh.sahkar.data.LoanWitness;
import com.badargadh.sahkar.data.Member;
import com.badargadh.sahkar.enums.CollectionType;
import com.badargadh.sahkar.enums.LoanApplicationStatus;
import com.badargadh.sahkar.enums.LoanStatus;
import com.badargadh.sahkar.exception.BusinessException;
import com.badargadh.sahkar.repository.EmiPaymentRepository;
import com.badargadh.sahkar.repository.FeePaymentRepository;
import com.badargadh.sahkar.repository.LoanAccountRepository;
import com.badargadh.sahkar.repository.LoanApplicationRepository;
import com.badargadh.sahkar.repository.LoanWitnessRepository;
import com.badargadh.sahkar.repository.MemberRepository;

@Service
public class LoanDisbursementService {

	@Autowired private LoanWitnessRepository loanWitnessRepository;
    @Autowired private LoanAccountRepository loanAccountRepo;
    @Autowired private LoanApplicationRepository loanAppRepo;
    @Autowired private MemberRepository memberRepo;
    @Autowired private FeePaymentRepository feePaymentRepo;
    @Autowired private EmiPaymentRepository emiPaymentRepo;
    @Autowired private FinancialMonthService monthService;
    @Autowired private FeeService feeService;
    @Autowired private LoanService loanService;
    @Autowired private PaymentCollectionService paymentCollectionService;
    
    public void validateDisbursementEligibility(Member member) {
        FinancialMonth activeMonth = monthService.getActiveMonth()
            .orElseThrow(() -> new BusinessException("No active financial month found."));
        /*
        // 1. Validate Monthly Fees (from FeePayment table)
        boolean feesPaid = feePaymentRepo.existsByMemberAndFinancialMonth(member, activeMonth);
        if (!feesPaid) {
            throw new BusinessException("Disbursement Blocked: Monthly Fees for " + 
                activeMonth.getMonthName() + " have not been paid.");
        }

        // 2. Validate EMI if the member has an active loan (from EmiPayment table)
        Optional<LoanAccount> activeLoan = loanAccountRepo.findByMemberAndLoanStatus(member, LoanStatus.ACTIVE);
        if (activeLoan.isPresent()) {
            boolean emiPaid = emiPaymentRepo.existsByMemberAndFinancialMonth(member, activeMonth);
            if (!emiPaid) {
                throw new BusinessException("Disbursement Blocked: Pending EMI for " + 
                    activeMonth.getMonthName() + " must be cleared first.");
            }
        }*/
        
        boolean feesPaid = feePaymentRepo.existsByMemberAndFinancialMonth(member, activeMonth);
        if (!feesPaid) {
        	throw new BusinessException("Disbursement Blocked: Pending FEES/EMI for " + 
                    activeMonth.getMonthName() + " must be cleared first.");
        }
        
        Optional<LoanAccount> optional = loanAccountRepo.findByMemberAndLoanStatus(member, LoanStatus.ACTIVE);
        if(optional.isPresent()) {
        	boolean emiPaid = emiPaymentRepo.existsByMemberAndFinancialMonth(member, activeMonth);
            if (!emiPaid) {
            	throw new BusinessException("Disbursement Blocked: Pending FEES/EMI for " + 
                        activeMonth.getMonthName() + " must be cleared first.");
            }
        }
    }

    @Transactional
    public LoanAccount processDisbursement(LoanApplication app, List<String> witnessNos, 
            CollectionType type, String receiverNo, String remarks) {
        
        // 1. Verify Witnesses & Receiver exist in Member Table
        for (String no : witnessNos) {
            if( no != null && !no.isEmpty()) {
            	memberRepo.findByMemberNo(Integer.parseInt(no))
                	.orElseThrow(() -> new BusinessException("Witness Member No. " + no + " not found."));
            }
        }
        
        Member receiver = null;
        if(receiverNo != null && !receiverNo.isEmpty()) {
	        receiver = memberRepo.findByMemberNo(Integer.parseInt(receiverNo))
	            .orElseThrow(() -> new BusinessException("Receiver Member No. " + receiverNo + " not found."));
        }
        
        
        //check if existing loan balance pending then first add fullpayment in current month emi payment, 
        //close loan account and then create new loan account
        LoanAccount activeLoanAccount = loanService.findMemberActiveLoan(app.getMember());
        if(activeLoanAccount != null) {
        	double pendingAmount = activeLoanAccount.getPendingAmount();
        	if(pendingAmount > 0) {
        		paymentCollectionService.addFullpayment(activeLoanAccount, monthService.getActiveMonth().get());
        		
        		activeLoanAccount.setPendingAmount(0.0);
        		activeLoanAccount.setEndDate( monthService.getActiveMonth().get().getStartDate().plusDays(15));
        		activeLoanAccount.setLoanStatus(LoanStatus.PAID);
        		loanAccountRepo.save(activeLoanAccount);
        		
        		app.setPrevLoanFullAmountDeduction(pendingAmount);
        	}
        }

        // 3. Create Loan Account
        LoanAccount account = new LoanAccount();
        account.setMember(app.getMember());
        account.setLoanApplication(app);
        account.setGrantedAmount(app.getAppliedAmount());
        account.setPendingAmount(app.getAppliedAmount());
        account.setLoanStatus(LoanStatus.ACTIVE);
        account.setFinancialMonth(monthService.getActiveMonth().get());
        account.setCreatedDate(monthService.getActiveMonth().get().getStartDate().plusDays(10));
        
        List<LoanWitness> loanWitnesses = witnessNos.stream()
        		.filter(no -> no != null && no.length() > 0)
        		.map(no -> getLoanWitness(no, app))
        		.collect(Collectors.toList());
        
        // 4. Update Application with Tracking Details
        app.setStatus(LoanApplicationStatus.DISBURSED);
        app.setCollectionType(type);
        app.setReceivedBy(receiver);
        app.setAuthorityName(receiverNo);
        app.setCollectionRemarks(remarks);
        app.setDisbursementDateTime(LocalDateTime.now());
        app.setWitnesses(loanWitnesses);
        
        loanAccountRepo.save(account);
        loanAppRepo.save(app);
        
        Double feesDeduction = feeService.getMemberFeeDeductionOnFirstLoan(app.getMember());
        
        feeService.recordLoanDeductionFee(app.getMember(), feesDeduction.longValue(), app.getApplicationDateTime(), monthService.getActiveMonth().get(), null);
        
        app.setFeesDeduction(feesDeduction);
        loanAppRepo.save(app);
        
        return account;
    }
    
    public void processBackdateLoanDisbursment(LoanApplication app) {
    	// 3. Create Loan Account
        LoanAccount account = new LoanAccount();
        account.setMember(app.getMember());
        account.setLoanApplication(app);
        account.setGrantedAmount(app.getAppliedAmount());
        account.setPendingAmount(app.getAppliedAmount());
        account.setLoanStatus(LoanStatus.ACTIVE);
        account.setFinancialMonth(monthService.getActiveMonth().get());
        //account.setStartDate(LocalDate.now().plusMonths(1).withDayOfMonth(1));
        
        // 4. Update Application with Tracking Details
        app.setStatus(LoanApplicationStatus.DISBURSED);
        app.setCollectionType(CollectionType.SELF);

        loanAccountRepo.save(account);
        loanAppRepo.save(app);
        
        Double feesDeduction = feeService.getMemberFeeDeductionOnFirstLoan(app.getMember());
        
        feeService.recordLoanDeductionFee(app.getMember(), feesDeduction.longValue(), app.getApplicationDateTime(), monthService.getActiveMonth().get(), null);
        
    }
    
    private LoanWitness getLoanWitness(String no, LoanApplication application) {

    	Member member = memberRepo.findByMemberNo(Integer.parseInt(no)).get();
    	
    	LoanWitness loanWitness = new LoanWitness();
    	loanWitness.setWitnessMember(member);
    	loanWitness.setLoanApplication(application);
    	return loanWitness = loanWitnessRepository.save(loanWitness);
    }
}