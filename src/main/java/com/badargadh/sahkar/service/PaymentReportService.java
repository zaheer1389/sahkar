package com.badargadh.sahkar.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.badargadh.sahkar.data.EmiPayment;
import com.badargadh.sahkar.data.FeePayment;
import com.badargadh.sahkar.data.FinancialMonth;
import com.badargadh.sahkar.data.Member;
import com.badargadh.sahkar.dto.MonthlyPaymentDTO;
import com.badargadh.sahkar.enums.FeeType;
import com.badargadh.sahkar.repository.EmiPaymentRepository;
import com.badargadh.sahkar.repository.FeePaymentRepository;

@Service
public class PaymentReportService {

    @Autowired
    private FeePaymentRepository feeRepo;

    @Autowired
    private EmiPaymentRepository emiRepo;

    public List<MonthlyPaymentDTO> getCombinedPayments(FinancialMonth month) {
        // 1. Fetch all payments for the month
        List<FeePayment> fees = feeRepo.findByFinancialMonth(month);
        List<EmiPayment> emis = emiRepo.findByFinancialMonth(month);

        Map<Long, MonthlyPaymentDTO> reportMap = new HashMap<>();

        // 2. Process Fees
        for (FeePayment f : fees) {
            MonthlyPaymentDTO dto = reportMap.computeIfAbsent(f.getMember().getId(), id -> createDto(f.getMember()));
            
            if (f.getFeeType() == FeeType.MONTHLY_FEE) dto.setMonthlyFee(f.getAmount());
            else if (f.getFeeType() == FeeType.JOINING_FEE) dto.setJoiningFee(f.getAmount());
            else if (f.getFeeType() == FeeType.LOAN_DEDUCTION) dto.setLoanDeduction(f.getAmount());
            
            dto.setTotalPaid(dto.getTotalPaid() + f.getAmount());
            updateDate(dto, f.getTransactionDateTime());
        }

        // 3. Process EMIs
        for (EmiPayment e : emis) {
            MonthlyPaymentDTO dto = reportMap.computeIfAbsent(e.getMember().getId(), id -> createDto(e.getMember()));
            
            dto.setEmiAmount(e.getAmountPaid());
            dto.setFullPayment(e.isFullPayment());
            if(e.isFullPayment()) {
                dto.setFullPaymentExtra(e.getFullPaymentAmount());
            }
            
            dto.setTotalPaid(dto.getTotalPaid() + e.getAmountPaid());
            updateDate(dto, e.getPaymentDateTime());
        }

        return new ArrayList<>(reportMap.values());
    }

    private MonthlyPaymentDTO createDto(Member m) {
        MonthlyPaymentDTO d = new MonthlyPaymentDTO();
        d.setMemberId(m.getId());
        d.setMemberName(m.getFullname());
        d.setMemberNo(m.getMemberNo());
        return d;
    }

    private void updateDate(MonthlyPaymentDTO dto, LocalDateTime dt) {
        if (dto.getLastTransactionDate() == null || dt.isAfter(dto.getLastTransactionDate())) {
            dto.setLastTransactionDate(dt);
        }
    }
}