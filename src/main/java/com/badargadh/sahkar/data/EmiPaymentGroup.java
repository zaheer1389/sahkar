package com.badargadh.sahkar.data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.badargadh.sahkar.enums.CollectionLocation;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "emi_payment_groups")
public class EmiPaymentGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The person who physically brought the cash to the office/agent
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deposited_by_id")
    private Member depositedBy;

    // Fallback name if the depositor is not a registered member
    @Column(name = "depositor_name")
    private String depositorName;

    @Enumerated(EnumType.STRING)
    @Column(name = "collection_location")
    private CollectionLocation collectionLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "financial_month_id")
    private FinancialMonth financialMonth;

    @Column(name = "total_amount")
    private Double totalAmount = 0.0;

    @Column(name = "payment_count")
    private Integer paymentCount = 0;

    @Column(name = "transaction_date_time")
    private LocalDateTime transactionDateTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "added_by_id")
    private AppUser addedBy;

    // --- RELATIONSHIPS ---

    // Linking all EMIs processed in this batch
    @OneToMany(mappedBy = "paymentGroup", cascade = CascadeType.ALL)
    private List<EmiPayment> emiPayments = new ArrayList<>();

    // Linking all Fees processed in this batch
    @OneToMany(mappedBy = "paymentGroup", cascade = CascadeType.ALL)
    private List<FeePayment> feePayments = new ArrayList<>();
    
    @Transient
    private List<MonthlyPayment> monthlyPayments;

    // Helper method to add EMI and maintain sync
    public void addEmiPayment(EmiPayment emi) {
        emiPayments.add(emi);
        emi.setPaymentGroup(this);
    }

    // Helper method to add Fee and maintain sync
    public void addFeePayment(FeePayment fee) {
        feePayments.add(fee);
        fee.setPaymentGroup(this);
    }

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Member getDepositedBy() {
		return depositedBy;
	}

	public void setDepositedBy(Member depositedBy) {
		this.depositedBy = depositedBy;
	}

	public String getDepositorName() {
		return depositorName;
	}

	public void setDepositorName(String depositorName) {
		this.depositorName = depositorName;
	}

	public CollectionLocation getCollectionLocation() {
		return collectionLocation;
	}

	public void setCollectionLocation(CollectionLocation collectionLocation) {
		this.collectionLocation = collectionLocation;
	}

	public FinancialMonth getFinancialMonth() {
		return financialMonth;
	}

	public void setFinancialMonth(FinancialMonth financialMonth) {
		this.financialMonth = financialMonth;
	}

	public Double getTotalAmount() {
		return totalAmount;
	}

	public void setTotalAmount(Double totalAmount) {
		this.totalAmount = totalAmount;
	}

	public Integer getPaymentCount() {
		return paymentCount;
	}

	public void setPaymentCount(Integer paymentCount) {
		this.paymentCount = paymentCount;
	}

	public LocalDateTime getTransactionDateTime() {
		return transactionDateTime;
	}

	public void setTransactionDateTime(LocalDateTime transactionDateTime) {
		this.transactionDateTime = transactionDateTime;
	}

	public AppUser getAddedBy() {
		return addedBy;
	}

	public void setAddedBy(AppUser addedBy) {
		this.addedBy = addedBy;
	}

	public List<EmiPayment> getEmiPayments() {
		return emiPayments;
	}

	public void setEmiPayments(List<EmiPayment> emiPayments) {
		this.emiPayments = emiPayments;
	}

	public List<FeePayment> getFeePayments() {
		return feePayments;
	}

	public void setFeePayments(List<FeePayment> feePayments) {
		this.feePayments = feePayments;
	}

	public List<MonthlyPayment> getMonthlyPayments() {
		return monthlyPayments;
	}

	public void setMonthlyPayments(List<MonthlyPayment> monthlyPayments) {
		this.monthlyPayments = monthlyPayments;
	}
    
    
}