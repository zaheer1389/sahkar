package com.badargadh.sahkar.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.badargadh.sahkar.data.EmiPaymentGroup;
import com.badargadh.sahkar.data.FeePayment;
import com.badargadh.sahkar.data.FinancialMonth;
import com.badargadh.sahkar.data.Member;
import com.badargadh.sahkar.data.MonthlyFees;
import com.badargadh.sahkar.enums.CollectionLocation;
import com.badargadh.sahkar.enums.FeeType;
import com.badargadh.sahkar.repository.FeePaymentRepository;
import com.badargadh.sahkar.repository.MonthlyFeesRepository;
import com.badargadh.sahkar.util.UserSession;

@Service
public class FeeService {

    @Autowired private FeePaymentRepository feeRepo;
    @Autowired private MonthlyFeesRepository monthlyFeesRepository;

    public void recordLoanDeductionFee(Member member, Long amount, LocalDateTime localDate, FinancialMonth financialMonth, Member depositor) {
        if (amount <= 0) return;
        
        FeePayment payment = new FeePayment();
        payment.setMember(member);
        payment.setAmount(amount.doubleValue());
        payment.setFeeType(FeeType.LOAN_DEDUCTION);
        payment.setTransactionDateTime(localDate);
        payment.setFinancialMonth(financialMonth);
        payment.setDipositedBy(depositor);
        payment.setAddedBy(UserSession.getLoggedInUser());
        payment.setRemarks("First Loan deduction fee for member no "+member.getMemberNo()+" and name - "+member.getFullname());
        feeRepo.save(payment);
    }
    
    public FeePayment recordReJoiningFee(Member member, Long amount, LocalDateTime localDate, 
    		FinancialMonth financialMonth, Member depositor, CollectionLocation location, EmiPaymentGroup group) {
        if (amount <= 0) return null;
        
        FeePayment payment = new FeePayment();
        payment.setMember(member);
        payment.setAmount(amount.doubleValue());
        payment.setFeeType(FeeType.RE_JOINING_FEE);
        payment.setTransactionDateTime(localDate);
        payment.setFinancialMonth(financialMonth);
        payment.setDipositedBy(depositor);
        payment.setCollectionLocation(location);
        payment.setPaymentGroup(group);
        payment.setAddedBy(UserSession.getLoggedInUser());
        payment.setRemarks("Auto-generated rejoining fee for member no "+member.getMemberNo()+" and name - "+member.getFullname());
        return feeRepo.save(payment);
    }
    
    public FeePayment recordJoiningFee(Member member, Long amount, LocalDateTime localDate, 
    		FinancialMonth financialMonth, Member depositor, CollectionLocation location, EmiPaymentGroup group) {
        if (amount <= 0) return null;
        
        FeePayment payment = new FeePayment();
        payment.setMember(member);
        payment.setAmount(amount.doubleValue());
        payment.setFeeType(FeeType.JOINING_FEE);
        payment.setTransactionDateTime(localDate);
        payment.setFinancialMonth(financialMonth);
        payment.setDipositedBy(depositor);
        payment.setCollectionLocation(location);
        payment.setPaymentGroup(group);
        payment.setAddedBy(UserSession.getLoggedInUser());
        payment.setRemarks("Auto-generated joining fee for member no "+member.getMemberNo()+" and name - "+member.getFullname());
        return feeRepo.save(payment);
    }

    public FeePayment recordMonthlyFee(Member member, Long amount, LocalDateTime localDate, 
    		FinancialMonth financialMonth, Member depositor, CollectionLocation location, EmiPaymentGroup group) {
        if (amount <= 0) return null;
        
        addMonthlyFees(financialMonth, amount, localDate.toLocalDate());

        FeePayment payment = new FeePayment();
        payment.setMember(member);
        payment.setAmount(amount.doubleValue());
        payment.setFeeType(FeeType.MONTHLY_FEE);
        payment.setTransactionDateTime(localDate);
        payment.setFinancialMonth(financialMonth);
        payment.setDipositedBy(depositor);
        payment.setAddedBy(UserSession.getLoggedInUser());
        payment.setCollectionLocation(location);
        payment.setPaymentGroup(group);
        payment.setRemarks("Monthly fee for member no "+member.getMemberNo()+" and name - "+member.getFullname());
        return feeRepo.save(payment);
    }
    
    public MonthlyFees addMonthlyFees(FinancialMonth financialMonth, Long amount, LocalDate localDate) {
    	Optional<MonthlyFees> optional = monthlyFeesRepository.findByFinancialMonth(financialMonth);
        if(!optional.isPresent()) {
        	MonthlyFees monthlyFees = new MonthlyFees();
        	monthlyFees.setAmount(amount);
        	monthlyFees.setFinancialMonth(financialMonth);
        	monthlyFees.setAddedDate(localDate);
        	return monthlyFeesRepository.save(monthlyFees);
        }
        
        return optional.get();
    }
    
    public Optional<MonthlyFees> getMonthlyFeesByFinancialMonth(FinancialMonth financialMonth) {
    	return monthlyFeesRepository.findByFinancialMonth(financialMonth);
    }
    
    public Double getMemberTotalFees(Member member) {
    	return feeRepo.getMemberTotalFee(member.getId());
    }

    public Long getTotalMonthlyFeesDepositedInSociety() {
		return monthlyFeesRepository.sumOfMonthlyFeesAmount();
	}
    
    public Double getMemberFeeDeductionOnFirstLoan(Member member) {
    	Double totalSocietyFees = getTotalMonthlyFeesDepositedInSociety().doubleValue();
    	Double memberTotalFees = getMemberTotalFees(member);
    	System.err.println(memberTotalFees);
    	System.err.println(totalSocietyFees);
    	return  totalSocietyFees - memberTotalFees;
    }
}