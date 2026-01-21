package com.badargadh.sahkar.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.badargadh.sahkar.data.EmiPayment;
import com.badargadh.sahkar.data.FinancialMonth;
import com.badargadh.sahkar.data.LoanAccount;
import com.badargadh.sahkar.data.LoanApplication;
import com.badargadh.sahkar.data.Member;

@Repository
public interface EmiPaymentRepository extends JpaRepository<EmiPayment, Long> {
	
	Optional<EmiPayment> findByMemberAndFinancialMonth(Member member, FinancialMonth financialMonth);
	
    boolean existsByMemberAndFinancialMonth(Member member, FinancialMonth financialMonth);
    
    boolean existsByLoanAccountAndFinancialMonth(LoanAccount loanAccount, FinancialMonth financialMonth);
    
    List<EmiPayment> findByFinancialMonth(FinancialMonth month);
    
    @Query("SELECT p FROM EmiPayment p WHERE YEAR(p.paymentDateTime) = :year")
    List<EmiPayment> findAllPaymentsByYear(@Param("year") int year);
    
    // Find the latest application date in the system
    @Query("SELECT MAX(e.paymentDateTime) FROM EmiPayment e")
    LocalDateTime findMaxEmiPaymentDateTime();
    
    @Query("SELECT COALESCE(SUM(e.amountPaid), 0.0) FROM EmiPayment e " +
            "WHERE e.financialMonth.id = :monthId")
    Double sumOfTotalEmiPaymentsByFinancialMonth(@Param("monthId") Long monthId);
    
    @Query("SELECT COALESCE(SUM(e.fullPaymentAmount), 0.0) FROM EmiPayment e " +
            "WHERE e.financialMonth.id = :monthId")
    Double sumOfTotalFullPaymentsByFinancialMonth(@Param("monthId") Long monthId);
    
    List<EmiPayment> findByFinancialMonthAndFullPaymentIsTrue(FinancialMonth financialMonth);
    
    @Query("SELECT COALESCE(SUM(e.amountPaid), 0.0) FROM EmiPayment e " +
            "WHERE e.financialMonth.startDate < :startDate")
    Double sumOfTotalEmiPaymentsBeforeFinancialMonth(@Param("startDate") LocalDate startDate);
    
    @Query("SELECT COALESCE(SUM(e.fullPaymentAmount), 0.0) FROM EmiPayment e " +
            "WHERE e.financialMonth.startDate < :startDate")
    Double sumOfTotalFullPaymentsBeforeFinancialMonth(@Param("startDate") LocalDate startDate);
}