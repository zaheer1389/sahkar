package com.badargadh.sahkar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.badargadh.sahkar.data.AppConfig;

@Repository
public interface AppConfigRepository extends JpaRepository<AppConfig, Long> {
}