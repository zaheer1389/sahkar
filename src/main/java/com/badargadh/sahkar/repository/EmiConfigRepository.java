package com.badargadh.sahkar.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.badargadh.sahkar.data.EmiConfig;

@Repository
public interface EmiConfigRepository extends JpaRepository<EmiConfig, Long> {
    
    // Used to populate ComboBoxes in the Loan Application screen
    List<EmiConfig> findAllByActiveTrueOrderByAmountAsc();
    
    // Used to refresh the Settings Table
    List<EmiConfig> findAllByOrderByAmountAsc();
    
    EmiConfig findByAmount(Double amount);
    
    boolean existsByAmount(Double amount);
}