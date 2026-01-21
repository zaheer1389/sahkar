package com.badargadh.sahkar.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.badargadh.sahkar.data.EmiPaymentGroup;

@Repository
public interface EmiPaymentGroupRepository extends JpaRepository<EmiPaymentGroup, Long> {
	
	@Query("SELECT g FROM EmiPaymentGroup g " +
	       "LEFT JOIN FETCH g.emiPayments " +
	       "LEFT JOIN FETCH g.feePayments " +
	       "WHERE g.id = :id")
	Optional<EmiPaymentGroup> findByIdWithPayments(@Param("id") Long id);
	
}