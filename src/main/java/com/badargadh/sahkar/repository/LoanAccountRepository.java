package com.badargadh.sahkar.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.badargadh.sahkar.data.FinancialMonth;
import com.badargadh.sahkar.data.LoanAccount;
import com.badargadh.sahkar.data.LoanApplication;
import com.badargadh.sahkar.data.Member;
import com.badargadh.sahkar.enums.LoanStatus;

@Repository
public interface LoanAccountRepository extends JpaRepository<LoanAccount, Long> {
	
	// Find the active loan for a member
	// Finds the most recent closed loan for a specific member
	Optional<LoanAccount> findFirstByMemberAndLoanStatusOrderByEndDateDesc(Member member, LoanStatus loanStatus);
    
    // Find the active loan for a member
    Optional<LoanAccount> findByMemberAndLoanStatus(Member member, LoanStatus loanStatus);
    
    List<LoanAccount> findByStartDateBetweenAndLoanStatus(LocalDate start, LocalDate end, LoanStatus loanStatus);
    
    // Get all active loans to generate monthly EMI collection lists
    List<LoanAccount> findAllByLoanStatus(LoanStatus loanStatus);
    
    List<LoanAccount> findByFinancialMonth(FinancialMonth month);
    
    Optional<LoanAccount> findByLoanApplication(LoanApplication application);
    
    List<LoanAccount> findByMemberMemberNo(int memberNo);
    
    List<LoanAccount> findByMember(Member member);
    
    @Query("SELECT COALESCE(SUM(l.grantedAmount), 0.0) FROM LoanAccount l " +
            "WHERE l.financialMonth.id = :monthId AND l.loanStatus != 'REJECTED'")
    Double sumOfLoanDisbursedAmount(@Param("monthId") Long monthId);
    
    @Query("SELECT COALESCE(SUM(l.grantedAmount), 0.0) FROM LoanAccount l WHERE l.financialMonth.startDate < :startDate")
    Double sumOfAllDisbursementsBeforeMonth(@Param("startDate") LocalDate startDate);
    
    @Query("SELECT COALESCE(SUM(CASE " +
	       "    WHEN la.pendingAmount < la.emiAmount THEN la.pendingAmount " +
	       "    ELSE la.emiAmount " +
	       "END), 0) " +
	       "FROM LoanAccount la " +
	       "WHERE la.loanStatus = 'ACTIVE' " +
	       "AND la.startDate < :currentMonthStart " +
	       "AND la.pendingAmount > 0")
	Double sumExpectedEmi(@Param("currentMonthStart") LocalDate currentMonthStart);
    
    // Count of loans granted in the last month (Fresh Loans)
    @Query("SELECT COUNT(la) FROM LoanAccount la " +
           "WHERE la.loanStatus = 'ACTIVE' AND la.emiAmount is null ")
    Long countLastMonthFreshLoans();
    
 // In LoanAccountRepository
    @Query("SELECT COUNT(l) FROM LoanAccount l WHERE l.loanStatus = 'ACTIVE'")
    long countActiveLoans();

    @Query("SELECT l.emiAmount, COUNT(l) FROM LoanAccount l WHERE l.loanStatus = 'ACTIVE' GROUP BY l.emiAmount")
    List<Object[]> getEmiWiseCounts();

    @Query("SELECT FUNCTION('MONTHNAME', l.startDate), COUNT(l) FROM LoanAccount l WHERE l.startDate > :cutoff GROUP BY FUNCTION('MONTHNAME', l.startDate)")
    List<Object[]> getLoansGrantedByMonth(@Param("cutoff") LocalDate cutoff);
    
    @Query("SELECT SUM(l.pendingAmount) FROM LoanAccount l WHERE l.loanStatus = 'ACTIVE'")
    Double sumTotalPending();
    
    @Query("""
	SELECT
	   CASE
	       WHEN la.pendingAmount < la.emiAmount THEN la.pendingAmount
	       ELSE la.emiAmount
	   END,
	   COUNT(la.id)
	FROM LoanAccount la
	WHERE la.loanStatus = 'ACTIVE'
	  AND la.startDate < :currentMonthStart
	  AND la.pendingAmount > 0
	GROUP BY
	   CASE
	       WHEN la.pendingAmount < la.emiAmount THEN la.pendingAmount
	       ELSE la.emiAmount
	   END
	""")
	List<Object[]> findRunningLoanEmiCounts(@Param("currentMonthStart") LocalDate currentMonthStart);

}