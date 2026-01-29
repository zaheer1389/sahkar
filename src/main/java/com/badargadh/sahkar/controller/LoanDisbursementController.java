package com.badargadh.sahkar.controller;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.badargadh.sahkar.data.LoanAccount;
import com.badargadh.sahkar.data.LoanApplication;
import com.badargadh.sahkar.data.Member;
import com.badargadh.sahkar.enums.CollectionType;
import com.badargadh.sahkar.enums.LoanStatus;
import com.badargadh.sahkar.service.FeeService;
import com.badargadh.sahkar.service.LoanDisbursementService;
import com.badargadh.sahkar.service.LoanService;
import com.badargadh.sahkar.service.MemberService;
import com.badargadh.sahkar.service.ReceiptPrintingService;
import com.badargadh.sahkar.util.AppLogger;
import com.badargadh.sahkar.util.DialogManager;
import com.badargadh.sahkar.util.NotificationManager;
import com.badargadh.sahkar.util.NotificationManager.NotificationType;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

@Component
public class LoanDisbursementController extends BaseController {

	@FXML private Label lblHeaderMemberNo, lblFeesDeductionAmt;
	@FXML private Label lblPrevLoanDeduction, lblNetPayable;
    @FXML private Label lblHeaderMemberName, lblHeaderAmount, lblWitness1Name, lblWitness2Name, lblReceiverName;
    @FXML private TextField txtWitness1, txtWitness2, txtAuthorityName;
    @FXML private TextArea txtRemarks;
    
    //@FXML private TextField txtReceiverNo;
    @FXML private RadioButton rbSelf, rbRelative;
    @FXML private VBox vboxRelative;
    @FXML private Button btnConfirm;

    @Autowired private MemberService memberService;
    @Autowired private LoanDisbursementService disbursementService;
    @Autowired private FeeService feeService;
    @Autowired private LoanService loanService;
    @Autowired private ReceiptPrintingService printingService;

    private LoanApplication selectedApp;
    private LoanAccount activeLoanAccount;

    public void initData(LoanApplication app) {
        this.selectedApp = app;
        
        double newAmount = app.getAppliedAmount();
        double oldPending = 0;
        
        activeLoanAccount = loanService.findMemberActiveLoan(app.getMember());
        if(activeLoanAccount != null) {
        	oldPending = activeLoanAccount.getPendingAmount();
        }
        
        Double feesDeduction = feeService.getMemberFeeDeductionOnFirstLoan(app.getMember());
        double netToPay = newAmount - oldPending - feesDeduction;
        
        // Populate Header Details
        lblHeaderMemberName.setText(app.getMember().getFullname().toUpperCase());
        lblHeaderMemberNo.setText(app.getMember().getMemberNo()+"");
        lblHeaderAmount.setText(String.format("₹ %.0f", app.getAppliedAmount()));
        
        lblPrevLoanDeduction.setText(String.format("₹ %.2f", oldPending));
        lblNetPayable.setText(String.format("₹ %.2f", netToPay));
        lblFeesDeductionAmt.setText(String.format("₹ %.0f", feesDeduction));
        
        applyNumericFilter(txtWitness1);
        applyNumericFilter(txtWitness2);
        //applyNumericFilter(txtReceiverNo);
    }

    @FXML
    private void toggleCollectionUI() {
        boolean showRelative = rbRelative.isSelected();
        vboxRelative.setVisible(showRelative);
        vboxRelative.setManaged(showRelative);
        
        // Get the current window and force it to resize to fit the new height
        Stage stage = (Stage) btnConfirm.getScene().getWindow();
        stage.sizeToScene();
    }

    @FXML private void handleWitness1Fetch() { fetchMember(txtWitness1, lblWitness1Name); }
    @FXML private void handleWitness2Fetch() { fetchMember(txtWitness2, lblWitness2Name); }
    //@FXML private void handleReceiverFetch() { fetchMember(txtReceiverNo, lblReceiverName); }

    private void fetchMember(TextField tf, Label lbl) {
        try {
            Member m = memberService.findByMemberNumber(Integer.parseInt(tf.getText().trim()));
            lbl.setText("Name: " + m.getFullname());
        } catch (Exception e) { lbl.setText("Name: Not Found"); }
    }

    @FXML
    private void handleDisburse() {
        try {
        	CollectionType type = rbSelf.isSelected() ? CollectionType.SELF : CollectionType.AUTHORITY;
            if(isValidWitnessDetails(type)) {
            	if(showSecurityGate("Please confirm your identity using password to disbursed loan amount.")) {
                	
                    //String receiverNo = rbSelf.isSelected() ? selectedApp.getMember().getMemberNo()+"" : txtReceiverNo.getText();
                    String remarks = txtRemarks.getText();

                    LoanAccount loanAccount = disbursementService.processDisbursement(selectedApp, 
                        Arrays.asList(txtWitness1.getText(), txtWitness2.getText()), type, txtAuthorityName.getText(), remarks);

                    printingService.printLoanDisbursementReceipt(selectedApp, loanAccount, false);

                    if(loanAccount.getLoanStatus() == LoanStatus.ACTIVE) {
                    	NotificationManager.show("Loan disbursed succssfully and recipt printed!", NotificationType.SUCCESS, Pos.CENTER);
                    }
                    
                    boolean wantAnother = DialogManager.confirm(
                            "Print Duplicate?", 
                            "Would you like to print a duplicate copy or re-print due to paper issues?"
                        );

                    if (wantAnother) {
                        printingService.printLoanDisbursementReceipt(selectedApp, loanAccount, true);
                        NotificationManager.show("Duplicate Receipt printed!", NotificationType.SUCCESS, Pos.CENTER);
                    }
                    
                    close();
                }
            }
        } catch (Exception e) {
        	e.printStackTrace();
        	AppLogger.error("Loan_Disbursment_Error", e);
            NotificationManager.show(e.getMessage(), NotificationType.ERROR, Pos.BOTTOM_CENTER);
        }
    }

    @FXML private void handleCapture(ActionEvent e) { /* Placeholder for Webcam */ }
    @FXML private void handleCancel() { close(); }
    private void close() { ((Stage) btnConfirm.getScene().getWindow()).close(); }
    
    private void applyNumericFilter(TextField field) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                field.setText(newVal.replaceAll("[^\\d]", ""));
            }
        });
    }
    
    public boolean isValidWitnessDetails(CollectionType type) {
        // Get the current applicant's member number to compare
        String applicantNo = selectedApp.getMember().getMemberNo()+"";

        // 1. Validate Witness 1
        if(txtWitness1.getText().isEmpty() || lblWitness1Name.getText().isEmpty()) {
            NotificationManager.show("Please enter witness 1 details", NotificationType.ERROR, Pos.CENTER);
            txtWitness1.requestFocus();
            return false;
        }
        else if(lblWitness1Name.getText().contains("Not Found")) {
            NotificationManager.show("Please enter witness 1 correct details", NotificationType.ERROR, Pos.CENTER);
            txtWitness1.requestFocus();
            return false;
        }
        // NEW: Check if Witness 1 is the Applicant
        else if(txtWitness1.getText().trim().equals(applicantNo)) {
            NotificationManager.show("Applicant cannot be Witness 1", NotificationType.ERROR, Pos.CENTER);
            txtWitness1.requestFocus();
            return false;
        }
        
        // 2. Validate Witness 2
        /*if(txtWitness2.getText().isEmpty() || lblWitness2Name.getText().isEmpty()) {
            NotificationManager.show("Please enter witness 2 details", NotificationType.ERROR, Pos.CENTER);
            txtWitness2.requestFocus();
            return false;
        }
        else if(lblWitness2Name.getText().contains("Not Found")) {
            NotificationManager.show("Please enter witness 2 correct details", NotificationType.ERROR, Pos.CENTER);
            txtWitness2.requestFocus();
            return false;
        }*/
        // NEW: Check if Witness 2 is the Applicant
        else if(txtWitness2 != null && txtWitness2.getText().length() > 0 && txtWitness2.getText().trim().equals(applicantNo)) {
            NotificationManager.show("Applicant cannot be Witness 2", NotificationType.ERROR, Pos.CENTER);
            txtWitness2.requestFocus();
            return false;
        }

        // BONUS: Ensure Witness 1 and Witness 2 are not the same person
        if(txtWitness2 != null && txtWitness2.getText().length() > 0 && 
        		txtWitness1.getText().trim().equals(txtWitness2.getText().trim())) {
            NotificationManager.show("Witness 1 and Witness 2 must be different members", NotificationType.ERROR, Pos.CENTER);
            txtWitness2.requestFocus();
            return false;
        }
        
        if(type == CollectionType.AUTHORITY) {
        	if(txtAuthorityName.getText() == null || txtAuthorityName.getText().isEmpty()) {
        		NotificationManager.show("Please provide authority person name", NotificationType.ERROR, Pos.CENTER);
        		txtAuthorityName.requestFocus();
                return false;
        	}
        }

        return true;
    }
}

