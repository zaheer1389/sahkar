package com.badargadh.sahkar.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.badargadh.sahkar.data.FinancialMonth;
import com.badargadh.sahkar.data.LoanApplication;
import com.badargadh.sahkar.data.Member;
import com.badargadh.sahkar.enums.LoanApplicationStatus;

@Repository
public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {
	
	@Query("SELECT la FROM LoanApplication la " +
           "JOIN LoanAccount acc ON acc.loanApplication = la " +
           "WHERE acc.loanStatus = 'ACTIVE' ")
    List<LoanApplication> findAllActiveLoanApplications();
	
	@Query("SELECT la FROM LoanApplication la " +
           "JOIN LoanAccount acc ON acc.loanApplication = la " +
           "WHERE acc.loanStatus = 'ACTIVE' " +
           "AND la.witnesses IS EMPTY")
    List<LoanApplication> findActiveLoansWithoutWitness();
	
    // Find applications waiting for the monthly draw
    List<LoanApplication> findAllByStatus(LoanApplicationStatus status);
    
    // Find all applications submitted by a specific member
    List<LoanApplication> findAllByMember(Member member);
    
    List<LoanApplication> findAllByOrderByApplicationDateTimeDesc();
    
    List<LoanApplication> findAllByStatusOrderByApplicationDateTimeAsc(LoanApplicationStatus status);
    
    List<LoanApplication> findAllByFinancialMonthAndStatus(FinancialMonth month, LoanApplicationStatus status);
    
    List<LoanApplication> findAllByFinancialMonthAndStatusInOrderByStatusAsc(FinancialMonth month, Collection<LoanApplicationStatus> statuses);
    
    Optional<LoanApplication> findFirstByFinancialMonthAndDrawRankStartingWithOrderByDrawRankAsc(FinancialMonth month, String drawRank);
    
    int countByFinancialMonthAndDrawRankStartingWith(FinancialMonth month, String drawRank);
    
    boolean existsByFinancialMonthAndDrawRankIsNotNull(FinancialMonth financialMonth);
    
    // Find the latest application date in the system
    @Query("SELECT MAX(l.applicationDateTime) FROM LoanApplication l")
    LocalDateTime findMaxApplicationDateTime();
    
    Optional<LoanApplication> findByMemberAndStatus(Member member, LoanApplicationStatus status);
    
    Optional<LoanApplication> findByMemberAndFinancialMonth(Member member, FinancialMonth financialMonth);
    
    @Query("SELECT l FROM LoanApplication l WHERE l.applicationDateTime >= :cutoff ORDER BY l.applicationDateTime DESC")
    List<LoanApplication> findRecentApplications(@Param("cutoff") LocalDateTime cutoff);
    
    @Query("SELECT a FROM LoanApplication a " +
           "LEFT JOIN FETCH a.witnesses w " +
           "LEFT JOIN FETCH w.witnessMember " + // Fetch the Member object inside the witness too!
           "WHERE a.id = :id")
    Optional<LoanApplication> findByIdWithWitnesses(@Param("id") Long id);
    
    @Modifying
    @Query("UPDATE LoanApplication l SET l.status = :status " +
           "WHERE l.member.memberNo IN :memberNos " +
           "AND l.financialMonth = :month " +
           "AND l.status in ('APPLIED', 'WAITING') ")
    int updateStatusForSelectedMembers(@Param("memberNos") List<Integer> memberNos, 
                                       @Param("month") FinancialMonth month, 
                                       @Param("status") LoanApplicationStatus status);

    @Modifying
    @Query("UPDATE LoanApplication l SET l.status = :status " +
           "WHERE l.member.memberNo NOT IN :memberNos " +
           "AND l.financialMonth = :month " +
           "AND l.status in ('APPLIED', 'WAITING') ")
    int updateRemainingToWaiting(@Param("memberNos") List<Integer> memberNos, 
                                 @Param("month") FinancialMonth month, 
                                 @Param("status") LoanApplicationStatus status);
    
}