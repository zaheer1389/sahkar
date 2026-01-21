package com.badargadh.sahkar.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.badargadh.sahkar.data.FinancialMonth;
import com.badargadh.sahkar.data.MonthlyExpense;
import com.badargadh.sahkar.enums.ExpenseCategory;
import com.badargadh.sahkar.exception.BusinessException;
import com.badargadh.sahkar.repository.MonthlyExpenseRepository;

@Service
public class MonthlyExpenseService {

    @Autowired private MonthlyExpenseRepository expenseRepo;
    @Autowired private FinancialMonthService monthService;

    public List<MonthlyExpense> getExpensesForActiveMonth() {
        return monthService.getActiveMonth()
                .map(expenseRepo::findByFinancialMonthOrderByIdDesc)
                .orElse(List.of());
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
        
        // Sum of all money lent out
        double totalDebit = all.stream()
            .filter(e -> e.getCategory() == ExpenseCategory.PAYMENT_DEBIT_TO_JAMMAT_BADARGADH)
            .mapToDouble(MonthlyExpense::getAmount)
            .sum();

        // Sum of all money returned back
        double totalCredit = all.stream()
            .filter(e -> e.getCategory() == ExpenseCategory.PAYMENT_CREDIT_FROM_JAMMAT_BADARGADH)
            .mapToDouble(MonthlyExpense::getAmount)
            .sum();

        return totalDebit - totalCredit;
    }
}