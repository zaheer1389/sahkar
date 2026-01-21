package com.badargadh.sahkar.enums;

public enum LoanApplicationStatus {
    APPLIED,           // Form submitted to the office
    SELECTED_IN_DRAW,  // Lucky draw winner
    DISBURSED,         // Cash handed over, fees deducted
    REJECTED,           // Application cancelled/invalid
    REJECTED_FOR_REMARK,
    WAITING,
    NO_SHOW
}