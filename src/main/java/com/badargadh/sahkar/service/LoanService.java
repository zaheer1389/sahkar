package com.badargadh.sahkar.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.badargadh.sahkar.data.AppConfig;
import com.badargadh.sahkar.data.EmiPayment;
import com.badargadh.sahkar.data.FinancialMonth;
import com.badargadh.sahkar.data.LoanAccount;
import com.badargadh.sahkar.data.LoanApplication;
import com.badargadh.sahkar.data.LoanWitness;
import com.badargadh.sahkar.data.Member;
import com.badargadh.sahkar.data.PaymentRemark;
import com.badargadh.sahkar.enums.FeeType;
import com.badargadh.sahkar.enums.LoanApplicationStatus;
import com.badargadh.sahkar.enums.LoanStatus;
import com.badargadh.sahkar.enums.MemberStatus;
import com.badargadh.sahkar.enums.RemarkType;
import com.badargadh.sahkar.exception.BusinessException;
import com.badargadh.sahkar.repository.EmiPaymentRepository;
import com.badargadh.sahkar.repository.FeePaymentRepository;
import com.badargadh.sahkar.repository.LoanAccountRepository;
import com.badargadh.sahkar.repository.LoanApplicationRepository;
import com.badargadh.sahkar.repository.LoanWitnessRepository;
import com.badargadh.sahkar.repository.MemberRepository;
import com.badargadh.sahkar.repository.MonthlyFeesRepository;
import com.badargadh.sahkar.repository.PaymentRemarkRepository;
import com.badargadh.sahkar.util.AppLogger;

import jakarta.transaction.Transactional;

@Service
public class LoanService {

	@Autowired private LoanWitnessRepository loanWitnessRepository;
    @Autowired private LoanAccountRepository loanAccountRepo;
    @Autowired private FeePaymentRepository feeRepo;
    @Autowired private AppConfigService configService;
    @Autowired private LoanApplicationRepository loanAppRepo;
    @Autowired private FinancialMonthService monthService;
    @Autowired private EmiPaymentRepository emiPaymentRepo;
    @Autowired private PaymentRemarkRepository paymentRemarkRepository;
    @Autowired private PaymentRemarkRepository remarkRepo;
    @Autowired private MemberRepository memberRepository;
    @Autowired private MonthlyFeesRepository monthlyFeesRepository;

    public AppConfig getLoanSettings() {
        return configService.getSettings();
    }
    
    public List<LoanApplication> getAllApplications(LoanApplicationStatus status) {
        return loanAppRepo.findAllByStatusOrderByApplicationDateTimeAsc(status);
    }
    
    public List<LoanApplication> getActiveApplications() {
    	return loanAppRepo.findAllActiveLoanApplications();
    }
    
    public List<LoanApplication> findActiveLoansWithoutWitness() {
    	return loanAppRepo.findActiveLoansWithoutWitness();
    }
    
    public List<LoanApplication> getAllApplications() {
        return loanAppRepo.findAllByOrderByApplicationDateTimeDesc();
    }
    
    public List<LoanApplication> getRecentApplications(LocalDateTime dateTime) {
        return loanAppRepo.findRecentApplications(dateTime);
    }
    
    public List<LoanApplication> getAllApplications(FinancialMonth month, LoanApplicationStatus status) {
        return loanAppRepo.findAllByFinancialMonthAndStatus(month, status);
    }
    
    public List<LoanApplication> getAllApplicationsWithStatuses(FinancialMonth month, List<LoanApplicationStatus> statuses) {
        return loanAppRepo.findAllByFinancialMonthAndStatusInOrderByStatusAscMemberMemberNoAsc(month, statuses);
    }

    public void validateEligibility(Member member, LocalDateTime date) throws BusinessException {
        AppConfig config = configService.getSettings();

        // 1. Check if Financial Month is Open
        if (!monthService.getActiveMonth().isPresent()) {
            throw new BusinessException("Cannot submit application: No financial month is currently OPEN.");
        }
        
        if(member.getStatus() == MemberStatus.CANCELLED) {
        	throw new BusinessException("Cannot submit application: Member is cancelled");
        }
        
        FinancialMonth activeMonth = monthService.getActiveMonth().get();
        
        boolean feesPaid = feeRepo.existsByMemberAndFinancialMonth(member, activeMonth);
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
        
        
        LocalDateTime maxApplicationDateTime = loanAppRepo.findMaxApplicationDateTime();
        AppLogger.info("Validating loan date. Current Max: " + maxApplicationDateTime + " | New App: " + date);
        if(maxApplicationDateTime.isAfter(date)) {
        	throw new BusinessException("Cannot apply for loan in back date. Please check your application date.");
        }
        
        
        Optional<LoanApplication> loanApplication = loanAppRepo.findByMemberAndFinancialMonth(member, activeMonth);
        if(loanApplication.isPresent()) {
        	throw new BusinessException("Cannot submit application: Member has already applied for loan this month");
        }
        
        
        LocalDate monthBusinessDate = LocalDate.of(activeMonth.getYear(), Month.valueOf(activeMonth.getMonthName()), 10);
        LocalDateTime applicationDatetime = LocalDateTime.of(monthBusinessDate, LocalTime.now());
        
        if(!monthService.isValidTransactionDate(applicationDatetime)) {
        	throw new BusinessException("System Date Error!\n" +
                "The System date (" + applicationDatetime + ") is outside the current OPEN period ");
        }
        
        //We have every month fees data from Apr 2025 on ward. So member joined after that cutoff can directly check with no of fees count
        if(member.getJoiningDateTime().isAfter(LocalDateTime.of(2025, 4, 1, 0, 0 , 0))) {
        	int totalFeesMonths = feeRepo.countByMemberAndFeeType(member, FeeType.MONTHLY_FEE) + 1;
            System.err.println("total fees paid in months "+totalFeesMonths);
            if (totalFeesMonths < config.getNewMemberCoolingPeriod()) {
                throw new BusinessException("Cooling Period active. Paid: " + totalFeesMonths + 
                    " months. Required: " + config.getNewMemberCoolingPeriod());
            }
        }
        else {
        	//for member joined before Apr 2025 we need to breakdown total fees and need to find out how much months fees paid
        	long totalFee = feeRepo.getMemberTotalFee(member.getId()).longValue();
        	//to get fee months minus joining fee from total fee and divide it with monthly fee and again add joining fee month
        	long totalFeesMonths = ((totalFee - 300) / 20) + 1;
        	System.err.println("total fees paid in months "+totalFeesMonths);
            if (totalFeesMonths < config.getNewMemberCoolingPeriod()) {
                throw new BusinessException("Cooling Period active. Paid: " + totalFeesMonths + 
                    " months. Required: " + config.getNewMemberCoolingPeriod());
            }
        }

        // 3. Check Pending Loan Balance from LoanAccount (Must be < 2000)
        Optional<LoanAccount> activeAccount = loanAccountRepo.findByMemberAndLoanStatus(member, LoanStatus.ACTIVE);

        if (activeAccount.isPresent()) {
        	LoanAccount loanAccount = activeAccount.get();
            Double pending = loanAccount.getPendingAmount();
            int balanceEmi = (int) (pending / loanAccount.getEmiAmount());
            if (balanceEmi > 10) {
                throw new BusinessException("Balance emi is more than allowed limit i.e. (total balance emi "+balanceEmi+" is more than allowed 10 emi for over loan application)");
            }                       
        }
    }

    @Transactional
    public void submitApplication(LoanApplication app) {
        // Final server-side safety check before saving
        validateEligibility(app.getMember(), app.getApplicationDateTime());
        
        FinancialMonth month = monthService.getActiveMonth().get();
        LocalDate monthBusinessDate = LocalDate.of(month.getYear(), Month.valueOf(month.getMonthName()), 10);
        //LocalTime time = LocalTime.now();
        //LocalDateTime applicationDateTime = LocalDateTime.of(monthBusinessDate, time);
        
        LoanApplicationStatus status = null;
        
        // 1. Fetch all active (uncleared) remarks
        List<PaymentRemark> activeRemarks = paymentRemarkRepository.findByMemberAndIsClearedFalseOrderByIssuedDateAsc(app.getMember());
        if (!activeRemarks.isEmpty()) {
        	// 2. Consume the oldest remark
            PaymentRemark oldest = activeRemarks.get(0);
            oldest.setCleared(true);
            oldest.setClearedDate(monthBusinessDate);
            oldest.setClearingReason("APPLICATION_REJECTION_PENALTY");
            paymentRemarkRepository.save(oldest);
            
            status = LoanApplicationStatus.REJECTED_FOR_REMARK;
        }
        
        boolean drawAlreadyDone = loanAppRepo.existsByFinancialMonthAndDrawRankIsNotNull(month);
        if (drawAlreadyDone) {
            app.setSurplusNoticeApp(true);
            // Optional: Show a small info message to the admin
            AppLogger.info("Application marked as Surplus Notice (Draw 1 already completed).");
        }
        
        app.setFinancialMonth(month);
        app.setStatus(status != null ? status : LoanApplicationStatus.APPLIED);
        //app.setApplicationDateTime(applicationDateTime);
        loanAppRepo.save(app);
    }
    
    @Transactional
    public void rejectApplication(LoanApplication loanApplication, LoanApplicationStatus finalStatus, String selectedReason) {
        
    	// Safety check: Don't reject if already disbursed
        if (loanApplication.getStatus() == LoanApplicationStatus.DISBURSED) {
            throw new BusinessException("Cannot reject an already disbursed loan.");
        }

        loanApplication.setStatus(finalStatus);
        loanApplication.setRejectionReason(selectedReason);
        loanAppRepo.save(loanApplication);
        
        if(finalStatus == LoanApplicationStatus.NO_SHOW) {
        	addLoanNoShowRemarks(loanApplication);
        	promoteNextWaitingMember(loanApplication.getFinancialMonth());
        }
    }
    
    @Transactional
    public void promoteNextWaitingMember(FinancialMonth month) {
        // 1. Find the first person in the waiting list
        Optional<LoanApplication> nextInLine = loanAppRepo
            .findFirstByFinancialMonthAndDrawRankStartingWithOrderByDrawRankAsc(month, "WL");

        nextInLine.ifPresent(next -> {
            // 2. Determine the next SL number
            long currentSLCount = loanAppRepo.countByFinancialMonthAndDrawRankStartingWith(month, "SL");
            String newRank = String.format("SL-%02d", currentSLCount + 1);

            // 3. Update Status and Rank
            next.setDrawRank(newRank);
            next.setStatus(LoanApplicationStatus.SELECTED_IN_DRAW);
            loanAppRepo.save(next);
            
            // Note: You can optionally re-index the remaining WL members 
            // but usually, leaving them as WL-02, WL-03 is fine for history.
        });
    }
    
    @Transactional
    public void addLoanNoShowRemarks(LoanApplication application) {
    	for(int i = 0; i <= 2; i++) {
    		PaymentRemark remark = new PaymentRemark();
            remark.setMember(application.getMember());
            remark.setRemarkType(RemarkType.FORM_CANCELLED);
            remark.setIssuedDate(LocalDate.now());
            remark.setFinancialMonth(application.getFinancialMonth());
            remarkRepo.save(remark);
    	}
    }
    
    @Transactional
    public void processDrawResults(Map<Long, LoanApplication>  memberRankMap, FinancialMonth activeMonth) {
        if (memberRankMap == null || memberRankMap.isEmpty()) {
            throw new BusinessException("No members selected for processing.");
        }
        /*
        // 1. Mark winners as 'SELECTED_IN_DRAW'
        int winnersCount = loanAppRepo.updateStatusForSelectedMembers(
            selectedMemberNos, 
            activeMonth, 
            LoanApplicationStatus.SELECTED_IN_DRAW
        );

        // 2. Mark the rest of the 'APPLIED' entries as 'WAITING'
        int waitingCount = loanAppRepo.updateRemainingToWaiting(
            selectedMemberNos, 
            activeMonth, 
            LoanApplicationStatus.WAITING
        );*/
        int winnersCount = 0, waitingCount = 0;
        for (Map.Entry<Long, LoanApplication> entry : memberRankMap.entrySet()) {
        	LoanApplication app = entry.getValue();
        	String rank = app.getDrawRank();
        	loanAppRepo.save(app);
        	
        	if (rank.startsWith("SL")) {
                winnersCount++;
            } else {
                waitingCount++;
            }
        }
        

        System.out.println("Processed Draw: " + winnersCount + " winners, " + waitingCount + " moved to waiting.");
    }
    
    public LoanAccount getLoanAccountForApplication(LoanApplication application) {
        return loanAccountRepo.findByLoanApplication(application).orElse(null);
    }
    
    public LoanAccount findMemberActiveLoan(Member member) {
    	Optional<LoanAccount> optional = loanAccountRepo.findByMemberAndLoanStatus(member, LoanStatus.ACTIVE);
    	return optional.isPresent() ? optional.get() : null;
    }
    
    public Optional<LoanApplication> findByIdWithWitnesses(Long id) {
    	return loanAppRepo.findByIdWithWitnesses(id);
    }
    
    public LoanWitness getLoanWitness(Integer memberNo) {
    	Member member = memberRepository.findByMemberNo(memberNo).get();
    	LoanAccount loanAccount = findMemberActiveLoan(member);
    	if(loanAccount != null) {
    		List<LoanWitness> loanWitness = loanWitnessRepository.findByLoanApplicationOrderByIdAsc(loanAccount.getLoanApplication());
        	return loanWitness != null && loanWitness.size() > 0 ? loanWitness.get(0) : null;
    	}
    	return null;
    }

    public int calculateMemberLoanPriority(Member member, FinancialMonth currentFinancialMonth) {
    	LocalDate referenceDate = (currentFinancialMonth != null) ? currentFinancialMonth.getStartDate() : LocalDate.now();

        // 1. Check if Member is "NEW" or "OLD" based on Fees
        double societyRequiredFee = monthlyFeesRepository.sumOfMonthlyFeesAmount().doubleValue();
        double memberPaidFee = feeRepo.getMemberTotalFee(member.getId());

        boolean isNewMember = memberPaidFee < societyRequiredFee;

        // 3. Logic for New Members (No previous loan, still paying joining/monthly fees)
        if (isNewMember) {
            // Higher fee paid = higher priority among new members
            return feeRepo.countByMember(member); 
        }
        
        // 2. Handle Active Loans (Lowest Priority)
        Optional<LoanAccount> activeLoan = loanAccountRepo.findFirstByMemberAndLoanStatusOrderByEndDateDesc(member, LoanStatus.ACTIVE);
        if (activeLoan.isPresent()) {
            LoanAccount loan = activeLoan.get();
            int remainingEmis = (loan.getEmiAmount() != null && loan.getEmiAmount() > 0) 
                                ? (int) (loan.getPendingAmount() / loan.getEmiAmount()) : 0;
            return -remainingEmis;
        }

        // 4. Logic for Old Members (Loan Cleared)
        Optional<LoanAccount> lastPaidLoan = loanAccountRepo.findFirstByMemberAndLoanStatusOrderByEndDateDesc(member, LoanStatus.PAID);
        
        int bonusMonths = 0;
        LocalDate gapStartDate;

        if (lastPaidLoan.isPresent()) {
            LoanAccount loan = lastPaidLoan.get();
            gapStartDate = loan.getEndDate();

            // Calculate Full Payment Bonus
            Optional<EmiPayment> finalPaymentOpt = emiPaymentRepo.findTopByMemberOrderByPaymentDateTimeDesc(member);
            if (finalPaymentOpt.isPresent() && finalPaymentOpt.get().isFullPayment()) {
                double emi = loan.getEmiAmount();
                double extraPaid = finalPaymentOpt.get().getFullPaymentAmount();
                if (emi > 0 && extraPaid > 0) {
                    bonusMonths = (int) Math.ceil(extraPaid / emi);
                }
            }
        } else {
            // No loan history but full fees paid (rare but possible)
            gapStartDate = LocalDate.of(2024, 12, 10);
        }

        long naturalGap = Math.max(0,
                (referenceDate.getYear() - gapStartDate.getYear()) * 12L +
                (referenceDate.getMonthValue() - gapStartDate.getMonthValue()));

        int totalWait = (int) naturalGap - bonusMonths;

        return totalWait;
    }
}