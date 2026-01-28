package com.badargadh.sahkar.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.badargadh.sahkar.data.FinancialMonth;
import com.badargadh.sahkar.data.MonthlyStatementDTO;
import com.badargadh.sahkar.repository.FinancialMonthRepository;
import com.badargadh.sahkar.service.FinancialMonthService;
import com.badargadh.sahkar.service.MonthlyStatementService;
import com.badargadh.sahkar.util.AppLogger;
import com.badargadh.sahkar.util.NotificationManager;
import com.badargadh.sahkar.util.NotificationManager.NotificationType;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

@Component
public class MonthlyStatementController {
	
	@FXML private TextArea txtClosingRemarks;
    @FXML private Button btnEditSave;
    
    @Autowired private FinancialMonthRepository monthRepo;
    
    private FinancialMonth currentMonth;
    private boolean isEditMode = false;
	
    @FXML private Label lblOpeningBalance, lblClosingBalance, valNewMemberFee, valMonthlyFee, valLoanDeduction, valTotalEmi;
    @FXML private Label valFullPayment, valMiscCredit, valLoanGranted, valFeeRefund, valExpenseDebit;
    @FXML private Label valTotalIncome;
    @FXML private Label valTotalOutgoing;
    
    @Autowired private MonthlyStatementService statementService;
    @Autowired private FinancialMonthService financialMonthService;

    public void refresh() {
        try {
        	currentMonth = financialMonthService.getActiveMonth()
        			.orElseThrow(() -> new IllegalStateException("No active month found."));
        	
        	MonthlyStatementDTO dto = statementService.getActiveMonthStatement(currentMonth);
            
        	lblOpeningBalance.setText(formatCurrency(dto.getOpeningBal()));
            lblClosingBalance.setText(formatCurrency(dto.getClosingBalance()));

            valNewMemberFee.setText(formatCurrency(dto.getNewMemberFee()));
            valMonthlyFee.setText(formatCurrency(dto.getMonthlyFee()));
            valLoanDeduction.setText(formatCurrency(dto.getLoanDeduction()));
            valTotalEmi.setText(formatCurrency(dto.getTotalEmi()));
            valFullPayment.setText(formatCurrency(dto.getFullPaymentAmount()));
            valMiscCredit.setText(formatCurrency(dto.getExpenseCredit()));

            valLoanGranted.setText(formatCurrency(dto.getTotalLoanGranted()));
            valFeeRefund.setText(formatCurrency(dto.getTotalFeeRefund()));
            valExpenseDebit.setText(formatCurrency(dto.getExpenseDebit()));
            
            txtClosingRemarks.setText(currentMonth.getClosingRemarks());
            txtClosingRemarks.setEditable(false);
            
            valTotalIncome.setText(formatCurrency(dto.getTotalIncome()));
            valTotalOutgoing.setText(formatCurrency(dto.getTotalOutgoing()));
            
            // Final Closing Balance
            double closing = dto.getClosingBalance();
            lblClosingBalance.setText(formatCurrency(closing));
            
            // Dynamic color for closing balance
            if (closing < 0) {
                lblClosingBalance.setStyle("-fx-text-fill: #c0392b; -fx-font-size: 34px; -fx-font-weight: bold;");
            } else {
                lblClosingBalance.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 34px; -fx-font-weight: bold;");
            }
        }
        catch(Exception e) {
        	NotificationManager.show(e.getMessage(), NotificationType.ERROR, Pos.CENTER);
            AppLogger.error("Monthly_Statement_Data_Load_Error", e);
        }
    }

    private String formatCurrency(Double amount) {
        return String.format("₹ %.0f/-", amount);
    }
    
    @FXML
    private void handleEditSaveRemarks() {
        if (!isEditMode) {
            // Switch to Edit Mode
            isEditMode = true;
            txtClosingRemarks.setEditable(true);
            txtClosingRemarks.setStyle("-fx-control-inner-background: #fff9c4; -fx-font-size: 14px;"); // Light yellow background
            btnEditSave.setText("Save Remarks");
            btnEditSave.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        } else {
            // Save Data
            saveRemarksToDatabase();
            
            // Switch back to View Mode
            isEditMode = false;
            txtClosingRemarks.setEditable(false);
            txtClosingRemarks.setStyle("-fx-control-inner-background: white; -fx-font-size: 14px;");
            btnEditSave.setText("Edit Remarks");
            btnEditSave.setStyle("-fx-background-color: #34495e; -fx-text-fill: white;");
            
            NotificationManager.show("Remarks saved successfully!", NotificationType.SUCCESS, Pos.CENTER);
        }
    }

    private void saveRemarksToDatabase() {
        String remarks = txtClosingRemarks.getText();
        currentMonth.setClosingRemarks(remarks);
        monthRepo.save(currentMonth);
    }
}