package com.badargadh.sahkar.controller;

import java.awt.print.PrinterJob;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.badargadh.sahkar.data.AppConfig;
import com.badargadh.sahkar.data.EmiConfig;
import com.badargadh.sahkar.service.AppConfigService;
import com.badargadh.sahkar.util.AppLogger;
import com.badargadh.sahkar.util.DialogManager;
import com.badargadh.sahkar.util.NotificationManager;
import com.badargadh.sahkar.util.NotificationManager.NotificationType;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
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
    
    @FXML private ComboBox<String> cbPrinters;
    
    //Remark settings
    @FXML private Button btnEditRemarkSettings;
    //@FXML private GridPane gridRemarkSettings;
    @FXML private VBox vboxRemarkFields;
    @FXML private Spinner<Integer> spnRemarkDate;
    @FXML private ComboBox<String> cbRemarkHour;
    @FXML private ComboBox<String> cbRemarkMinute;
    @FXML private CheckBox chkAutoRemark;
    
    @FXML private TableView<EmiConfig> tblEmiAmounts;
    @FXML private TableColumn<EmiConfig, Double> colEmiValue;
    @FXML private TableColumn<EmiConfig, Boolean> colEmiStatus;
    @FXML private TableColumn<EmiConfig, Void> colEmiAction;
    @FXML private TextField txtNewEmiValue;
    
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

        handleRefreshPrinters();
        loadCurrentSettings();
        setupEmiTable();
        loadEmiData();
        
        // Apply numeric filters to prevent typing decimals/letters
        applyNumericFilter(txtMonthlyFees);
        applyNumericFilter(txtNewMemberFees);
        applyNumericFilter(txtLoanAmount);
        applyNumericFilter(txtRefundCooling);
        applyNumericFilter(txtNewEmiValue);
    }
    
    @FXML
    private void handleRefreshPrinters() {
        ObservableList<String> printerList = FXCollections.observableArrayList();
        
        // Lookup all installed print services
        PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
        
        for (PrintService printer : printServices) {
            printerList.add(printer.getName());
        }

        cbPrinters.setItems(printerList);

        // Auto-select the system default printer if the list was empty
        if (!printerList.isEmpty()) {
            PrintService defaultService = PrintServiceLookup.lookupDefaultPrintService();
            if (defaultService != null) {
                cbPrinters.setValue(defaultService.getName());
            } else {
                cbPrinters.getSelectionModel().selectFirst();
            }
        }
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
    
    private void loadEmiData() {
        List<EmiConfig> configs = configService.getAllEmiConfigs();
        tblEmiAmounts.setItems(FXCollections.observableArrayList(configs));
        
        double height = (configs.size() * tblEmiAmounts.getFixedCellSize()) + 38;
        tblEmiAmounts.setPrefHeight(height);
        tblEmiAmounts.setMinHeight(height);
        tblEmiAmounts.setMaxHeight(height);
    }

    private void setupEmiTable() {
        colEmiValue.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colEmiStatus.setCellValueFactory(new PropertyValueFactory<>("active"));
        // Custom cell for Status (Enabled/Disabled)
        colEmiStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean active, boolean empty) {
                super.updateItem(active, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Label label = new Label(active ? "ACTIVE" : "DISABLED");
                    label.setStyle(active ? "-fx-text-fill: green; -fx-font-weight: bold;" : "-fx-text-fill: red;");
                    setGraphic(label);
                }
            }
        });

        colEmiAction.setCellFactory(column -> new TableCell<>() {
            // Custom Styled CheckBox to act as a Toggle Switch
            private final CheckBox toggle = new CheckBox();
            
            {
                //toggle.getStyleClass().add("toggle-switch"); // Custom CSS class
                toggle.setCursor(Cursor.HAND);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    EmiConfig config = getTableView().getItems().get(getIndex());
                    
                    // Unbind to prevent recursive trigger during loading
                    toggle.setOnAction(null);
                    toggle.setSelected(config.isActive());
                    
                    // Re-bind action
                    toggle.setOnAction(event -> toggleEmiStatus(config));
                    
                    setGraphic(toggle);
                    setAlignment(Pos.CENTER);
                }
            }
        });
    }
    
    private void toggleEmiStatus(EmiConfig config) {
    	String msg = "Please confirm your identity to disable EMI amount : "+config.getAmount()+". Disabling emi amount will be no longer can be added in emi repayment";
        if(showSecurityGate(msg)) {
        	Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    configService.toggleEmiStatus(config.getId());
                    return null;
                }
            };

            task.setOnSucceeded(e -> {
                // Only refresh the specific item if performance is a concern, 
                // or reload all to be safe
                loadEmiData(); 
                NotificationManager.show("EMI Updated", NotificationType.SUCCESS, Pos.BOTTOM_RIGHT);
            });

            new Thread(task).start();
        }
    }

    @FXML
    private void handleAddEmi() {
        String input = txtNewEmiValue.getText().trim();
        
        try {
            double amount = Double.parseDouble(input);
            
            // Immediate UI Validation
            if (amount % 100 != 0) {
                DialogManager.showError("Invalid Amount", "EMI must be a multiple of 100.");
                return;
            }

            configService.saveEmiConfig(amount);
            loadEmiData(); 
            txtNewEmiValue.clear();
            NotificationManager.show("EMI Configured", NotificationType.SUCCESS, Pos.BOTTOM_RIGHT);
            
        } catch (NumberFormatException e) {
            DialogManager.showError("Input Error", "Please enter a valid numeric value.");
            AppLogger.error("EMI_Amount_Configuration_Error", e);
        } catch (IllegalArgumentException e) {
            // Catches the "Duplicate" or "Negative" error from service
            DialogManager.showError("Configuration Error", e.getMessage());
            AppLogger.error("EMI_Amount_Configuration_Error", e);
        }
    }
    
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
                AppLogger.error("Loan_Related_Configuration_Error", e);
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
    
    @FXML
    private void handleTestPrint() {
        String selectedPrinterName = cbPrinters.getValue();

        if (selectedPrinterName == null || selectedPrinterName.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Printer Warning", "Please select a printer first.");
            return;
        }

        try {
            // Find the actual PrintService object for the selected name
            PrintService selectedService = null;
            PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
            for (PrintService service : services) {
                if (service.getName().equals(selectedPrinterName)) {
                    selectedService = service;
                    break;
                }
            }

            if (selectedService != null) {
                // Use your existing printing utility class (e.g., PosPrinterUtil)
                // If you don't have one, here is a simple implementation:
                printTestReceipt(selectedService);
                
                showAlert(Alert.AlertType.INFORMATION, "Print Success", 
                        "Test receipt sent to " + selectedPrinterName);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Print Error", 
                    "Could not print to " + selectedPrinterName + ": " + e.getMessage());
        }
    }

    private void printTestReceipt(PrintService service) throws Exception {
        // 1. Build the receipt content
        StringBuilder testContent = new StringBuilder();
        testContent.append("--------------------------------\n");
        testContent.append("      SAHKAR ROSCA SYSTEM      \n");
        testContent.append("        PRINTER TEST OK        \n");
        testContent.append("--------------------------------\n");
        testContent.append("Date:   ").append(java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")
        )).append("\n");
        testContent.append("Status: Online\n");
        testContent.append("--------------------------------\n");
        
        // Crucial: Add empty lines at the end so the paper feeds out 
        // far enough to be torn off from the cutter.
        testContent.append("\n\n\n\n\n"); 

        // Optional: Standard ESC/POS Paper Cut Command (Most printers)
        // char[] cutPaper = {0x1D, 0x56, 0x41, 0x00}; 
        // testContent.append(new String(cutPaper));

        // 2. Convert to Bytes
        byte[] bytes = testContent.toString().getBytes();

        // 3. Define the DocFlavor for raw data (AUTOSENSE)
        DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
        
        // 4. Create the Print Job and Document
        DocPrintJob printJob = service.createPrintJob();
        Doc doc = new SimpleDoc(bytes, flavor, null);

        // 5. Execute Print
        printJob.print(doc, null);
        
        AppLogger.info("Raw test receipt bytes sent to: " + service.getName());
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}