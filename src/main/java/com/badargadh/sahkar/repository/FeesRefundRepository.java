package com.badargadh.sahkar.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.badargadh.sahkar.data.FeesRefund;
import com.badargadh.sahkar.data.FinancialMonth;

@Repository
public interface FeesRefundRepository extends JpaRepository<FeesRefund, Long> {


    @Query("SELECT COALESCE(SUM(r.amount), 0.0) FROM FeesRefund r " +
           "WHERE r.financialMonth.id = :monthId")
    Double sumRefundsByMonth(@Param("monthId") Long monthId);
    
    @Query("SELECT COALESCE(SUM(r.amount), 0.0) FROM FeesRefund r " +
            "WHERE r.financialMonth.startDate < :startDate")
    Double sumOfAllRefundsBeforeMonth(@Param("startDate") LocalDate startDate);
    
    List<FeesRefund> findByFinancialMonth(FinancialMonth month);
    
    @Query(value = "SELECT m.month_id, COUNT(f.id) " +
            "FROM fees_refunds f " +
            "JOIN financial_months m ON f.financial_month_id = m.id " +
            "WHERE f.financial_month_id IS NOT NULL AND m.start_date > :cutoff " +
            "GROUP BY m.month_id, m.start_date "+
            "ORDER BY m.start_date ASC", nativeQuery = true)
    List<Object[]> getFeesRefundCountsByFinancialMonthNative(@Param("cutoff") LocalDateTime cutoff);
    
    
}