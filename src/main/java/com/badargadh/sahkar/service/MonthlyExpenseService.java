package com.badargadh.sahkar.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.badargadh.sahkar.data.AppConfig;
import com.badargadh.sahkar.data.FinancialMonth;
import com.badargadh.sahkar.data.Member;
import com.badargadh.sahkar.data.MonthlyExpense;
import com.badargadh.sahkar.dto.ExpenseSummaryDTO;
import com.badargadh.sahkar.enums.ExpenseCategory;
import com.badargadh.sahkar.enums.ExpenseType;
import com.badargadh.sahkar.exception.BusinessException;
import com.badargadh.sahkar.repository.MonthlyExpenseRepository;

@Service
public class MonthlyExpenseService {

	@Autowired AppConfigService appConfigService;
    @Autowired private MonthlyExpenseRepository expenseRepo;
    @Autowired private FinancialMonthService monthService;

    public List<MonthlyExpense> getExpensesForActiveMonth() {
        return monthService.getActiveMonth()
                .map(expenseRepo::findByFinancialMonthOrderByIdDesc)
                .orElse(List.of());
    }

    public List<MonthlyExpense> getExpenses() {
        return expenseRepo.findAllByOrderByDateDesc();
    }

    @Transactional
    public void saveExpense(MonthlyExpense expense) {
        // 1. Retrieve the currently OPEN month
        FinancialMonth activeMonth = monthService.getActiveMonth()
                .orElseThrow(() -> new BusinessException("Cannot save: No financial month is currently OPEN."));
        
        // 2. Date Validation
        /*LocalDate expenseDate = expense.getDate();
        if (expenseDate.isBefore(activeMonth.getStartDate()) || 
            expenseDate.isAfter(activeMonth.getEndDate())) {
            
            throw new BusinessException("Expense date (" + expenseDate + ") must be between " + 
                                      activeMonth.getStartDate() + " and " + activeMonth.getEndDate());
        }*/

        expense.setFinancialMonth(activeMonth);
        expenseRepo.save(expense);
    }
    
    public Double getJammatOutstandingBalance() {
        List<MonthlyExpense> all = expenseRepo.findAll();

        ExpenseSummaryDTO summary = all.stream().collect(
    			ExpenseSummaryDTO::new,
		    (acc, e) -> {
		        double amt = e.getAmount();
		        if (e.getCategory() == ExpenseCategory.JAMMAT_OPENING_BALANCE) {
		        	acc.opening += amt;
		        }
		        if (e.getCategory() == ExpenseCategory.PAYMENT_DEBIT_TO_JAMMAT_BADARGADH) {
		        	acc.debit += amt;
		        }
		        if (e.getCategory() == ExpenseCategory.PAYMENT_CREDIT_FROM_JAMMAT_BADARGADH) {
		        	acc.credit += amt;
		        }
		    },
		    (acc1, acc2) -> { // For parallel stream support
		        acc1.opening += acc2.opening;
		        acc1.debit += acc2.debit;
		        acc1.credit += acc2.credit;
		    }
		);

        return summary.getDebit() - summary.getOpening() - summary.getCredit();
    }
    
    public Double getMonthlyExpenseBalance() {
    	List<MonthlyExpense> all = expenseRepo.findAll();
    	
    	ExpenseSummaryDTO summary = all.stream().collect(
    			ExpenseSummaryDTO::new,
		    (acc, e) -> {
		        double amt = e.getAmount();
		        if (e.getCategory() == ExpenseCategory.EXPENSE_OPENING_BALANCE) {
		        	acc.opening += amt;
		        }
		        if (e.getCategory() == ExpenseCategory.STATIONERY || e.getCategory() == ExpenseCategory.MISC) {
		        	acc.debit += amt;
		        }
		        if (e.getCategory() == ExpenseCategory.MEMBER_FEE || e.getCategory() == ExpenseCategory.LOAN_BOOK_CREDIT) {
		        	acc.credit += amt;
		        }
		    },
		    (acc1, acc2) -> { // For parallel stream support
		        acc1.opening += acc2.opening;
		        acc1.debit += acc2.debit;
		        acc1.credit += acc2.credit;
		    }
		);
    	
        return summary.getOpening() + summary.getCredit() - summary.getDebit();
    } 
    
    public void addNewMemberFeeAsExpenseCredit(Member member, FinancialMonth month) {
    	AppConfig appConfig = appConfigService.getSettings();
    	
    	String remarks ="New member fees - "+member.getMemberNo()+" "+member.getFullname();
    	MonthlyExpense expense = new MonthlyExpense();
    	expense.setAmount(appConfig.getNewMemberStationaryFees().doubleValue());
    	expense.setCategory(ExpenseCategory.MEMBER_FEE);
    	expense.setType(ExpenseType.CREDIT);
    	expense.setDate(LocalDate.now());
    	expense.setRemarks(remarks);
    	expense.setFinancialMonth(month);
    	expenseRepo.save(expense);
    }
}