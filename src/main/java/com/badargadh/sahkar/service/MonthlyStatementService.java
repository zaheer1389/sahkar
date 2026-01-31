package com.badargadh.sahkar.service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.badargadh.sahkar.enums.ExpenseCategory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.badargadh.sahkar.data.EmiPayment;
import com.badargadh.sahkar.data.FeesRefund;
import com.badargadh.sahkar.data.FinancialMonth;
import com.badargadh.sahkar.data.LoanAccount;
import com.badargadh.sahkar.data.Member;
import com.badargadh.sahkar.data.MonthlyStatementDTO;
import com.badargadh.sahkar.enums.ExpenseType;
import com.badargadh.sahkar.enums.FeeType;
import com.badargadh.sahkar.enums.LoanStatus;
import com.badargadh.sahkar.repository.EmiPaymentRepository;
import com.badargadh.sahkar.repository.FeePaymentRepository;
import com.badargadh.sahkar.repository.FeesRefundRepository;
import com.badargadh.sahkar.repository.LoanAccountRepository;
import com.badargadh.sahkar.repository.MemberHistoryRepository;
import com.badargadh.sahkar.repository.MemberRepository;
import com.badargadh.sahkar.repository.MonthlyExpenseRepository;

@Service
public class MonthlyStatementService {
	
    @Autowired private FeePaymentRepository feeRepo;
    @Autowired private EmiPaymentRepository emiRepo;
    @Autowired private LoanAccountRepository loanRepo;
    @Autowired private MonthlyExpenseRepository expenseRepo;
    @Autowired private FeesRefundRepository refundRepo;
    @Autowired private FinancialMonthService financialMonthService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private MemberHistoryRepository historyRepository;

    public MonthlyStatementDTO getActiveMonthStatement(FinancialMonth month) {
    	
    	Long monthId = month.getId();
    	
        MonthlyStatementDTO dto = new MonthlyStatementDTO();
        
        Double openingBal = financialMonthService.calculateOpeningBalance(month);

        dto.setOpeningBal(openingBal);
        
        List<EmiPayment> emiPayments = emiRepo.findByFinancialMonth(month);
        
        Map<Double, Long> emiBreakdown = emiPayments.stream()
								        		.filter(p -> p.getAmountPaid() != null && p.getAmountPaid() > 0)
								        	    .collect(Collectors.groupingBy(
								        	    		EmiPayment::getAmountPaid, 
								        	            TreeMap::new, // TreeMap keeps the amounts sorted (100, 200, 300...)
								        	            Collectors.counting()
							        	        ));
        dto.setEmiBreakdown(emiBreakdown);
        
        // --- INCOME SECTION ---
        dto.setNewMemberFee(feeRepo.getSumByType(monthId, FeeType.JOINING_FEE));
        dto.setMonthlyFee(feeRepo.getSumByType(monthId, FeeType.MONTHLY_FEE));
        dto.setLoanDeduction(feeRepo.getSumByType(monthId, FeeType.LOAN_DEDUCTION));
        dto.setExpenseCredit(expenseRepo.sumAmountByType(monthId, ExpenseType.CREDIT, ExpenseCategory.PAYMENT_CREDIT_FROM_JAMMAT_BADARGADH));
        
        // EMI Total
        dto.setTotalEmi(emiRepo.sumOfTotalEmiPaymentsByFinancialMonth(monthId));
        
        // Full Loan Payments (Closures)
        dto.setFullPaymentAmount(emiRepo.sumOfTotalFullPaymentsByFinancialMonth(monthId));

        // --- OUTGOING SECTION ---
        dto.setTotalLoanGranted(loanRepo.sumOfLoanDisbursedAmount(monthId));
        dto.setTotalFeeRefund(refundRepo.sumRefundsByMonth(monthId));
        
        dto.setExpenseDebit(expenseRepo.sumAmountByType(monthId, ExpenseType.DEBIT, ExpenseCategory.PAYMENT_DEBIT_TO_JAMMAT_BADARGADH));
        
        List<Member> cancelledMembers = memberRepository.findCancelledMemberBtweenDates(LocalDateTime.of(month.getStartDate(), LocalTime.of(0, 0))
        										, LocalDateTime.of(month.getEndDate(), LocalTime.of(23, 59)));
        
        List<Member> newMembers = memberRepository.findAllByFinancialMonth(month);
        
        List<FeesRefund> feesRefunds = refundRepo.findByFinancialMonth(month);
        
        List<LoanAccount> loanAccounts = loanRepo.findByFinancialMonth(month);
        
        List<EmiPayment> payments = emiRepo.findByFinancialMonth(month).stream().filter(payment -> payment.getFullPaymentAmount() > 0).toList();
        
        List<LoanAccount> accounts = loanRepo.findByStartDateBetweenAndLoanStatus(month.getStartDate(), month.getEndDate(), LoanStatus.ACTIVE);
        
        dto.setCancelledMembersData(cancelledMembers.stream().map(member -> List.of(
        	getMemberNo(member),
    		member.getGujFullname()
    	)).collect(Collectors.toList()));
        
        dto.setNewMembersData(newMembers.stream().map(member -> List.of(
        	getMemberNo(member),
    		member.getGujFullname()
    	)).collect(Collectors.toList()));
        
        dto.setRefundMembersData(feesRefunds.stream().map(member -> List.of(
        	getMemberNo(member.getMember()),
    		member.getMember().getGujFullname(),
    		String.format("%.0f",member.getAmount())
    	)).collect(Collectors.toList()));
        
        dto.setNewLoanListMembersData(loanAccounts.stream().map(member -> List.of(
        	getMemberNo(member.getMember()),
    		member.getMember().getGujFullname()
    	)).collect(Collectors.toList()));
        
        dto.setFullpaymentsData(payments.stream().map(payment -> List.of(
        	getMemberNo(payment.getMember()),
        	payment.getMember().getGujFullname(),
        	String.format("%.0f", payment.getFullPaymentAmount())
        )).toList());
        
        dto.setPrevMonthNewLoansEMIAmtData(accounts.stream().map(account -> List.of(
        	account.getMember().getMemberNo()+"",
        	account.getMember().getGujFullname(),
        	account.getEmiAmount().intValue()+""
        )).toList());

        return dto;
    }
    
    private String getMemberNo(Member member) {
    	return member.getMemberNo() != null 
    			?  member.getMemberNo() +""
    			:  historyRepository.findByOriginalMemberId(member.getId()).getMemberNo()+"";
    }
}