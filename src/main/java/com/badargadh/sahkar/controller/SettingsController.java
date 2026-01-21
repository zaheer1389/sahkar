package com.badargadh.sahkar.controller;

import java.net.URL;
import java.util.ResourceBundle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.badargadh.sahkar.data.AppConfig;
import com.badargadh.sahkar.service.AppConfigService;
import com.badargadh.sahkar.util.DialogManager;
import com.badargadh.sahkar.util.NotificationManager;
import com.badargadh.sahkar.util.NotificationManager.NotificationType;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

@Component
public class SettingsController extends BaseController implements Initializable {

    // Member Configs
    @FXML private TextField txtMonthlyFees;
    @FXML private Button btnEditMonthly;
    
    @FXML private TextField txtNewMemberFees;
    @FXML private Button btnEditNewMember;

    // Loan Configs
    @FXML private Spinner<Integer> spnCoolingPeriod;
    @FXML private Button btnEditCooling;

    // Operational Limits
    @FXML private TextField txtLoanAmount;
    @FXML private Button btnEditLoan;
    
    //Fees Refund Cooling Period
    @FXML private TextField txtRefundCooling;
    @FXML private Button btnEditRefundCooling;
    
    //Remark settings
    @FXML private Button btnEditRemarkSettings;
    //@FXML private GridPane gridRemarkSettings;
    @FXML private VBox vboxRemarkFields;
    @FXML private Spinner<Integer> spnRemarkDate;
    @FXML private ComboBox<String> cbRemarkHour;
    @FXML private ComboBox<String> cbRemarkMinute;
    @FXML private CheckBox chkAutoRemark;
    
    @Autowired
    private AppConfigService configService;
    
    private boolean isRemarkEditMode = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize Spinner range (0 to 60 months, default 6)
        spnCoolingPeriod.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 60, 6)
        );
        
        // 1. Setup Spinner for Day (1-28 to avoid February issues)
        spnRemarkDate.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 28, 11));

        // 2. Setup Time Drops
        for (int i = 0; i < 24; i++) cbRemarkHour.getItems().add(String.format("%02d", i));
        for (int i = 0; i < 60; i += 5) cbRemarkMinute.getItems().add(String.format("%02d", i));

        loadCurrentSettings();
        
        // Apply numeric filters to prevent typing decimals/letters
        applyNumericFilter(txtMonthlyFees);
        applyNumericFilter(txtNewMemberFees);
        applyNumericFilter(txtLoanAmount);
        applyNumericFilter(txtRefundCooling);
    }

    /**
     * Loads the singleton config from the database into the UI.
     */
    private void loadCurrentSettings() {
        AppConfig config = configService.getSettings();
        
        txtMonthlyFees.setText(String.valueOf(config.getMonthlyFees()));
        txtNewMemberFees.setText(String.valueOf(config.getNewMemberFees()));
        txtLoanAmount.setText(String.valueOf(config.getLoanAmount()));
        spnCoolingPeriod.getValueFactory().setValue(config.getNewMemberCoolingPeriod());
        txtRefundCooling.setText(String.valueOf(config.getFeesRefundCoolingPeriod()));
        
        chkAutoRemark.setSelected(config.isAutoRemark());
        spnRemarkDate.getValueFactory().setValue(config.getRemarkDateOfMonth());
        cbRemarkHour.setValue(String.format("%02d", config.getRemarkTimeHour()));
        cbRemarkMinute.setValue(String.format("%02d", config.getRemarkTimeMinute()));
    }

    // --- Action Handlers ---

    @FXML
    private void toggleMonthlyEdit() {
        handleInlineEdit(txtMonthlyFees, btnEditMonthly, "monthlyFees");
    }

    @FXML
    private void toggleNewMemberEdit() {
        handleInlineEdit(txtNewMemberFees, btnEditNewMember, "newMemberFees");
    }

    @FXML
    private void toggleLoanEdit() {
        handleInlineEdit(txtLoanAmount, btnEditLoan, "loanAmount");
    }
    
    @FXML
    private void toggleRefundCoolingEdit() {
    	handleInlineEdit(txtRefundCooling, btnEditRefundCooling, "feesRefundCoolingPeriod");
    }

    @FXML
    private void toggleCoolingEdit() {
        if (spnCoolingPeriod.isDisable()) {
            // Enable mode
            spnCoolingPeriod.setDisable(false);
            btnEditCooling.setText("✔");
            btnEditCooling.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        } else {
            // Save mode
            AppConfig config = configService.getSettings();
            config.setNewMemberCoolingPeriod(spnCoolingPeriod.getValue());
            configService.saveSettings(config);
            
            spnCoolingPeriod.setDisable(true);
            btnEditCooling.setText("✎");
            btnEditCooling.setStyle("");
            DialogManager.showInfo("Saved", "Cooling period updated.");
        }
    }

    /**
     * Unified logic for TextField in-line editing.
     */
    private void handleInlineEdit(TextField field, Button btn, String fieldName) {
        if (field.isDisable()) {
            // Switch to EDIT mode
            field.setDisable(false);
            field.requestFocus();
            btn.setText("✔");
            btn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        } else {
            // Switch to SAVE mode
            try {
                String input = field.getText().trim();
                if (input.isEmpty()) throw new NumberFormatException("Empty");

                long value = Long.parseLong(input);
                AppConfig config = configService.getSettings();

                if(fieldName.equals("monthlyFees")) config.setMonthlyFees(value);
                else if(fieldName.equals("newMemberFees")) config.setNewMemberFees(value);
                else if(fieldName.equals("loanAmount")) config.setLoanAmount(value);
                else if(fieldName.equals("feesRefundCoolingPeriod")) config.setFeesRefundCoolingPeriod((int)value);
                
                configService.saveSettings(config);
                
                // Reset UI to Locked state
                field.setDisable(true);
                btn.setText("✎");
                btn.setStyle("");
                DialogManager.showInfo("Updated", "Settings saved successfully.");
                
            } catch (NumberFormatException e) {
                DialogManager.showError("Invalid Input", "Please enter a valid whole number.");
                // Reset field to DB value on error
                loadCurrentSettings();
            }
        }
    }
    
    @FXML
    private void handleToggleRemarkEdit() {
        if (!isRemarkEditMode) {
            // Switch to EDIT Mode
            enableRemarkFields(true);
            btnEditRemarkSettings.setText("✔");
            btnEditRemarkSettings.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
            isRemarkEditMode = true;
        } else {
            // Switch to VIEW Mode & SAVE
            saveRemarkSettings();
            enableRemarkFields(false);
            btnEditRemarkSettings.setText("✎");
            btnEditRemarkSettings.setStyle("");
            isRemarkEditMode = false;
        }
    }

    private void enableRemarkFields(boolean enable) {
    	vboxRemarkFields.setDisable(!enable);
    }

    private void saveRemarkSettings() {
        AppConfig config = configService.getSettings();
        
        // Using your specific fields
        config.setAutoRemark(chkAutoRemark.isSelected());
        config.setRemarkDateOfMonth(spnRemarkDate.getValue());
        config.setRemarkTimeHour(Integer.parseInt(cbRemarkHour.getValue()));
        config.setRemarkTimeMinute(Integer.parseInt(cbRemarkMinute.getValue()));
        
        configService.saveSettings(config);
        NotificationManager.show("Disciplinary rules updated successfully!", NotificationType.SUCCESS, Pos.CENTER);
    }

    @FXML
    private void handleReset() {
        loadCurrentSettings();
    }

    private void applyNumericFilter(TextField field) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                field.setText(newVal.replaceAll("[^\\d]", ""));
            }
        });
    }
}