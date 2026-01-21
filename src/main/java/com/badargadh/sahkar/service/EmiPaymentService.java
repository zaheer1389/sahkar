package com.badargadh.sahkar.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.badargadh.sahkar.data.EmiPayment;
import com.badargadh.sahkar.repository.EmiPaymentRepository;

@Service
public class EmiPaymentService {

	@Autowired private EmiPaymentRepository emiPaymentRepository;
	
	public EmiPayment recordEmiPayment(EmiPayment emiPayment) {
		return emiPaymentRepository.save(emiPayment);
	}
}
