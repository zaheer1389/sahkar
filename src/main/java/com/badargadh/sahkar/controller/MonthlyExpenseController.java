package com.badargadh.sahkar.controller;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.badargadh.sahkar.data.MonthlyExpense;
import com.badargadh.sahkar.enums.ExpenseCategory;
import com.badargadh.sahkar.enums.ExpenseType;
import com.badargadh.sahkar.exception.BusinessException;
import com.badargadh.sahkar.service.MonthlyExpenseService;
import com.badargadh.sahkar.util.AppLogger;
import com.badargadh.sahkar.util.DialogManager;
import com.badargadh.sahkar.util.NotificationManager;
import com.badargadh.sahkar.util.NotificationManager.NotificationType;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

@Component
public class MonthlyExpenseController extends BaseController {

	@FXML private ComboBox<ExpenseCategory> cmbCategory;
    @FXML private ComboBox<ExpenseType> cmbType;
    @FXML private TextField txtAmount;
    @FXML private TextArea txtRemarks;
    
    @FXML private TableView<MonthlyExpense> tblExpenses;
    @FXML private TableColumn<MonthlyExpense, LocalDate> colDate;
    @FXML private TableColumn<MonthlyExpense, String> colType, colCategory, colRemarks;
    @FXML private TableColumn<MonthlyExpense, Double> colAmount;

    @Autowired private MonthlyExpenseService expenseService;

    @FXML
    public void initialize() {
        // 1. Setup Dropdowns
        cmbCategory.setItems(FXCollections.observableArrayList(ExpenseCategory.values()));
        cmbType.setItems(FXCollections.observableArrayList(ExpenseType.values()));

        // 2. Logic: Auto-select Type based on Jammat Category selection
        cmbCategory.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == ExpenseCategory.PAYMENT_DEBIT_TO_JAMMAT_BADARGADH) {
                cmbType.setValue(ExpenseType.DEBIT);
            } else if (newVal == ExpenseCategory.PAYMENT_CREDIT_FROM_JAMMAT_BADARGADH) {
                cmbType.setValue(ExpenseType.CREDIT);
            }
        });

        setupTableColumns();
        refreshTable();
    }
    
    @FXML
    public void setupTableColumns() {
        
        // 2. Setup Table Columns
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colRemarks.setCellValueFactory(new PropertyValueFactory<>("remarks"));

    }
    
    @FXML
    private void handleSaveExpense() {
        try {
            // Validation
            if (cmbCategory.getValue() == null || cmbType.getValue() == null || txtAmount.getText().isEmpty()) {
                NotificationManager.show("Required fields missing", NotificationType.WARNING, Pos.CENTER);
                return;
            }

            // Cross-check Jammat logic to prevent user error
            ExpenseCategory cat = cmbCategory.getValue();
            ExpenseType type = cmbType.getValue();
            
            if (cat == ExpenseCategory.PAYMENT_DEBIT_TO_JAMMAT_BADARGADH && type != ExpenseType.DEBIT) {
                throw new BusinessException("Debit category must have DEBIT type selected.");
            }
            if (cat == ExpenseCategory.PAYMENT_CREDIT_FROM_JAMMAT_BADARGADH && type != ExpenseType.CREDIT) {
                throw new BusinessException("Credit category must have CREDIT type selected.");
            }

            MonthlyExpense expense = new MonthlyExpense();
            expense.setDate(LocalDate.now());
            expense.setCategory(cat);
            expense.setType(type);
            expense.setAmount(Double.parseDouble(txtAmount.getText()));
            expense.setRemarks(txtRemarks.getText());

            expenseService.saveExpense(expense);
            
            // Show Balance Info for Jammat transactions
            if (cat.name().contains("JAMMAT")) {
                showJammatSummary();
            }

            clearForm();
            refreshTable();
            NotificationManager.show("Transaction Saved", NotificationType.SUCCESS, Pos.TOP_RIGHT);

        } catch (BusinessException e) {
            DialogManager.showError("Logic Error", e.getMessage());
            AppLogger.error("Monthly_Expense_Page_Error", e);
        } catch (Exception e) {
            DialogManager.showError("Error", e.getMessage());
            AppLogger.error("Monthly_Expense_Page_Error", e);
        }
    }

    private void showJammatSummary() {
        Double balance = expenseService.getJammatOutstandingBalance();
        NotificationManager.show("Total Jammat Debt Balance: ₹" + balance, 
                               NotificationType.INFO, Pos.TOP_RIGHT);
    }

    public void refreshTable() {
        tblExpenses.getItems().setAll(expenseService.getExpensesForActiveMonth());
    }

    private void clearForm() {
        cmbType.setValue(null);
        cmbCategory.setValue(null);;
        txtAmount.clear();
        txtRemarks.clear();
    }
}