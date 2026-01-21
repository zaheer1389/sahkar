package com.badargadh.sahkar.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.badargadh.sahkar.data.FinancialMonth;
import com.badargadh.sahkar.data.MonthlyFees;

@Repository
public interface MonthlyFeesRepository extends JpaRepository<MonthlyFees, Long>{
	
	Optional<MonthlyFees> findByFinancialMonth(FinancialMonth financialMonth);
	
	@Query("SELECT sum(e.amount) from MonthlyFees e")
	Long sumOfMonthlyFeesAmount();
	
	@Query(value = "SELECT sum(e.amount) from MonthlyFees e \n"
			+ "JOIN FinancialMonth b \n"
			+ "ON e.FinancialMonthId = b.FinancialMonthId \n"
			+ "where b.StartDate < :date ", nativeQuery = true)
	Long sumOfMonthlyFeesAmountUptoMonth(LocalDate date);
}
