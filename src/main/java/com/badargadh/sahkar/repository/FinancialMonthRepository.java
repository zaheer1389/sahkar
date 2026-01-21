package com.badargadh.sahkar.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.badargadh.sahkar.data.FinancialMonth;
import com.badargadh.sahkar.enums.MonthStatus;

@Repository
public interface FinancialMonthRepository extends JpaRepository<FinancialMonth, Long> {

	List<FinancialMonth> findAllByOrderByIdAsc();
	
	List<FinancialMonth> findAllByYearOrderByIdAsc(int year);
	
    // Find the currently active month
    Optional<FinancialMonth> findByStatus(MonthStatus status);

    // Find a month by its specific name and year (for ComboBox selection)
    Optional<FinancialMonth> findByMonthNameAndYear(String monthName, int year);

    // Find the next available month to carry forward the balance
    // This finds the month with the next highest ID that is currently BLANK
    Optional<FinancialMonth> findFirstByIdGreaterThanAndStatusOrderByIdAsc(Long id, MonthStatus status);
    
    @Query("SELECT e.amountPaid, COUNT(e), SUM(e.amountPaid) FROM EmiPayment e " +
    	       "WHERE e.financialMonth.id = :monthId " +
    	       "GROUP BY e.amountPaid")
    List<Object[]> getEmiSummaryByAmount(@Param("monthId") Long monthId);
}