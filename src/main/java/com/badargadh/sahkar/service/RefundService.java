package com.badargadh.sahkar.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.badargadh.sahkar.data.FeesRefund;
import com.badargadh.sahkar.data.FinancialMonth;
import com.badargadh.sahkar.data.Member;
import com.badargadh.sahkar.data.MemberHistory;
import com.badargadh.sahkar.data.PaymentRemark;
import com.badargadh.sahkar.enums.CancellationReason;
import com.badargadh.sahkar.enums.MemberStatus;
import com.badargadh.sahkar.repository.FeesRefundRepository;
import com.badargadh.sahkar.repository.MemberHistoryRepository;
import com.badargadh.sahkar.repository.MemberRepository;
import com.badargadh.sahkar.repository.PaymentRemarkRepository;

@Service
public class RefundService {

    @Autowired private MemberRepository memberRepo;
    @Autowired private MemberHistoryRepository historyRepo;
    @Autowired private FeesRefundRepository refundRepo;
    @Autowired private FinancialMonthService monthService;
    @Autowired private FeeService feeService;
    @Autowired private PaymentRemarkRepository remarkRepository;

    /**
     * Fetches members eligible for refund based on cooling period OR 'Expired' status.
     */
    public List<Member> getEligibleMembers(LocalDate endDate) {
        return memberRepo.findMembersEligibleForRefund(endDate);
    }
    
    public List<Member> getMonthlyRefundReportList(LocalDate startDate, LocalDate endDate) {
        return memberRepo.findMonthlyRefundReportList(startDate, endDate);
    }

    /**
     * Core logic: Records the refund, archives the member, and clears active ID.
     */
    @Transactional
    public void processRefund(Member member, String nomineeName) {
    	
    	Double refundEligibleFees = feeService.getMemberTotalFees(member);
    	
        FinancialMonth activeMonth = monthService.getActiveMonth()
                .orElseThrow(() -> new RuntimeException("No active financial month found."));

        // 1. Create FeesRefund record (This impacts the Monthly Statement)
        FeesRefund refund = new FeesRefund();
        refund.setMember(member);
        refund.setAmount(refundEligibleFees);
        refund.setFinancialMonth(activeMonth);
        refund.setRefundDateTime(LocalDateTime.of(activeMonth.getStartDate().plusDays(15), LocalTime.of(15, 30)));
        
        String remarks = "Refund issued to: " + nomineeName;
        if (member.getCancellationReason() == CancellationReason.MEMBER_EXPIRED) {
            remarks = "[Death Claim] " + remarks;
        }
        refund.setRemarks(remarks);
        refundRepo.save(refund);

        // 2. Snapshot current member data to History
        MemberHistory history = new MemberHistory();
        // Copies Name, Phone, Fees paid, etc., from Member to History entity
        BeanUtils.copyProperties(member, history, "id"); 
    	history.setOriginalMemberId(member.getId());
    	history.setTotalFeesRefunded(refundEligibleFees);
    	history.setRefundDateTime(refund.getRefundDateTime());
    	historyRepo.save(history);

        // 3. Clear the active Member record
        // Setting memberNo to null allows reuse for new members
        member.setMemberNo(null);
        member.setStatus(MemberStatus.REFUNDED); 
        memberRepo.save(member);
        
        List<PaymentRemark> paymentRemarks = remarkRepository.findByMemberAndIsClearedFalseOrderByIssuedDateAsc(member);
        
        for(PaymentRemark remark : paymentRemarks) {
        	remark.setCleared(true);
        	remark.setClearedDate(activeMonth.getStartDate().plusDays(15));
        	remark.setClearingReason("MEMBERSHIP_FEES_REFUND");
        	remarkRepository.save(remark);
        }
    }
}