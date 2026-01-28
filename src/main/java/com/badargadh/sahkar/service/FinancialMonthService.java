package com.badargadh.sahkar.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.badargadh.sahkar.data.FinancialMonth;
import com.badargadh.sahkar.data.MonthlyStatementDTO;
import com.badargadh.sahkar.enums.ExpenseType;
import com.badargadh.sahkar.enums.FeeType;
import com.badargadh.sahkar.enums.MonthStatus;
import com.badargadh.sahkar.repository.EmiPaymentRepository;
import com.badargadh.sahkar.repository.FeePaymentRepository;
import com.badargadh.sahkar.repository.FeesRefundRepository;
import com.badargadh.sahkar.repository.FinancialMonthRepository;
import com.badargadh.sahkar.repository.LoanAccountRepository;
import com.badargadh.sahkar.repository.MonthlyExpenseRepository;
import com.badargadh.sahkar.util.AppLogger;

@Service
public class FinancialMonthService {

    @Autowired private FinancialMonthRepository monthRepo;
    @Autowired private EmiPaymentRepository emiRepo;
    @Autowired private MonthlyExpenseRepository expenseRepo;
    @Autowired private LoanAccountRepository loanRepo;
    @Autowired private FeesRefundRepository refundRepo;
    @Autowired private FeePaymentRepository feeRepo;
    @Autowired private AppConfigService appConfigService;
    @Autowired private FeesRefundRepository feesRefundRepository;
    //@Autowired MonthlyStatementService statementService; 
    /**
     * Fetches all records for the TableView.
     */
    public List<FinancialMonth> getAllMonths() {
        return monthRepo.findAll();
    }

    /**
     * Gets the current open month, if any.
     */
    public Optional<FinancialMonth> getActiveMonth() {
        return monthRepo.findByStatus(MonthStatus.OPEN);
    }

    /**
     * Opens a month based on ComboBox selection.
     */
    @Transactional
    public FinancialMonth openMonthByDetails(String name, int year) {
        // 1. Rule: Cannot open a month if another is already OPEN
        monthRepo.findByStatus(MonthStatus.OPEN).ifPresent(m -> {
            throw new IllegalStateException("Process Blocked: " + m.getMonthName() + " is already OPEN. Close it first.");
        });

        FinancialMonth target;
        Optional<FinancialMonth> optional = monthRepo.findByMonthNameAndYear(name, year);

        if (optional.isPresent()) {
            target = optional.get();
            // Rule: Cannot re-open a month that was already CLOSED
            /*if (target.getStatus() == MonthStatus.CLOSED) {
                throw new IllegalStateException("Forbidden: " + name + "-" + year + " is already CLOSED and finalized.");
            }*/
        } else {
        	LocalDate startDate = LocalDate.of(year, Month.valueOf(name), 1);
        	LocalDate endDate = YearMonth.of(year, Month.valueOf(name)).atEndOfMonth();
            
        	// Create new record if it doesn't exist
            target = new FinancialMonth();
            target.setMonthName(name.toUpperCase());
            target.setYear(year);        
            target.setMonthId(name.toUpperCase() + "-" + year);
            target.setStartDate(startDate);
            target.setEndDate(endDate);
            
            // Optional: Inherit balance from the last closed month
            // Double lastClosing = monthRepo.findTopByOrderByIdDesc().map(FinancialMonth::getClosingBalance).orElse(0.0);
            // target.setOpeningBalance(lastClosing);
            // target.setClosingBalance(lastClosing);
        }

        // 2. Set Status and Start Date
        target.setStatus(MonthStatus.OPEN);
        
        try {
            // Convert String "JANUARY" to Integer 1
            //int monthValue = java.time.Month.valueOf(name.toUpperCase()).getValue();
            //target.setStartDate(LocalDate.of(year, monthValue, 1));
        } catch (IllegalArgumentException e) {
        	AppLogger.error("financial month service error", e);
            throw new RuntimeException("Invalid month name provided: " + name);            
        }

        return monthRepo.save(target);
    }

    /**
     * Closes the active month and carries the balance forward.
     */
    @Transactional
    public void closeActiveMonth() {
        // 1. Get the current active month
        FinancialMonth active = monthRepo.findByStatus(MonthStatus.OPEN)
                .orElseThrow(() -> new IllegalStateException("No active month found to close."));

        // 2. Lock the current month
        active.setStatus(MonthStatus.CLOSED);
        active.setEndDate(active.getStartDate().with(TemporalAdjusters.lastDayOfMonth()));
        active.setOpeningBalance(calculateOpeningBalance(active));
        monthRepo.save(active);

        // 3. Carry Forward Logic: Find the next BLANK month
        monthRepo.findFirstByIdGreaterThanAndStatusOrderByIdAsc(active.getId(), MonthStatus.BLANK)
                .ifPresent(nextMonth -> {
                    // Set the next month's opening balance to this month's closing balance
                    nextMonth.setOpeningBalance(active.getClosingBalance());
                    // Update its current closing balance to match opening (until transactions happen)
                    nextMonth.setClosingBalance(active.getClosingBalance());
                    monthRepo.save(nextMonth);
                });
    }
    
    public Optional<FinancialMonth> getMonthFromMonthAndYear(String month, int year) {
    	return monthRepo.findByMonthNameAndYear(month, year);
    }
    
    public boolean isValidTransactionDate(LocalDateTime localDateTime) {
        // 1. Retrieve the currently active month
        FinancialMonth activeMonth = getActiveMonth().get();           
        // 2. Return true ONLY if the date is within the start and end range (inclusive)
        return !localDateTime.toLocalDate().isBefore(activeMonth.getStartDate()) 
               && !localDateTime.toLocalDate().isAfter(activeMonth.getEndDate());
    }
    
    public Double calculateOpeningBalance(FinancialMonth activeMonth) {
    	
    	LocalDate date = activeMonth.getStartDate().minusDays(1);
        
    	Double openingBal = appConfigService.getSettings().getOpeningBal();
    	
    	Double sumOfJoiningFees = feeRepo.getSumByTypeBeforeMonth(date, FeeType.JOINING_FEE);
        Double sumOfMonthlyFees = feeRepo.getSumByTypeBeforeMonth(date, FeeType.MONTHLY_FEE);
        Double sumOfLoanDeductionFees = feeRepo.getSumByTypeBeforeMonth(date, FeeType.LOAN_DEDUCTION);        
        Double sumOfEmiPayments = emiRepo.sumOfTotalEmiPaymentsBeforeFinancialMonth(date);        
        Double sumOfFullPayments = emiRepo.sumOfTotalFullPaymentsBeforeFinancialMonth(date);        
        Double sumOfCreditExpenses = expenseRepo.sumOfAllCreditsBeforeMonth(date);
        
        
        Double sumOfTotalLoanDisbursed = loanRepo.sumOfAllDisbursementsBeforeMonth(date);        
        Double sumOfFeesRefunded = feesRefundRepository.sumOfAllRefundsBeforeMonth(date);
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
    
    public Double calculateMonthTotals(FinancialMonth activeMonth) {
    	
    	LocalDate date = activeMonth.getStartDate().minusDays(1);

    	Double sumOfJoiningFees = feeRepo.getSumByTypeBeforeMonth(date, FeeType.JOINING_FEE);
        Double sumOfMonthlyFees = feeRepo.getSumByTypeBeforeMonth(date, FeeType.MONTHLY_FEE);
        Double sumOfLoanDeductionFees = feeRepo.getSumByTypeBeforeMonth(date, FeeType.LOAN_DEDUCTION);        
        Double sumOfEmiPayments = emiRepo.sumOfTotalEmiPaymentsBeforeFinancialMonth(date);        
        Double sumOfFullPayments = emiRepo.sumOfTotalFullPaymentsBeforeFinancialMonth(date);        
        Double sumOfCreditExpenses = expenseRepo.sumOfAllCreditsBeforeMonth(date);
        
        
        Double sumOfTotalLoanDisbursed = loanRepo.sumOfAllDisbursementsBeforeMonth(date);        
        Double sumOfFeesRefunded = feesRefundRepository.sumOfAllRefundsBeforeMonth(date);
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
        
        Double closingBal = totalIncome - totalExpense;
    	
    	return closingBal;
    }
    
    @Transactional
    public void recalculateAllBalances() {
        List<FinancialMonth> allMonths = monthRepo.findAllByOrderByIdAsc();
        for (FinancialMonth month : allMonths) {
        	
        	Long monthId = month.getId();
        	
        	Double openingBal = calculateOpeningBalance(month);
        	
        	// --- INCOME SECTION ---
            double newMemberFee = feeRepo.getSumByType(monthId, FeeType.JOINING_FEE);
            double monthlyFee = feeRepo.getSumByType(monthId, FeeType.MONTHLY_FEE);
            double loanDeductionFee = feeRepo.getSumByType(monthId, FeeType.LOAN_DEDUCTION);
            double expensesCredit = expenseRepo.sumAmountByType(monthId, ExpenseType.CREDIT);
            
            // EMI Total
            double emiTotal = emiRepo.sumOfTotalEmiPaymentsByFinancialMonth(monthId);
            
            // Full Loan Payments (Closures)
            double fullPayments = emiRepo.sumOfTotalFullPaymentsByFinancialMonth(monthId);

            // --- OUTGOING SECTION ---
            double loanGrantedAmount = loanRepo.sumOfLoanDisbursedAmount(monthId);
            double feeRefunds = refundRepo.sumRefundsByMonth(monthId);
            
            double expenseDebit = expenseRepo.sumAmountByType(monthId, ExpenseType.DEBIT);
            
            double income = newMemberFee + monthlyFee + loanDeductionFee + expensesCredit + emiTotal + fullPayments;
            double expenses = loanGrantedAmount + feeRefunds + expenseDebit;
            
            Double closingBal = openingBal + income - expenses;
            
            month.setOpeningBalance(openingBal);
            month.setClosingBalance(closingBal);
            
            
        }
    }
   
}