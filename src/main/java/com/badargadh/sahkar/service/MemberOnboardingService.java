package com.badargadh.sahkar.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.badargadh.sahkar.data.AppConfig;
import com.badargadh.sahkar.data.FinancialMonth;
import com.badargadh.sahkar.data.Member;
import com.badargadh.sahkar.data.MemberHistory;
import com.badargadh.sahkar.enums.MemberOnboardingType;
import com.badargadh.sahkar.exception.BusinessException;
import com.badargadh.sahkar.repository.MemberHistoryRepository;

@Service
public class MemberOnboardingService {

    @Autowired private MemberService memberService;
    @Autowired private FinancialMonthService monthService;
    @Autowired private FeeService feeService;
    @Autowired private AppConfigService configService;
    @Autowired private MemberHistoryRepository historyRepository;

    public MemberOnboardingService() {
    	
    }

    @Transactional
    public Member registerNewMember(Member member, MemberOnboardingType type) {
        // 1. Check if Financial Month is Open
        if (!monthService.getActiveMonth().isPresent()) {
            throw new BusinessException("Cannot add member: No financial month is currently OPEN.");
        }
        
        FinancialMonth month = monthService.getActiveMonth().get();
        LocalDate monthBusinessDate = LocalDate.of(month.getYear(), Month.valueOf(month.getMonthName()), 10);
        
        LocalDateTime joiningDateTime = LocalDateTime.of(monthBusinessDate, LocalTime.of(13, 30));
        
        if(!monthService.isValidTransactionDate(joiningDateTime)) {
        	throw new BusinessException("System Date Error!\n" +
                "The PC date (" + joiningDateTime + ") is outside the current OPEN period ");
        }

        // 2. Check for Duplicate Mobile Number
        if (memberService.existsByMemberNo(member.getMemberNo())) {
            throw new BusinessException("A member with member number "+member.getMemberNo()+" already exists.");
        }

        // 3. Create the Member
        member.setJoiningDateTime(joiningDateTime);
        Member savedMember = memberService.save(member);

        // 4. Handle Fees
        AppConfig config = configService.getSettings();
        
        
        if(type == MemberOnboardingType.NEW) {
        	// Add One-time Joining Fee
            feeService.recordJoiningFee(savedMember, config.getNewMemberFees(), joiningDateTime, month, null, null, null);
        }
        else if(type == MemberOnboardingType.REJOIN) {
        	// Add One-time ReJoining Fee
        	Long totalFees = feeService.getTotalMonthlyFeesDepositedInSociety();
            feeService.recordJoiningFee(savedMember, totalFees, joiningDateTime, month, null, null, null);
        }
        
        // Add Initial Monthly Subscription Fee
        //feeService.recordMonthlyFee(savedMember, config.getMonthlyFees(), joiningDateTime, month, null);
        
        return savedMember;
    }
    
    public boolean isMemberWasPastMember(int memberNo) {
    	List<MemberHistory> memberHistories = historyRepository.findAllByMemberNo(memberNo);
    	return memberHistories != null && memberHistories.size() > 0; 
    }
}