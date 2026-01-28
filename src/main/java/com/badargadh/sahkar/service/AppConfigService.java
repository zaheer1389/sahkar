package com.badargadh.sahkar.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.badargadh.sahkar.data.AppConfig;
import com.badargadh.sahkar.data.EmiConfig;
import com.badargadh.sahkar.repository.AppConfigRepository;
import com.badargadh.sahkar.repository.EmiConfigRepository;

import jakarta.annotation.PostConstruct;

@Service
public class AppConfigService {

    @Autowired
    private AppConfigRepository configRepo;
    
    @Autowired
    private EmiConfigRepository emiConfigRepo;

    @Value("${openingBal}")
    private Double openingBal;
    
    @PostConstruct
    private void init() {
    	if(emiConfigRepo.findAll().size() == 0) {
    		saveEmiConfig(100d);
    		saveEmiConfig(200d);
    		saveEmiConfig(300d);
    		saveEmiConfig(400d);
    	}
    }
    
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
    
    public List<EmiConfig> getAllEmiConfigs() {
        return emiConfigRepo.findAllByOrderByAmountAsc();
    }

    @Transactional
    public void saveEmiConfig(Double amount) {
        // 1. Check if positive
        if (amount <= 0) {
            throw new IllegalArgumentException("EMI amount must be greater than zero.");
        }
        
        // 2. Check for Multiple of 100
        // Using % (modulo) operator: if amount % 100 is not 0, it's not a multiple
        if (amount % 100 != 0) {
            throw new IllegalArgumentException("EMI amount must be in multiples of 100 (e.g., 100, 200, 500).");
        }

        // 3. Check for duplicates
        if (emiConfigRepo.existsByAmount(amount)) {
            throw new IllegalArgumentException("This EMI amount already exists in the configuration.");
        }

        emiConfigRepo.save(new EmiConfig(amount));
    }

    @Transactional
    public void toggleEmiStatus(Long id) {
        emiConfigRepo.findById(id).ifPresent(config -> {
            config.setActive(!config.isActive());
            emiConfigRepo.save(config);
        });
    }
    
    public EmiConfig findByEmiAmount(Double amount) {
    	return emiConfigRepo.findByAmount(amount);
    }
}