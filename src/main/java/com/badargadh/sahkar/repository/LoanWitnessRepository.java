package com.badargadh.sahkar.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.badargadh.sahkar.data.LoanWitness;
import com.badargadh.sahkar.data.Member;

@Repository
public interface LoanWitnessRepository extends JpaRepository<LoanWitness, Long> {
    // Find all loans witnessed by a specific member (for eligibility checks)
    List<LoanWitness> findAllByWitnessMember(Member witnessMember);
}