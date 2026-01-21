package com.badargadh.sahkar.repository;

import com.badargadh.sahkar.data.FeesRefund;
import com.badargadh.sahkar.data.FinancialMonth;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FeesRefundRepository extends JpaRepository<FeesRefund, Long> {


    @Query("SELECT COALESCE(SUM(r.amount), 0.0) FROM FeesRefund r " +
           "WHERE r.financialMonth.id = :monthId")
    Double sumRefundsByMonth(@Param("monthId") Long monthId);
    
    @Query("SELECT COALESCE(SUM(r.amount), 0.0) FROM FeesRefund r " +
            "WHERE r.financialMonth.startDate < :startDate")
    Double sumOfAllRefundsBeforeMonth(@Param("startDate") LocalDate startDate);
    
    List<FeesRefund> findByFinancialMonth(FinancialMonth month);
}