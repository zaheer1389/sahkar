package com.badargadh.sahkar.controller;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.badargadh.sahkar.data.FinancialMonth;
import com.badargadh.sahkar.enums.MonthStatus;
import com.badargadh.sahkar.event.FinancialStatusChangedEvent;
import com.badargadh.sahkar.service.FinancialMonthService;
import com.badargadh.sahkar.util.AppLogger;
import com.badargadh.sahkar.util.DialogManager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

@Component
public class FinancialMonthController extends BaseController implements Initializable {

    @FXML private TableView<FinancialMonth> tblMonths;
    @FXML private TableColumn<FinancialMonth, String> colMonthId, colMonthName;
    @FXML private TableColumn<FinancialMonth, Integer> colYear;
    @FXML private TableColumn<FinancialMonth, Double> colOpeningBalance, colClosingBalance;
    @FXML private TableColumn<FinancialMonth, MonthStatus> colStatus;

    
    @FXML private ComboBox<String> cmbMonthName;
    @FXML private ComboBox<Integer> cmbYear;
    
    @FXML private Label lblCurrentStatus, lblSelectionHint;
    @FXML private Button btnOpenMonth, btnCloseMonth;

    @Autowired
    private FinancialMonthService monthService;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        populateComboBoxes();
        loadMonthData();
        
     // Perform the one-time bulk recalculation on page load
        try {
            monthService.recalculateAllBalances();
        } catch (Exception e) {
        	AppLogger.error("Initial balance sync failed", e);
        }

        // Listener: Update buttons based on selection in the table
        tblMonths.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                syncSelectionToCombo(newVal);
                updateButtonStates(newVal);
            }
        });

        // Listener: Update buttons based on ComboBox changes (for manual search/open)
        cmbMonthName.valueProperty().addListener((obs, old, val) -> validateManualSelection());
        cmbYear.valueProperty().addListener((obs, old, val) -> validateManualSelection());
        
        // ADD THIS: Shortcut for ENTER key
        javafx.event.EventHandler<javafx.scene.input.KeyEvent> enterKeyHandler = event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                handleOpenMonth();
            }
        };

        cmbMonthName.setOnKeyPressed(enterKeyHandler);
        cmbYear.setOnKeyPressed(enterKeyHandler);
    }

    private void setupTableColumns() {
        colMonthId.setCellValueFactory(new PropertyValueFactory<>("monthId"));
        colMonthName.setCellValueFactory(new PropertyValueFactory<>("monthName"));
        colYear.setCellValueFactory(new PropertyValueFactory<>("year"));
        colOpeningBalance.setCellValueFactory(new PropertyValueFactory<>("openingBalance"));
        colClosingBalance.setCellValueFactory(new PropertyValueFactory<>("closingBalance"));

        // Status Column with Rounded Badges
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(MonthStatus item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(item.toString());
                    String color = "";
                    if(item == MonthStatus.OPEN) color = "#27ae60";   // Green
                    if(item == MonthStatus.CLOSED) color = "#e74c3c"; // Red
                    else color = "#95a5a6";     // Grey (BLANK)
                    
                    badge.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; " +
                                  "-fx-padding: 3 10; -fx-background-radius: 5; -fx-font-weight: bold;");
                    setGraphic(badge);
                }
            }
        });

        // Numeric Columns (Right Aligned, Zero hidden)
        setupNumericColumn(colOpeningBalance, "openingBalance");
        setupNumericColumn(colClosingBalance, "closingBalance");
    }

    private void setupNumericColumn(TableColumn<FinancialMonth, Double> column, String property) {
        column.setCellValueFactory(new PropertyValueFactory<>(property));
        column.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item == 0) {
                    setText("");
                } else {
                    setText(String.format("%.0f", item)); // Show as Integer
                    setStyle("-fx-alignment: CENTER-RIGHT; -fx-padding: 0 10 0 0;");
                }
            }
        });
    }

    private void populateComboBoxes() {
        cmbMonthName.setItems(FXCollections.observableArrayList(
            "JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE", 
            "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER"
        ));
        
        int currentYear = LocalDate.now().getYear();
        cmbYear.setItems(FXCollections.observableArrayList(currentYear - 1, currentYear, currentYear + 1));
        cmbYear.setValue(currentYear);
    }

    private void loadMonthData() {
        ObservableList<FinancialMonth> data = FXCollections.observableArrayList(monthService.getAllMonths());
        tblMonths.setItems(data);

        // Update Header System Status
        monthService.getActiveMonth().ifPresentOrElse(
            m -> {
                lblCurrentStatus.setText("System Active: " + m.getMonthName() + " " + m.getYear());
                lblCurrentStatus.setStyle("-fx-text-fill: #27ae60;");
            },
            () -> {
                lblCurrentStatus.setText("System Status: LOCKED (No Open Month)");
                lblCurrentStatus.setStyle("-fx-text-fill: #e74c3c;");
            }
        );
    }

    private void syncSelectionToCombo(FinancialMonth month) {
        cmbMonthName.setValue(month.getMonthName());
        cmbYear.setValue(month.getYear());
    }

    private void updateButtonStates(FinancialMonth month) {
        //btnOpenMonth.setDisable(month.getStatus() != MonthStatus.BLANK);
        //btnCloseMonth.setDisable(month.getStatus() != MonthStatus.OPEN);
        lblSelectionHint.setText("Selected: " + month.getMonthId() + " Status: " + month.getStatus());
    }

    private void validateManualSelection() {
        // logic to check if selected month/year in combo exists in table
        // and update buttons accordingly
    }

    @FXML
    private void handleOpenMonth() {
        String month = cmbMonthName.getValue();
        Integer year = cmbYear.getValue();

        if (month == null || year == null) {
            DialogManager.showError("Required", "Please select Month and Year.");
            return;
        }

        if (DialogManager.confirm("Open Period", "Open " + month + " " + year + " for transactions?")) {
            try {
                monthService.openMonthByDetails(month, year);
                loadMonthData();
                eventPublisher.publishEvent(new FinancialStatusChangedEvent(this));
                DialogManager.showInfo("Success", month + " is now the active financial period.");
                
            } catch (Exception e) {
                DialogManager.showError("Process Blocked", e.getMessage());
            }
        }
    }

    @FXML
    private void handleCloseMonth() {
        FinancialMonth active = monthService.getActiveMonth().orElse(null);
        if (active == null) {
            DialogManager.showError("Error", "There is no open month to close.");
            return;
        }

        String confirmMsg = "Close " + active.getMonthName() + "?\nThis will lock records and carry forward balance: ₹" + active.getClosingBalance();
        
        if (DialogManager.confirm("Finalize Accounts", confirmMsg)) {
            try {
                monthService.closeActiveMonth();
                loadMonthData();
                eventPublisher.publishEvent(new FinancialStatusChangedEvent(this));
                DialogManager.showInfo("Closed", active.getMonthName() + " is now finalized.");
            } catch (Exception e) {
                DialogManager.showError("Process Failed", e.getMessage());
                AppLogger.error("Financial_Month_Closing_Error", e);
            }
        }
    }
}