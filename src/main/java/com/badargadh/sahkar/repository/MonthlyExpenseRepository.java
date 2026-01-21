package com.badargadh.sahkar.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.badargadh.sahkar.data.FinancialMonth;
import com.badargadh.sahkar.data.MonthlyExpense;
import com.badargadh.sahkar.enums.ExpenseCategory;
import com.badargadh.sahkar.enums.ExpenseType;

@Repository
public interface MonthlyExpenseRepository extends JpaRepository<MonthlyExpense, Long> {
	
    // Fetch all entries for the active month log
    List<MonthlyExpense> findByFinancialMonthOrderByIdDesc(FinancialMonth month);
    
    @Query("SELECT COALESCE(SUM(e.amount), 0.0) FROM MonthlyExpense e " +
            "WHERE e.financialMonth.id = :monthId AND e.type = :type and e.category != 'JAMMAT_OPENING_BALANCE' ")
     Double sumAmountByType(@Param("monthId") Long monthId, @Param("type") ExpenseType type);

     @Query("SELECT COALESCE(SUM(e.amount), 0.0) FROM MonthlyExpense e " +
            "WHERE e.financialMonth.id = :monthId AND e.category = :category")
     Double sumAmountByCategory(@Param("monthId") Long monthId, @Param("category") ExpenseCategory category);
     
     /**
      * Sums all Credit transactions (Income/Jammat Recovery) 
      * from all months prior to the specified month ID.
      */
     @Query("SELECT COALESCE(SUM(e.amount), 0.0) FROM MonthlyExpense e " +
            "WHERE e.financialMonth.startDate < :startDate AND e.type = com.badargadh.sahkar.enums.ExpenseType.CREDIT "
            + "and e.category != 'JAMMAT_OPENING_BALANCE'")
     Double sumOfAllCreditsBeforeMonth(@Param("startDate") LocalDate startDate);

     /**
      * Sums all Debit transactions (Expenses/Jammat Lending) 
      * from all months prior to the specified month ID.
      */
     @Query("SELECT COALESCE(SUM(e.amount), 0.0) FROM MonthlyExpense e " +
            "WHERE e.financialMonth.startDate < :startDate AND e.type = com.badargadh.sahkar.enums.ExpenseType.DEBIT")
     Double sumOfAllDebitsBeforeMonth(@Param("startDate") LocalDate startDate);
}