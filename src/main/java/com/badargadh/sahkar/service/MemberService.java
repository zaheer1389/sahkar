package com.badargadh.sahkar.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.badargadh.sahkar.data.FinancialMonth;
import com.badargadh.sahkar.data.LoanAccount;
import com.badargadh.sahkar.data.Member;
import com.badargadh.sahkar.dto.MemberDTO;
import com.badargadh.sahkar.dto.MemberSummaryDTO;
import com.badargadh.sahkar.enums.CancellationReason;
import com.badargadh.sahkar.enums.MemberStatus;
import com.badargadh.sahkar.exception.BusinessException;
import com.badargadh.sahkar.repository.FeePaymentRepository;
import com.badargadh.sahkar.repository.MemberRepository;

@Service
public class MemberService {
    
    @Autowired private MemberRepository memberRepo;
    @Autowired private AppConfigService appConfigService;
    @Autowired private LoanService loanService;
    @Autowired private FeePaymentRepository feeRepo;
    @Autowired private FinancialMonthService monthService;

    public Optional<Member> findByMemberNo(Integer memberNo) {
    	return memberRepo.findByMemberNo(memberNo);
    }
    
    public List<Member> searchByName(String searchText) {
    	return memberRepo.searchByName(searchText);
    }

    public boolean existsByMemberNo(Integer memberNo) {
        return memberRepo.existsByMemberNo(memberNo);
    }

    public Member save(Member member) {
        return memberRepo.save(member);
    }
    
    public List<MemberSummaryDTO> findActiveMembers() {
    	return memberRepo.findActiveMembers();
    }
    
    public List<MemberDTO> findActiveMembersForReport() {
    	return memberRepo.findActiveMembersForReport();
    }
    
    public List<MemberSummaryDTO> findActiveMembersGujNames() {
    	return memberRepo.findActiveMembersGuj();
    }

	public Member findByMemberNumber(Integer memberNo) {
	    // Attempt to find the member, throw specific exception if not found
	    return memberRepo.findByMemberNo(memberNo)
	        .orElseThrow(() -> new BusinessException("Member Number [" + memberNo + "] does not exist in the system."));
	}
	
	public Long getMemberJoiningFeeDetails() {
		return appConfigService.getSettings().getNewMemberFees() + appConfigService.getSettings().getMonthlyFees();
	}
	
	public void cancelMembershipValidationChecks(Integer memberId) {
	    // 1. Fetch Member
	    Member member = memberRepo.findByMemberNo(memberId)
	            .orElseThrow(() -> new BusinessException("Member not found."));

	    // 2. Fetch Active Month
	    FinancialMonth activeMonth = monthService.getActiveMonth()
	            .orElseThrow(() -> new BusinessException("No active financial month found."));

	    // 3. CHECK: No active loan allowed
	    LoanAccount activeLoan = loanService.findMemberActiveLoan(member);
	    if (activeLoan != null && activeLoan.getPendingAmount() > 0) {
	        throw new BusinessException("Cannot cancel: Member has an active loan with balance ₹" + activeLoan.getPendingAmount());
	    }

	    // 4. CHECK: Current month fees must be paid
	    boolean feesPaid = feeRepo.existsByMemberAndFinancialMonth(member, activeMonth);
	    if (!feesPaid) {
	        throw new BusinessException("Cannot cancel: Monthly fees for " + activeMonth.getMonthName() + " must be paid first.");
	    }

	    // 5. CHECK: Date must be in active month boundaries
	    /*LocalDate now = LocalDate.now();
	    if (now.isBefore(activeMonth.getStartDate()) || 
	       (activeMonth.getEndDate() != null && now.isAfter(activeMonth.getEndDate()))) {
	        throw new BusinessException("Cancellation date must be within the active month dates.");
	    }*/
	}
	
	public void cancelMembership(Integer memberId, CancellationReason reason, String remarks) {
	    // 1. Fetch Member
	    Member member = memberRepo.findByMemberNo(memberId)
	            .orElseThrow(() -> new BusinessException("Member not found."));

	    member.setStatus(MemberStatus.CANCELLED);
	    member.setCancellationDateTime(LocalDateTime.now());
	    member.setCancellationReason(reason);
	    member.setCancellationRemarks(remarks);
	    // Optional: Free the member number if your society policy allows reuse
	    // member.setMemberNo(null); 

	    memberRepo.save(member);
	}

}