package com.badargadh.sahkar.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.badargadh.sahkar.data.LoanAccount;
import com.badargadh.sahkar.data.Member;
import com.badargadh.sahkar.dto.PendingPaymentDTO;
import com.badargadh.sahkar.enums.LoanStatus;
import com.badargadh.sahkar.enums.MemberStatus;
import com.badargadh.sahkar.repository.AppConfigRepository;
import com.badargadh.sahkar.repository.LoanAccountRepository;
import com.badargadh.sahkar.repository.MemberRepository;

@Service
public class PendingPaymentService {

    @Autowired private MemberRepository memberRepo;
    @Autowired private LoanAccountRepository loanRepo;
    @Autowired private AppConfigService appConfigService;

    public List<PendingPaymentDTO> calculatePendingPayments() {
        // 1. Get the standard monthly fee from settings
        Double standardFee = appConfigService.getSettings().getMonthlyFees().doubleValue();
        
        // 2. Get all active members
        List<Member> activeMembers = memberRepo.findAllByStatus(MemberStatus.ACTIVE);
        
        // 3. Get all active loans to map them quickly
        Map<Long, LoanAccount> activeLoans = loanRepo.findAllByLoanStatus(LoanStatus.ACTIVE)
            .stream()
            .collect(Collectors.toMap(l -> l.getMember().getId(), l -> l));

        List<PendingPaymentDTO> pendingList = new ArrayList<>();

        for (Member member : activeMembers) {
            PendingPaymentDTO dto = new PendingPaymentDTO();
            dto.setMemberId(member.getId());
            dto.setMemberNo(member.getMemberNo());
            dto.setFullName(member.getFullname());
            dto.setExpectedFee(standardFee);

            // 4. Check for Loan EMI
            LoanAccount loan = activeLoans.get(member.getId());
            if (loan != null) {
                dto.setLoanAccountId(loan.getId());
                dto.setRemainingLoanBalance(loan.getPendingAmount());
                
                // LOGIC: If pending balance < standard EMI, charge only the balance
                double emiToCharge = Math.min(loan.getEmiAmount(), loan.getPendingAmount());
                dto.setExpectedEmi(emiToCharge);
            } else {
                dto.setExpectedEmi(0.0);
            }

            dto.setTotalPending(dto.getExpectedFee() + dto.getExpectedEmi());
            pendingList.add(dto);
        }

        return pendingList;
    }
}