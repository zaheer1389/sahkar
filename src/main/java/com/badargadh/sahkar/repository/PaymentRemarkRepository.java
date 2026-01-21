package com.badargadh.sahkar.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.badargadh.sahkar.data.FinancialMonth;
import com.badargadh.sahkar.data.Member;
import com.badargadh.sahkar.data.PaymentRemark;

@Repository
public interface PaymentRemarkRepository extends JpaRepository<PaymentRemark, Long> {
	Optional<PaymentRemark> findByMemberAndFinancialMonth(Member member, FinancialMonth month);

	List<PaymentRemark> findByMemberAndIsClearedFalseOrderByIssuedDateAsc(Member member);

	int countByMemberAndIsClearedFalse(Member member);
	
	List<PaymentRemark> getRemarksByMember(Member member);

}