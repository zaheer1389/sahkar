package com.badargadh.sahkar.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.badargadh.sahkar.data.AppConfig;
import com.badargadh.sahkar.data.EmiPayment;
import com.badargadh.sahkar.data.EmiPaymentGroup;
import com.badargadh.sahkar.data.FeePayment;
import com.badargadh.sahkar.data.FinancialMonth;
import com.badargadh.sahkar.data.LoanAccount;
import com.badargadh.sahkar.data.Member;
import com.badargadh.sahkar.data.MonthlyPayment;
import com.badargadh.sahkar.data.PaymentRemark;
import com.badargadh.sahkar.enums.CollectionLocation;
import com.badargadh.sahkar.enums.FeeType;
import com.badargadh.sahkar.enums.LoanStatus;
import com.badargadh.sahkar.enums.RemarkType;
import com.badargadh.sahkar.exception.BusinessException;
import com.badargadh.sahkar.repository.EmiPaymentGroupRepository;
import com.badargadh.sahkar.repository.EmiPaymentRepository;
import com.badargadh.sahkar.repository.FeePaymentRepository;
import com.badargadh.sahkar.repository.LoanAccountRepository;
import com.badargadh.sahkar.repository.PaymentRemarkRepository;
import com.badargadh.sahkar.util.UserSession;

@Service
public class PaymentCollectionService {

	@Autowired private EmiPaymentService emiPaymentService;
    @Autowired private FeeService feeService;
    @Autowired private LoanService loanService;
    @Autowired private FinancialMonthService monthService;
    @Autowired private LoanAccountRepository loanAccountRepo;
    @Autowired private EmiPaymentGroupRepository emiPaymentGroupRepo;
    @Autowired private PaymentRemarkRepository remarkRepo;
    @Autowired private EmiPaymentRepository emiPaymentRepository;
    @Autowired private FeePaymentRepository feePaymentRepository;
    @Autowired private AppConfigService appConfigService;
    
    @Transactional
    public EmiPaymentGroup processMonthlyCollection(List<MonthlyPayment> payments, Member depositor, String depositorName, CollectionLocation location) {
        FinancialMonth activeMonth = monthService.getActiveMonth()
                .orElseThrow(() -> new BusinessException("No active financial month found."));

        EmiPaymentGroup group = new EmiPaymentGroup();
        group.setDepositedBy(depositor);
        group.setDepositorName(depositorName);
        group.setCollectionLocation(location);
        group.setFinancialMonth(activeMonth);
        group.setTransactionDateTime(LocalDateTime.now());
        group.setPaymentCount(payments.size()); // Track how many payments were in the group
        group = emiPaymentGroupRepo.save(group);
        
        List<EmiPayment> emiPayments = new ArrayList<EmiPayment>();
        
        double groupTotal = 0;

        for (MonthlyPayment monthlyPayment : payments) {
            Member member = monthlyPayment.getMember();
            
            // 1. Save Monthly Fees
            FeePayment fee = feeService.recordMonthlyFee(member, monthlyPayment.getMonthlyFees().longValue(), 
                                       monthlyPayment.getEmiDate(), activeMonth, depositor, location, group);
            group.addFeePayment(fee);
            
            groupTotal += monthlyPayment.getMonthlyFees();

            // 2. Process Loan EMI and Balance
            LoanAccount loan = loanService.findMemberActiveLoan(member);
            if (loan != null && monthlyPayment.getEmiAmount() > 0) {
            	
            	// --- FIX: ADJUST EMI AMOUNT FOR LAST PAYMENT ---
                double requestedEmi = monthlyPayment.getEmiAmount().doubleValue();
                double currentPending = loan.getPendingAmount();
                double paymentAmount = requestedEmi;

                // If it's a regular EMI (not full payment) but exceeds pending balance, cap it.
                if (requestedEmi > currentPending) {
                    paymentAmount = currentPending;
                }
                
                // Set First EMI details if the loan isn't locked yet
                if (!loan.isEmiLocked()) {
                    loan.setEmiAmount(monthlyPayment.getEmiAmount().doubleValue());
                    loan.setStartDate(activeMonth.getStartDate().plusDays(10));
                }

                double fullPaymentAmount = monthlyPayment.getFullAmount().doubleValue();
                
                // Create and add EMI record to the group
                EmiPayment emi = new EmiPayment();
                emi.setMember(member);
                emi.setLoanAccount(loan);
                emi.setFinancialMonth(activeMonth);
                emi.setAmountPaid(paymentAmount);
                emi.setFullPayment(monthlyPayment.isFullPayment());
                emi.setFullPaymentAmount(fullPaymentAmount);
                emi.setPaymentDateTime(monthlyPayment.getEmiDate());
                emi.setDipositedBy(depositor);
                emi.setAddedBy(UserSession.getLoggedInUser());
                emi.setCollectionLocation(location);
                emi.setPaymentGroup(group);
                emi = emiPaymentService.recordEmiPayment(emi);    
                
                group.addEmiPayment(emi);
                
                emiPayments.add(emi);
                
                if (monthlyPayment.isFullPayment()) {
                	paymentAmount = paymentAmount + fullPaymentAmount;
                }

                // Update Loan Balance and check for closure
                double newPending = loan.getPendingAmount() - paymentAmount;
                loan.setPendingAmount(Math.max(0, newPending));

                if (loan.getPendingAmount() <= 0) {
                    loan.setLoanStatus(LoanStatus.PAID);
                    loan.setEndDate(activeMonth.getStartDate().plusDays(10));
                    // Ensure pending is exactly zero for the record
                    loan.setPendingAmount(0.0);
                }
                
                loanAccountRepo.save(loan);
                
                groupTotal += paymentAmount;
                
                monthlyPayment.setBalanceAmount((int)newPending);
            }
            
            AppConfig appConfig = appConfigService.getSettings();
            if(appConfig.isAutoRemark() || monthlyPayment.isRemarkAdded()) {
            	LocalDateTime now = LocalDateTime.now();
            	LocalDateTime deadline = LocalDateTime.of(activeMonth.getStartDate().withDayOfMonth(11), LocalTime.of(17, 0));
            	if (now.isAfter(deadline)) {
                    if(!remarkRepo.findByMemberAndFinancialMonth(member, activeMonth).isPresent()) {
                    	PaymentRemark remark = new PaymentRemark();
                        remark.setMember(member);
                        remark.setRemarkType(monthlyPayment.getEmiAmount() > 0 ? RemarkType.LATE_EMI : RemarkType.LATE_FEE);
                        remark.setIssuedDate(LocalDate.now());
                        remark.setFinancialMonth(activeMonth);
                        remarkRepo.save(remark);
                    }
                }
            }
            
        }
        
        group.setTotalAmount(groupTotal);
        return emiPaymentGroupRepo.save(group);
    }
    
    public boolean isMonthlyMemberEmiorFeesPaid(Member member, FinancialMonth financialMonth) {
    	
    	boolean feesPaid = feePaymentRepository.existsByMemberAndFinancialMonth(member, financialMonth);
    	boolean emiPaid = emiPaymentRepository.existsByMemberAndFinancialMonth(member, financialMonth);
        
        if (feesPaid || emiPaid) {
            return true;
        }
        
        return false;
    }
    
    public boolean addFullpayment(LoanAccount account, FinancialMonth month) {
    	Optional<EmiPayment> optional = emiPaymentRepository.findByMemberAndFinancialMonth(account.getMember(), month);
    	if(optional.isPresent()) {
    		EmiPayment emiPayment = optional.get();
    		emiPayment.setFullPayment(true);
    		emiPayment.setFullPaymentAmount(account.getPendingAmount());
    		emiPayment = emiPaymentRepository.save(emiPayment);
    		
    		return emiPayment.getFullPaymentAmount() > 0;
    	}
    	else {
    		throw new BusinessException("Monthly payment not found");
    	}
    	
    }
    
    @Transactional(readOnly = true)
    public EmiPaymentGroup getEmiPaymentGroupByGroupId(Long groupId) {
        return emiPaymentGroupRepo.findById(groupId)
                .orElseThrow(() -> new BusinessException("Batch not found"));
    }
    
    @Transactional(readOnly = true)
    public List<MonthlyPayment> getMonthlyPaymentsByGroup(Long groupId) {
        EmiPaymentGroup group = emiPaymentGroupRepo.findById(groupId)
        .orElseThrow(() -> new BusinessException("Batch not found"));
        // Map to group everything by Member ID
        Map<Long, MonthlyPayment> memberMap = new HashMap<>();

        // 1. Map Fees
        for (FeePayment fee : group.getFeePayments()) {
            MonthlyPayment mp = memberMap.computeIfAbsent(fee.getMember().getId(), k -> {
                MonthlyPayment newMp = new MonthlyPayment();
                newMp.setMember(fee.getMember());
                newMp.setEmiDate(fee.getTransactionDateTime());
                return newMp;
            });
            
            if (fee.getFeeType() == FeeType.MONTHLY_FEE) {
                mp.setMonthlyFees(fee.getAmount().intValue());
            }
        }

        // 2. Map EMIs
        for (EmiPayment emi : group.getEmiPayments()) {
            MonthlyPayment mp = memberMap.computeIfAbsent(emi.getMember().getId(), k -> {
                MonthlyPayment newMp = new MonthlyPayment();
                newMp.setMember(emi.getMember());
                newMp.setEmiDate(emi.getPaymentDateTime());
                return newMp;
            });

            mp.setEmiAmount(emi.getAmountPaid().intValue());
            mp.setFullAmount(emi.getFullPaymentAmount() != null ? emi.getFullPaymentAmount().intValue() : 0);
            mp.setFullPayment(emi.isFullPayment());
            // Show the current balance from the loan
            mp.setBalanceAmount(emi.getLoanAccount().getPendingAmount().intValue());
        }

        return new ArrayList<>(memberMap.values());
    }
}