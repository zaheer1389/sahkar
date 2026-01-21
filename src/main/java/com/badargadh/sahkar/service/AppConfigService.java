package com.badargadh.sahkar.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.badargadh.sahkar.data.AppConfig;
import com.badargadh.sahkar.repository.AppConfigRepository;

@Service
public class AppConfigService {

    @Autowired
    private AppConfigRepository configRepo;

    @Value("${openingBal}")
    private Double openingBal;
    
    /**
     * Retrieves the global settings. 
     * Creates default settings if none exist.
     */
    public AppConfig getSettings() {
        return configRepo.findById(1L).orElseGet(() -> {
            AppConfig defaults = new AppConfig();
            defaults.setMonthlyFees(20L);
            defaults.setNewMemberFees(300L);
            defaults.setLoanAmount(10000L);
            defaults.setNewMemberCoolingPeriod(30);
            defaults.setFeesRefundCoolingPeriod(3);
            defaults.setOpeningBal(openingBal);
            defaults.setRemarkDateOfMonth(11);
            defaults.setRemarkTimeHour(17);
            defaults.setRemarkTimeMinute(0);
            defaults.setAutoRemark(true);
            return configRepo.save(defaults);
        });
    }

    @Transactional
    public void saveSettings(AppConfig settings) {
        // Force the ID to 1 to ensure we always update the same record
        settings.setId(1L);
        configRepo.save(settings);
    }
}