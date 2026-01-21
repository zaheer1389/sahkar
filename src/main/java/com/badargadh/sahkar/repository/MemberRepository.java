package com.badargadh.sahkar.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.badargadh.sahkar.data.FinancialMonth;
import com.badargadh.sahkar.data.Member;
import com.badargadh.sahkar.dto.MemberSummaryDTO;
import com.badargadh.sahkar.dto.MonthlyPaymentCollectionDTO;
import com.badargadh.sahkar.dto.PendingMonthlyCollectionDTO;
import com.badargadh.sahkar.enums.MemberStatus;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
	// Used to check if a Member Number is currently occupied
	boolean existsByMemberNo(Integer memberNo);

	@Query("SELECT m FROM Member m WHERE " +
	       "LOWER(m.firstName) LIKE LOWER(CONCAT('%', :text, '%')) OR " +
	       "LOWER(m.middleName) LIKE LOWER(CONCAT('%', :text, '%')) OR " +
	       "LOWER(m.lastName) LIKE LOWER(CONCAT('%', :text, '%')) OR " +
	       "LOWER(CONCAT(m.firstName, ' ', m.lastName)) LIKE LOWER(CONCAT('%', :text, '%'))")
	List<Member> searchByName(@Param("text") String text);
	
	// Find member by their assigned number
	Optional<Member> findByMemberNo(Integer memberNo);

	// Find all members with a specific status (e.g., ACTIVE)
	List<Member> findAllByStatus(MemberStatus status);
	
	List<Member> findAllByFinancialMonth(FinancialMonth month);
	
	@Query("SELECT m FROM Member m WHERE m.cancellationDateTime between :start and :end")
	List<Member> findCancelledMemberBtweenDates(LocalDateTime start, LocalDateTime end);
	
	int countByStatus(MemberStatus status);

	@Query("SELECT m FROM Member m WHERE m.status IN ('ACTIVE','CANCELLED')")
	List<Member> findAllActiveMembers();

	@Query("SELECT new com.badargadh.sahkar.dto.MemberSummaryDTO("
			+ "m.memberNo, "
			+ "m.firstNameGuj, "
			+ "m.middleNameGuj, "
			+ "m.lastNameGuj, "
			+ "m.village, "
			+ "m.status, "
			+ "(SELECT COALESCE(SUM(f.amount), 0) FROM FeePayment f WHERE f.member = m), "
			+ "(SELECT COALESCE(la.pendingAmount, 0) FROM LoanAccount la WHERE la.member = m AND la.loanStatus = 'ACTIVE'), "
			+ "(SELECT COALESCE(la.emiAmount, 0) FROM LoanAccount la WHERE la.member = m AND la.loanStatus = 'ACTIVE'),"
			+ "m.cancellationDateTime"
			+ ") "
			+ "FROM Member m "
			+ "WHERE m.status in ('ACTIVE','CANCELLED')"
			+ "ORDER BY m.memberNo asc ")
	List<MemberSummaryDTO> findActiveMembersGuj();
	
	@Query("SELECT new com.badargadh.sahkar.dto.MemberSummaryDTO("
			+ "m.memberNo, "
			+ "m.firstName, "
			+ "m.middleName, "
			+ "m.lastName, "
			+ "m.village, "
			+ "m.status, "
			+ "(SELECT COALESCE(SUM(f.amount), 0) FROM FeePayment f WHERE f.member = m), "
			+ "(SELECT COALESCE(la.pendingAmount, 0) FROM LoanAccount la WHERE la.member = m AND la.loanStatus = 'ACTIVE'), "
			+ "(SELECT COALESCE(la.emiAmount, 0) FROM LoanAccount la WHERE la.member = m AND la.loanStatus = 'ACTIVE'),"
			+ "m.cancellationDateTime"
			+ ") "
			+ "FROM Member m "
			+ "WHERE m.status in ('ACTIVE','CANCELLED')"
			+ "ORDER BY m.memberNo asc ")
	List<MemberSummaryDTO> findActiveMembers();

	@Query("SELECT new com.badargadh.sahkar.dto.MemberSummaryDTO("
	        + "m.memberNo, m.firstName, m.middleName, m.lastName, m.village, m.status, "
	        + "(SELECT COALESCE(SUM(f.amount), 0) FROM FeePayment f WHERE f.member = m), " 
	        + "la.pendingAmount, "
	        + "la.emiAmount,"
	        + "CONCAT(COALESCE(m.firstNameGuj, ''), ' ', COALESCE(m.middleNameGuj, ''), ' ', COALESCE(m.lastNameGuj, '')),"
	        + "branchNameGuj) " 
	        + "FROM Member m "
	        + "LEFT JOIN LoanAccount la ON la.member = m AND la.loanStatus = 'ACTIVE' "
	        // Join with FinancialMonth to get the month's date boundaries
	        + "JOIN FinancialMonth fm ON fm.id = :monthId "
	        
	        + "WHERE (m.status IS NULL OR m.status = com.badargadh.sahkar.enums.MemberStatus.ACTIVE) "
	        
	        // --- NEW FILTER: Joining Date Check ---
	        // Only show members who joined on or before the current financial month's end date
	        + "AND (m.joiningDateTime IS NULL OR m.joiningDateTime <= fm.endDate) "
	        
	        // 1. Exclude if Monthly Fee is already paid
	        + "AND NOT EXISTS ( " + "  SELECT f FROM FeePayment f " + "  WHERE f.member = m "
	        + "  AND f.financialMonth.id = :monthId "
	        + "  AND f.feeType = com.badargadh.sahkar.enums.FeeType.MONTHLY_FEE" + ") " 
	        
	        // 2. Exclude if EMI is already paid
	        + "AND NOT EXISTS ( " + "  SELECT e FROM EmiPayment e " + "  WHERE e.loanAccount.member = m "
	        + "  AND e.financialMonth.id = :monthId" + ") "
	        
	        // 3. Exclude if Joining Fee is already paid
	        + "AND NOT EXISTS ( " + "  SELECT f2 FROM FeePayment f2 " + "  WHERE f2.member = m "
	        + "  AND f2.financialMonth.id = :monthId "
	        + "  AND f2.feeType = com.badargadh.sahkar.enums.FeeType.JOINING_FEE" + ")")
	List<MemberSummaryDTO> findActiveMembersPendingForMonth(@Param("monthId") Long monthId);
	
	@Query("SELECT new com.badargadh.sahkar.dto.PendingMonthlyCollectionDTO("
	        + "m.memberNo, m.firstName, m.middleName, m.lastName, m.village, m.status, "
	        + "(SELECT COALESCE(SUM(f.amount), 0.0) FROM FeePayment f WHERE f.member = m), " 
	        + "COALESCE(la.pendingAmount, 0.0), "
	        + "COALESCE(la.emiAmount, 0.0), "
	        + "CONCAT(COALESCE(m.firstNameGuj, ''), ' ', COALESCE(m.middleNameGuj, ''), ' ', COALESCE(m.lastNameGuj, '')), "
	        + "m.branchNameGuj) " 
	        + "FROM Member m "
	        + "LEFT JOIN LoanAccount la ON la.member = m AND la.loanStatus = 'ACTIVE' "
	        + "JOIN FinancialMonth fm ON fm.id = :monthId "
	        + "WHERE (m.status IS NULL OR m.status = com.badargadh.sahkar.enums.MemberStatus.ACTIVE) "
	        
	        // Joining Date Check
	        + "AND (m.joiningDateTime IS NULL OR m.joiningDateTime <= fm.endDate) "
	        
	        // Exclusion logic: Only show members who haven't paid yet for this month
	        + "AND NOT EXISTS (SELECT 1 FROM FeePayment f WHERE f.member = m "
	        + "  AND f.financialMonth.id = :monthId "
	        + "  AND f.feeType = com.badargadh.sahkar.enums.FeeType.MONTHLY_FEE) " 
	        
	        + "AND NOT EXISTS (SELECT 1 FROM EmiPayment e WHERE e.loanAccount.member = m "
	        + "  AND e.financialMonth.id = :monthId) "
	        
	        + "AND NOT EXISTS (SELECT 1 FROM FeePayment f2 WHERE f2.member = m "
	        + "  AND f2.financialMonth.id = :monthId "
	        + "  AND f2.feeType = com.badargadh.sahkar.enums.FeeType.JOINING_FEE) "
	        + "ORDER BY m.memberNo ASC")
	List<PendingMonthlyCollectionDTO> findActiveMembersPendingForMonth1(@Param("monthId") Long monthId);
	
	@Query("SELECT new com.badargadh.sahkar.dto.MonthlyPaymentCollectionDTO("
	        + "m.memberNo, m.firstName, m.middleName, m.lastName, m.village, m.status, "
	        + "(SELECT COALESCE(SUM(f.amount), 0.0) FROM FeePayment f WHERE f.member = m AND f.financialMonth.id = :monthId AND f.feeType = com.badargadh.sahkar.enums.FeeType.MONTHLY_FEE), "
	        + "(SELECT COALESCE(la.pendingAmount, 0.0) FROM LoanAccount la WHERE la.member = m AND la.loanStatus = 'ACTIVE'), "
	        + "(SELECT COALESCE(SUM(e.amountPaid), 0.0) FROM EmiPayment e WHERE e.member = m AND e.financialMonth.id = :monthId), "
	        + "(SELECT COALESCE(SUM(f.amount), 0.0) FROM FeePayment f WHERE f.member = m AND f.financialMonth.id = :monthId AND f.feeType = com.badargadh.sahkar.enums.FeeType.JOINING_FEE), "
	        
	        // Date Logic
	        + "(SELECT MAX(CASE WHEN COALESCE(f2.transactionDateTime, '1900-01-01') > COALESCE(e2.paymentDateTime, '1900-01-01') "
	        + "            THEN f2.transactionDateTime ELSE e2.paymentDateTime END) "
	        + " FROM Member m2 LEFT JOIN FeePayment f2 ON f2.member = m2 AND f2.financialMonth.id = :monthId "
	        + " LEFT JOIN EmiPayment e2 ON e2.member = m2 AND e2.financialMonth.id = :monthId WHERE m2 = m), "

	        // Location Logic
	        + "(SELECT COALESCE("
	        + "  (SELECT MAX(f3.collectionLocation) FROM FeePayment f3 WHERE f3.member = m AND f3.financialMonth.id = :monthId), "
	        + "  (SELECT MAX(e3.collectionLocation) FROM EmiPayment e3 WHERE e3.member = m AND e3.financialMonth.id = :monthId)"
	        + ") FROM Member m4 WHERE m4 = m), "

	        // NEW: Payment Group ID Logic
	        // Fetches the Group ID from either Fee or EMI table
	        + "(SELECT COALESCE("
	        + "  (SELECT MAX(f5.paymentGroup.id) FROM FeePayment f5 WHERE f5.member = m AND f5.financialMonth.id = :monthId), "
	        + "  (SELECT MAX(e5.paymentGroup.id) FROM EmiPayment e5 WHERE e5.member = m AND e5.financialMonth.id = :monthId)"
	        + ") FROM Member m6 WHERE m6 = m) "

	        + ") FROM Member m "
	        + "WHERE m.memberNo IS NOT NULL AND ("
	        + "  EXISTS (SELECT 1 FROM FeePayment f WHERE f.member = m AND f.financialMonth.id = :monthId) "
	        + "  OR EXISTS (SELECT 1 FROM EmiPayment e WHERE e.member = m AND e.financialMonth.id = :monthId)"
	        + ") "
	        + "ORDER BY m.memberNo ASC")
	List<MonthlyPaymentCollectionDTO> findReceivedPaymentsByMonthWithGroup(@Param("monthId") Long monthId);

	@Query("SELECT new com.badargadh.sahkar.dto.MemberSummaryDTO("
		    + "m.memberNo, m.firstName, m.middleName, m.lastName, m.village, m.status, "
		    + "(SELECT COALESCE(SUM(f.amount), 0) FROM FeePayment f WHERE f.member = m AND f.financialMonth.id = :monthId AND f.feeType = com.badargadh.sahkar.enums.FeeType.MONTHLY_FEE), "
		    + "(SELECT COALESCE(la.pendingAmount, 0) FROM LoanAccount la WHERE la.member = m AND la.loanStatus = 'ACTIVE'), "
		    + "(SELECT COALESCE(SUM(e.amountPaid), 0) FROM EmiPayment e WHERE e.member = m AND e.financialMonth.id = :monthId), "
		    + "(SELECT COALESCE(SUM(f.amount), 0) FROM FeePayment f WHERE f.member = m AND f.financialMonth.id = :monthId AND f.feeType = com.badargadh.sahkar.enums.FeeType.JOINING_FEE) ,"
		    // New logic to find the latest transaction date
		    + "(SELECT MAX(COALESCE(f.transactionDateTime, e.paymentDateTime)) FROM Member m2 "
		    + " LEFT JOIN FeePayment f ON f.member = m2 AND f.financialMonth.id = :monthId "
		    + " LEFT JOIN EmiPayment e ON e.member = m2 AND e.financialMonth.id = :monthId "
		    + " WHERE m2 = m) ) "
		    + "FROM Member m "
		    + "WHERE m.memberNo is not null AND EXISTS (SELECT 1 FROM FeePayment f WHERE f.member = m AND f.financialMonth.id = :monthId) "
		    + "OR EXISTS (SELECT 1 FROM EmiPayment e WHERE e.member = m AND e.financialMonth.id = :monthId)")
		List<MemberSummaryDTO> findReceivedPaymentsByMonth(@Param("monthId") Long monthId);
	
	@Query("SELECT new com.badargadh.sahkar.dto.MonthlyPaymentCollectionDTO("
	        + "m.memberNo, m.firstName, m.middleName, m.lastName, m.village, m.status, "
	        + "(SELECT COALESCE(SUM(f.amount), 0.0) FROM FeePayment f WHERE f.member = m AND f.financialMonth.id = :monthId AND f.feeType = com.badargadh.sahkar.enums.FeeType.MONTHLY_FEE), "
	        + "(SELECT COALESCE(la.pendingAmount, 0.0) FROM LoanAccount la WHERE la.member = m AND la.loanStatus = 'ACTIVE'), "
	        + "(SELECT COALESCE(SUM(e.amountPaid), 0.0) FROM EmiPayment e WHERE e.member = m AND e.financialMonth.id = :monthId), "
	        + "(SELECT COALESCE(SUM(f.amount), 0.0) FROM FeePayment f WHERE f.member = m AND f.financialMonth.id = :monthId AND f.feeType = com.badargadh.sahkar.enums.FeeType.JOINING_FEE), "
	        
	        // Latest Transaction Date Logic
	        + "(SELECT MAX(CASE WHEN COALESCE(f2.transactionDateTime, '1900-01-01') > COALESCE(e2.paymentDateTime, '1900-01-01') "
	        + "            THEN f2.transactionDateTime ELSE e2.paymentDateTime END) "
	        + " FROM Member m2 LEFT JOIN FeePayment f2 ON f2.member = m2 AND f2.financialMonth.id = :monthId "
	        + " LEFT JOIN EmiPayment e2 ON e2.member = m2 AND e2.financialMonth.id = :monthId WHERE m2 = m), "

	        // FIXED: Collection Location Logic
	        // Using MAX() instead of FETCH FIRST to grab a single non-null location
	        + "(SELECT COALESCE("
	        + "  (SELECT MAX(f3.collectionLocation) FROM FeePayment f3 WHERE f3.member = m AND f3.financialMonth.id = :monthId AND f3.collectionLocation IS NOT NULL), "
	        + "  (SELECT MAX(e3.collectionLocation) FROM EmiPayment e3 WHERE e3.member = m AND e3.financialMonth.id = :monthId AND e3.collectionLocation IS NOT NULL)"
	        + ") FROM Member m4 WHERE m4 = m) "

	        + ") FROM Member m "
	        + "WHERE m.memberNo IS NOT NULL AND ("
	        + "  EXISTS (SELECT 1 FROM FeePayment f WHERE f.member = m AND f.financialMonth.id = :monthId) "
	        + "  OR EXISTS (SELECT 1 FROM EmiPayment e WHERE e.member = m AND e.financialMonth.id = :monthId)"
	        + ") "
	        + "ORDER BY m.memberNo ASC")
	List<MonthlyPaymentCollectionDTO> findReceivedPaymentsByMonth1(@Param("monthId") Long monthId);

	@Query(value = "SELECT m.* FROM members m " +
	       "CROSS JOIN app_settings c " +
	       "WHERE m.member_status = 'CANCELLED' " +
	       "AND (" +
	       "   m.cancellation_reason = 'MEMBER_EXPIRED' " + 
	       "   OR (" +
	       "       CASE " +
	       "           WHEN m.cancellation_date_time < '2026-01-01' " +
	       "           THEN DATE_ADD(m.cancellation_date_time, INTERVAL 3 MONTH) " +
	       "           ELSE DATE_ADD(m.cancellation_date_time, INTERVAL c.fees_refund_cooling_period MONTH) " +
	       "       END < :endDate" +
	       "   )" +
	       ") ORDER BY m.cancellation_date_time ASC", 
	       nativeQuery = true)
	List<Member> findMembersEligibleForRefund(@Param("endDate") LocalDate endDate);
	
	@Query(value = "SELECT m.*, COALESCE((SELECT MAX(la.end_date) FROM loan_accounts la WHERE la.member_id = m.id), '1900-01-01') as last_loan_end, "
			+ "TIMESTAMPDIFF(MONTH, COALESCE((SELECT MAX(la.end_date) FROM loan_accounts la WHERE la.member_id = m.id), '1900-01-01'), :currentMonthStart) as waiting_duration "
			+ "FROM members m "
			+ "WHERE m.member_status = 'ACTIVE'  "
			+ "AND m.id IN (:applicantIds)  "
			+ "ORDER BY waiting_duration DESC, m.member_no ASC", 
		       nativeQuery = true)
	List<Member> findDrawPriorityList(@Param("applicantIds") List<Long> applicantIds, 
	                                  @Param("currentMonthStart") java.time.LocalDate currentMonthStart);
	
	@Query(value = "SELECT m.* FROM members m " +
		       "WHERE m.member_status = 'ACTIVE' " +
		       "AND m.id NOT IN (SELECT la.member_id FROM loan_accounts la WHERE la.loan_status = 'ACTIVE') " +
		       "LIMIT :limit", nativeQuery = true)
	List<Member> findEligibleMembersForTest(@Param("limit") int limit);
	
}