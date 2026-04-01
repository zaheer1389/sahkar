package com.badargadh.sahkar.repository.specification;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.badargadh.sahkar.data.FinancialMonth;
import com.badargadh.sahkar.data.LoanApplication;
import com.badargadh.sahkar.enums.CollectionType;
import com.badargadh.sahkar.enums.LoanApplicationStatus;

import jakarta.persistence.criteria.Predicate;

public class LoanApplicationSpecifications {

	public static Specification<LoanApplication> filterBy(String text, FinancialMonth month, LoanApplicationStatus status, CollectionType type) {
	    return (root, query, cb) -> {
	        List<Predicate> predicates = new ArrayList<>();

	        // 1. Search Logic
	        if (text != null && !text.isEmpty()) {
	            String pattern = "%" + text.toLowerCase() + "%";
	            predicates.add(cb.or(
	                cb.like(cb.lower(root.get("member").get("fullname")), pattern),
	                cb.like(root.get("member").get("memberNo").as(String.class), pattern)
	            ));
	        }

	        // 2. Date Range Logic
	        if (month != null) {
	            predicates.add(cb.between(root.get("applicationDateTime"), 
	                month.getStartDate().atStartOfDay(), 
	                month.getEndDate().atTime(23, 59, 59)));
	        }

	        // 3. Status Logic
	        if (status != null) {
	            predicates.add(cb.equal(root.get("status"), status));
	        }
	        
	        if(type != null) {
	            predicates.add(cb.equal(root.get("collectionType"), type));
	        }

	        // --- THE BYPASS ---
	        // Instead of predicates.toArray(), we combine them one by one
	        Predicate finalPredicate = cb.conjunction(); // This is 'TRUE'
	        for (Predicate p : predicates) {
	            finalPredicate = cb.and(finalPredicate, p);
	        }
	        
	        return finalPredicate;
	    };
	}
}