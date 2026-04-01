package com.badargadh.sahkar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.badargadh.sahkar.data.AuthorizationProof;

@Repository
public interface AuthorizationProofRepository extends JpaRepository<AuthorizationProof, Long> {
    
    
}