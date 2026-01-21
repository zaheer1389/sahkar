package com.badargadh.sahkar;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.badargadh.sahkar.data.AppUser;
import com.badargadh.sahkar.data.Member;
import com.badargadh.sahkar.enums.MemberStatus;
import com.badargadh.sahkar.enums.Role;
import com.badargadh.sahkar.repository.MemberRepository;
import com.badargadh.sahkar.repository.UserRepository;
import com.badargadh.sahkar.service.DataImportService;
import com.badargadh.sahkar.service.LoanWitnessRemarkImportService;
import com.badargadh.sahkar.service.UserService;

import jakarta.transaction.Transactional;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    @Autowired private UserRepository userRepo;
    @Autowired private MemberRepository memberRepo;
    @Autowired private DataImportService importService;
    @Autowired private UserService userService;
    @Autowired private LoanWitnessRemarkImportService importService2;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // 1. Create Initial Admin if none exists
        /*AppUser adminUser = userRepo.findByUsername("admin").orElseGet(() -> {
            // Create the login credentials
        	return userService.createInitialAdmin();
        });

        // 2. Run Data Import using this Admin as the processor
        if (memberRepo.count() <= 1) { // Only 'System Admin' exists
            System.out.println(">>> Initializing Database from CSV...");
            importService.importMembers("D:\\sahkar\\member_summary_2.csv", adminUser.getMember());
            System.out.println(">>> Database Initialization Complete.");
        }*/
    	
    	//importService2.processGujMemberNameExcel(new File("D:\\data\\sahkar\\Sahkar_Member_Witness_Remarks_With_Guj_Name.xlsx"));
    	//importService2.processGujMemberNameExcel2(new File("D:\\data\\sahkar\\Sahkar_Member_Witness_Remarks_Cancelled.xlsx"));
    }
}