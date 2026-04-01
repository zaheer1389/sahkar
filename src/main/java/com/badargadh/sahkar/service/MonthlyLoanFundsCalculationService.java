package com.badargadh.sahkar.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.badargadh.sahkar.data.AppConfig;
import com.badargadh.sahkar.data.EmiPayment;
import com.badargadh.sahkar.data.FinancialMonth;
import com.badargadh.sahkar.data.LoanAccount;
import com.badargadh.sahkar.data.LoanApplication;
import com.badargadh.sahkar.dto.MonthlyFundSummaryDTO;
import com.badargadh.sahkar.enums.ExpenseCategory;
import com.badargadh.sahkar.enums.ExpenseType;
import com.badargadh.sahkar.enums.FeeType;
import com.badargadh.sahkar.enums.LoanStatus;
import com.badargadh.sahkar.enums.MemberStatus;
import com.badargadh.sahkar.repository.EmiPaymentRepository;
import com.badargadh.sahkar.repository.FeePaymentRepository;
import com.badargadh.sahkar.repository.FeesRefundRepository;
import com.badargadh.sahkar.repository.LoanAccountRepository;
import com.badargadh.sahkar.repository.MemberRepository;
import com.badargadh.sahkar.repository.MonthlyExpenseRepository;
import com.badargadh.sahkar.repository.MonthlyFeesRepository;

@Service
public class MonthlyLoanFundsCalculationService {

	@Autowired private MemberRepository memberRepo;
	@Autowired private FeesRefundRepository refundRepo;
	@Autowired private FeePaymentRepository feePaymentRepository;
	@Autowired private MonthlyFeesRepository monthlyFeesRepository;
	@Autowired private EmiPaymentRepository emiPaymentRepository;
	@Autowired private LoanAccountRepository loanRepo;
	@Autowired private MonthlyExpenseRepository expenseRepo;
	
	@Autowired private AppConfigService appConfigService;
	
	private AppConfig appConfig;
	private int freshLoansEmiCounter = 0;
	
	public MonthlyFundSummaryDTO getMonthlyFundSummaryDTO(List<LoanApplication> applications, FinancialMonth month) {
		appConfig = appConfigService.getSettings();
		MonthlyFundSummaryDTO summaryDTO = new MonthlyFundSummaryDTO();
		summaryDTO.setOpeningBal(calculateOpeningBalance(month));
		summaryDTO.setNewMemberJoiningFees(calculateNewMemberJoiningFee(month));
		summaryDTO.setExpectedMonthlyFees(calculateExpectedFeesAmount());
		summaryDTO.setTotalTargetEmi(calculateTargetEmiCollection(month));
		summaryDTO.setFreshLoansEmiCounter(freshLoansEmiCounter);
		summaryDTO.setTotalFeesDeductions(calculateTotalExpectedFeesDeductions(applications));
		summaryDTO.setTotalFullPayment(calculateTotalFullPaymentsAmount(month));
		summaryDTO.setOtherMiscCredit(calculateMiscCredit(month));
		summaryDTO.setTotalRefundLiability(calculateTotalRefundLiability(month));
		summaryDTO.setOtherMiscDebit(calculateMiscDebit(month));
		return summaryDTO;
	}
	
	//Calculate opening balance
	private Double calculateOpeningBalance(FinancialMonth activeMonth) {
    	
    	LocalDate date = activeMonth.getStartDate().minusDays(1);
        
    	Double openingBal = appConfig.getOpeningBal();
    	
    	Double sumOfJoiningFees = feePaymentRepository.getSumByTypeBeforeMonth(date, FeeType.JOINING_FEE);
        Double sumOfMonthlyFees = feePaymentRepository.getSumByTypeBeforeMonth(date, FeeType.MONTHLY_FEE);
        Double sumOfLoanDeductionFees = feePaymentRepository.getSumByTypeBeforeMonth(date, FeeType.LOAN_DEDUCTION);        
        Double sumOfEmiPayments = emiPaymentRepository.sumOfTotalEmiPaymentsBeforeFinancialMonth(date);        
        Double sumOfFullPayments = emiPaymentRepository.sumOfTotalFullPaymentsBeforeFinancialMonth(date);        
        Double sumOfCreditExpenses = expenseRepo.sumOfAllCreditsBeforeMonth(date);
        
        
        Double sumOfTotalLoanDisbursed = loanRepo.sumOfAllDisbursementsBeforeMonth(date);        
        Double sumOfFeesRefunded = refundRepo.sumOfAllRefundsBeforeMonth(date);
        Double sumOfDebitExpenses = expenseRepo.sumOfAllDebitsBeforeMonth(date);
        
        Double totalIncome = sumOfJoiningFees 
        		+ sumOfMonthlyFees 
        		+ sumOfLoanDeductionFees 
        		+ sumOfEmiPayments
        		+ sumOfFullPayments 
        		+ sumOfCreditExpenses;
        
        Double totalExpense = sumOfTotalLoanDisbursed 
        		+ sumOfFeesRefunded
        		+ sumOfDebitExpenses;
        
        Double closingBal = openingBal + totalIncome - totalExpense;
    	
    	return closingBal;
    }
	
	private Double calculateNewMemberJoiningFee(FinancialMonth month) {
		return feePaymentRepository.getSumByType(month.getId(), FeeType.JOINING_FEE);
	}
	
	private Double calculateExpectedFeesAmount() {
		long activeCount = memberRepo.countByStatus(MemberStatus.ACTIVE);
		return activeCount * appConfig.getMonthlyFees().doubleValue();
	}

	//Calculate total expected fees deduction from new member loans
	private Double calculateTotalExpectedFeesDeductions(List<LoanApplication> applications) {
	    if (applications.isEmpty()) return 0.0;

	    // 1. Fetch Society Target ONCE (Static value for this calculation)
	    Double targetFees =  monthlyFeesRepository.sumOfMonthlyFeesAmount().doubleValue();

	    // 2. Extract all Member IDs from the applications
	    List<Long> memberIds = applications.stream()
	            .map(app -> app.getMember().getId())
	            .collect(Collectors.toList());

	    // 3. BATCH FETCH: Get all totals in one query
	    List<Object[]> results = feePaymentRepository.findTotalFeesForMembers(memberIds);
	    
	    // Convert results to a Map for instant lookup
	    Map<Long, Double> paidMap = results.stream()
	            .collect(Collectors.toMap(
	                res -> (Long) res[0], 
	                res -> (Double) res[1]
	            ));

	    // 4. Calculate total deduction using the map (No more DB calls in this loop!)
	    return applications.stream()
	            .mapToDouble(app -> {
	                Double actualPaid = paidMap.getOrDefault(app.getMember().getId(), 0.0);
	                double diff = targetFees - actualPaid;
	                return Math.max(0.0, diff); // Never return negative
	            })
	            .sum();
	}
	
	//Calculate total fees refund liability
	private Double calculateTotalRefundLiability(FinancialMonth targetMonth) {
        // 1. Get Actuals: Amount already paid out this month
        Double actualRefunds = refundRepo.sumRefundsByMonth(targetMonth.getId());

        // 2. Get Expected (Standard Cancelled): Cooling period ends by monthEnd
        // Using your existing native query logic
        Double expectedStandard = feePaymentRepository.calculateExpectedRefunds(targetMonth.getEndDate());

        // 3. Get Expected (Expired): 
        // Note: You should filter this to only include those NOT yet refunded
        Double expectedExpired = feePaymentRepository.calculateExpectedRefundsExpiredMember();

        // 4. Return the combined total
        // This represents: "What we already paid" + "What we still owe"
        return (actualRefunds != null ? actualRefunds : 0.0) + 
               (expectedStandard != null ? expectedStandard : 0.0) + 
               (expectedExpired != null ? expectedExpired : 0.0);
    }
    
    //Calculate expected monthly emi collection amount
    private double calculateTargetEmiCollection(FinancialMonth currentMonth) {
        double totalTarget = 0.0;

        // 1. Get all Active Loans
        List<LoanAccount> activeLoans = loanRepo.findAllByLoanStatus(LoanStatus.ACTIVE);
        for (LoanAccount loan : activeLoans) {
        	if(loan.getEmiAmount() == null && loan.getCreatedDate().isBefore(currentMonth.getStartDate())) {
        		freshLoansEmiCounter++;
        	}
        	double emiAmount = loan.getEmiAmount() != null ? loan.getEmiAmount() : 0;
            totalTarget += Math.min(emiAmount, loan.getPendingAmount());
        }
        List<EmiPayment> emiPayments = emiPaymentRepository.findByFinancialMonth(currentMonth);

        // 2. Get Paid Loans that were closed THIS month
        // We look for EMI payments marked as 'fullPayment' in the current financial month
        List<LoanAccount> closurePayments = loanRepo.findAllByLoanStatusAndEndDateBetween(LoanStatus.PAID, currentMonth.getStartDate(), currentMonth.getEndDate());
        //System.err.println("Closed loans : "+closurePayments.size());
        for (LoanAccount loan : closurePayments) {
        	Optional<EmiPayment> paymentOpt = emiPayments.stream()
                    .filter(e -> e.getLoanAccount().getId().equals(loan.getId()))
                    .findFirst();
        	double emi = paymentOpt.isPresent() ? paymentOpt.get().getAmountPaid() : 0;
        	totalTarget += emi;
        	//System.err.println("Closer amount : "+emi);
        }

        return totalTarget;
    }
    
    private Double calculateTotalFullPaymentsAmount(FinancialMonth month) {
    	return emiPaymentRepository.sumOfTotalFullPaymentsByFinancialMonth(month.getId());
    }
    
    private Double calculateMiscCredit(FinancialMonth month) {
    	return expenseRepo.sumAmountByType(month.getId(), ExpenseType.CREDIT, ExpenseCategory.PAYMENT_CREDIT_FROM_JAMMAT_BADARGADH);
    }
    
    private Double calculateMiscDebit(FinancialMonth month) {
    	return expenseRepo.sumAmountByType(month.getId(), ExpenseType.DEBIT, ExpenseCategory.PAYMENT_DEBIT_TO_JAMMAT_BADARGADH);
    }

}
