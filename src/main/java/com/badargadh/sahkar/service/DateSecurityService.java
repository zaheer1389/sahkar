package com.badargadh.sahkar.service;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.badargadh.sahkar.data.FinancialMonth;
import com.badargadh.sahkar.enums.MonthStatus;
import com.badargadh.sahkar.exception.BusinessException;
import com.badargadh.sahkar.repository.FinancialMonthRepository;
import com.badargadh.sahkar.repository.LoanApplicationRepository;

@Service
public class DateSecurityService {

    @Autowired private FinancialMonthRepository finMonthRepo;

    public LocalDate getValidatedDate() {
        // 1. Get the official OPEN period from DB
        FinancialMonth activeMonth = finMonthRepo.findByStatus(MonthStatus.OPEN)
            .orElseThrow(() -> new BusinessException("No Financial Month is currently OPEN. Transactions blocked."));

        LocalDate pcDate = LocalDate.now();

        // 2. Guard: PC Date must be within the Open Month's Range
        if (pcDate.isBefore(activeMonth.getStartDate()) || pcDate.isAfter(activeMonth.getEndDate())) {
            throw new BusinessException("System Date Error!\n" +
                "The PC date (" + pcDate + ") is outside the current OPEN period: " +
                activeMonth.getMonthName() + " (" + activeMonth.getStartDate() + " to " + activeMonth.getEndDate() + ")");
        }

        return pcDate;
    }
}