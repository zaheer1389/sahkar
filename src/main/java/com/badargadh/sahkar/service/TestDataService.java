package com.badargadh.sahkar.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.badargadh.sahkar.data.FinancialMonth;
import com.badargadh.sahkar.data.LoanApplication;
import com.badargadh.sahkar.data.Member;
import com.badargadh.sahkar.enums.LoanApplicationStatus;
import com.badargadh.sahkar.enums.MonthStatus;
import com.badargadh.sahkar.repository.FinancialMonthRepository;
import com.badargadh.sahkar.repository.LoanApplicationRepository;
import com.badargadh.sahkar.repository.MemberRepository;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class TestDataService {

	@Autowired private MemberRepository memberRepo;
    @Autowired private LoanApplicationRepository loanAppRepo;
    @Autowired private FinancialMonthRepository monthRepo;
    @Autowired private AppConfigService appConfigService;

    public void generateDummyApplications() {
    	
        // 1. Get the month for the test
        monthRepo.findByMonthNameAndYear("JANUARY", 2026).ifPresent(month -> {
        	// 2. Find 50 members with no current ACTIVE loan
            List<Member> eligibleMembers = memberRepo.findEligibleMembersForTest(25);

            if (eligibleMembers.isEmpty()) {
                System.out.println("No eligible members found to create test applications.");
                return;
            }

            // 3. Create LoanApplication records
            List<LoanApplication> applications = eligibleMembers.stream().map(member -> {
                LoanApplication app = new LoanApplication();
                app.setMember(member);
                app.setFinancialMonth(month);
                app.setStatus(LoanApplicationStatus.APPLIED);
                app.setApplicationDateTime(LocalDateTime.now());
                app.setAppliedAmount(appConfigService.getSettings().getLoanAmount().doubleValue());
                //app.setRemarks("SYSTEM_TEST_DATA");
                return app;
            }).collect(Collectors.toList());

            loanAppRepo.saveAll(applications);
            System.out.println("Successfully inserted " + applications.size() + " dummy applications.");
        });
        
    }
    
    private FinancialMonth getFinancialMonth(String month, int year) {
    	FinancialMonth financialMonth = new FinancialMonth();
    	financialMonth.setMonthName(month);
    	financialMonth.setYear(year);
    	financialMonth.setMonthId(month.toUpperCase() + "-" + year);
    	financialMonth.setStatus(MonthStatus.OPEN);
    	financialMonth.setStartDate(LocalDate.of(year, Month.valueOf(month), 1));
    	financialMonth.setEndDate(YearMonth.of(year, Month.valueOf(month)).atEndOfMonth());
    	return monthRepo.save(financialMonth);
    }
}