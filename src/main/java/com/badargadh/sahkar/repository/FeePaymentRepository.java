package com.badargadh.sahkar.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.badargadh.sahkar.data.FeePayment;
import com.badargadh.sahkar.data.FinancialMonth;
import com.badargadh.sahkar.data.Member;
import com.badargadh.sahkar.enums.FeeType;

@Repository
public interface FeePaymentRepository extends JpaRepository<FeePayment, Long> {
    // Get the latest transaction for a member to find the current running balance
    Optional<FeePayment> findTopByMemberOrderByIdDesc(Member member);
    
    // Sum all fees collected in a specific financial month for closing balance reports
    @Query("SELECT SUM(f.amount) FROM FeePayment f WHERE f.financialMonth = :month")
    Double sumByFinancialMonth(@Param("month") FinancialMonth month);
    
    @Query("SELECT SUM(f.amount) FROM FeePayment f WHERE f.member.id = :memberId")
    Double getMemberTotalFee(@Param("memberId") Long memberId);
    
    int countByMemberAndFeeType(Member member, FeeType feeType);
    
    boolean existsByMemberAndFinancialMonth(Member member, FinancialMonth financialMonth);
    
    // Find the latest application date in the system
    @Query("SELECT MAX(f.transactionDateTime) FROM FeePayment f")
    LocalDateTime findMaxFeePaymentDateTime();
    
    @Query("SELECT COALESCE(SUM(f.amount), 0.0) FROM FeePayment f " +
            "WHERE f.financialMonth.id = :monthId AND f.feeType = :feeType")
    Double getMemberTotalFeeByType(@Param("monthId") Long monthId, @Param("feeType") FeeType feeType);
    
    @Query("SELECT COALESCE(SUM(f.amount), 0.0) FROM FeePayment f " +
            "WHERE f.financialMonth.id = :monthId AND f.feeType = :feeType")
    Double getSumByType(@Param("monthId") Long monthId, @Param("feeType") FeeType feeType);
    
    boolean existsByMemberAndFinancialMonthAndFeeType(Member member, FinancialMonth financialMonth, FeeType feeType);
    
    @Query("SELECT COALESCE(SUM(f.amount), 0.0) FROM FeePayment f " +
	       "WHERE f.financialMonth.startDate < :startDate AND f.feeType = :feeType")
	Double getSumByTypeBeforeMonth(@Param("startDate") LocalDate startDate, @Param("feeType") FeeType feeType);

    
    @Query(value = "SELECT COALESCE(SUM(f.amount), 0) FROM fee_payments f " +
	       "JOIN members m ON f.member_id = m.id " +
	       "CROSS JOIN app_settings c " + // Join config to get the dynamic cooling period
	       "WHERE m.member_status = 'CANCELLED' " +
	       "AND m.cancellation_reason != 'MEMBER_EXPIRED' " + // Expired members are usually handled separately
	       "AND (" +
	       "    CASE " +
	       "        WHEN m.cancellation_date_time < '2026-01-01' " +
	       "        THEN DATE_ADD(m.cancellation_date_time, INTERVAL 3 MONTH) " +
	       "        ELSE DATE_ADD(m.cancellation_date_time, INTERVAL c.fees_refund_cooling_period MONTH) " +
	       "    END" +
	       ") <= :monthEnd", 
	       nativeQuery = true)
	Double calculateExpectedRefunds(@Param("monthEnd") LocalDate monthEnd);
    
    @Query("SELECT SUM(f.amount) FROM FeePayment f WHERE f.member.status IN ('ACTIVE', 'CANCELLED')")
    Double sumAllFees();
    
    @Query("SELECT f FROM FeePayment f " +
            "WHERE YEAR(f.transactionDateTime) = :year")
    List<FeePayment> findByYear(@Param("year") int year);
}