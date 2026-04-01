package com.badargadh.sahkar.controller;

import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

import javafx.application.Platform;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.badargadh.sahkar.data.AppConfig;
import com.badargadh.sahkar.data.FinancialMonth;
import com.badargadh.sahkar.dto.EmiCounterDTO;
import com.badargadh.sahkar.dto.PendingMonthlyCollectionDTO;
import com.badargadh.sahkar.repository.MemberRepository;
import com.badargadh.sahkar.service.AppConfigService;
import com.badargadh.sahkar.service.FinancialMonthService;
import com.badargadh.sahkar.service.report.JasperReportService;
import com.badargadh.sahkar.util.FileUtil;
import com.badargadh.sahkar.util.NotificationManager;
import com.badargadh.sahkar.util.NotificationManager.NotificationType;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

@Component
public class CollectionPendingController2 implements Initializable {
    
    @FXML private TextField txtSearchPending;
    @FXML private TableView<PendingMonthlyCollectionDTO> tblPending;
    @FXML private TableColumn<PendingMonthlyCollectionDTO, Integer> colMemberNo;
    @FXML private TableColumn<PendingMonthlyCollectionDTO, String> colName;
    @FXML private TableColumn<PendingMonthlyCollectionDTO, String> colFeesAmoount, colEmiAmount, colBalAmount;
    @FXML private Label lblTotalFees, lblTotalEmi, lblGrandTotal, lblTotalPendingPayments;
    
    @Autowired private MainController mainController;
    @Autowired private MemberRepository memberRepo;
    @Autowired private FinancialMonthService monthService;
    @Autowired private AppConfigService appConfigService;
    @Autowired private JasperReportService jasperReportService;
    
    private AppConfig appConfig;
    private FinancialMonth month;
    private ObservableList<PendingMonthlyCollectionDTO> masterPendingData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        appConfig = appConfigService.getSettings();
        
        colMemberNo.setCellValueFactory(new PropertyValueFactory<>("memberNo"));
        colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFullName()));
        
        // LOGIC: If Loan Pending Balance < Standard EMI, show Pending Balance as EMI
        colEmiAmount.setCellValueFactory(data -> {
            double emiDue = data.getValue().getEmiAmountDue() != null ? data.getValue().getEmiAmountDue() : 0.0;
            double loanBalance = data.getValue().getLoanPendingAmount() != null ? data.getValue().getLoanPendingAmount() : 0.0;
            
            // Final month logic: Take the smaller of the two
            double actualEmi = (loanBalance > 0 && loanBalance < emiDue) ? loanBalance : emiDue;
            return new SimpleStringProperty(String.format("%.0f", actualEmi));
        });
        
        colBalAmount.setCellValueFactory(data -> new SimpleStringProperty(String.format("%.0f", data.getValue().getLoanPendingAmount() != null ? data.getValue().getLoanPendingAmount() : 0.0)));

        colFeesAmoount.setCellValueFactory(data -> new SimpleStringProperty(String.format("%.0f", appConfig.getMonthlyFees().doubleValue())));
        
        setupSearch();
    }
    
    @FXML
    public void refreshPendingList() {
        monthService.getActiveMonth().ifPresent(active -> {
            month = active;
            // Fetching active members who haven't paid for this month yet
            List<PendingMonthlyCollectionDTO> pending = memberRepo.findActiveMembersPendingForMonth1(active.getId());
            
            masterPendingData.setAll(pending);
            
            // 1. Calculate Total Fees (Standard for all members in list)
            double totalFees = (long) pending.size() * appConfig.getMonthlyFees();
            
            // 2. Calculate Total EMI using the "Lower of Balance or EMI" logic
            double totalEmi = pending.stream()
                    .mapToDouble(m -> {
                        double emiDue = m.getEmiAmountDue() != null ? m.getEmiAmountDue() : 0.0;
                        double loanBalance = m.getLoanPendingAmount() != null ? m.getLoanPendingAmount() : 0.0;
                        // Use balance if it's the final payment (less than standard EMI)
                        return (loanBalance > 0 && loanBalance < emiDue) ? loanBalance : emiDue;
                    })
                    .sum();

            Platform.runLater(() -> {
                lblTotalFees.setText(String.format("₹ %,.0f", totalFees));
                lblTotalEmi.setText(String.format("₹ %,.0f", totalEmi));
                lblGrandTotal.setText(String.format("₹ %,.0f", (totalEmi + totalFees)));
                lblTotalPendingPayments.setText(String.valueOf(pending.size()));
                txtSearchPending.clear();
            });
        });
    }

    private void setupSearch() {
        FilteredList<PendingMonthlyCollectionDTO> filteredData = new FilteredList<>(masterPendingData, p -> true);

        txtSearchPending.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(member -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();
                return member.getFullName().toLowerCase().contains(lowerCaseFilter) || 
                       String.valueOf(member.getMemberNo()).contains(lowerCaseFilter);
            });
        });

        SortedList<PendingMonthlyCollectionDTO> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tblPending.comparatorProperty());
        tblPending.setItems(sortedData);
    }
    
    @FXML
    private void handleExportPdf() {
        if (masterPendingData.isEmpty() || month == null) {
            NotificationManager.show("No data available to export", NotificationType.WARNING, Pos.CENTER);
            return;
        }
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "Pending_Collection_Report_" + timestamp + ".pdf";
        File targetFile = FileUtil.getReportOutputFile(fileName);

        if (targetFile != null) {
            try {
                // Ensure the PDF uses the same list seen on screen
                jasperReportService.generatePendingCollectionReport(masterPendingData, month.getMonthId(), targetFile.getAbsolutePath());
                NotificationManager.show("Report Saved Successfully", NotificationType.SUCCESS, Pos.CENTER);
                mainController.showReport(targetFile);
            } catch (Exception e) {
                NotificationManager.show("Export Error: " + e.getMessage(), NotificationType.ERROR, Pos.CENTER);
            }
        }
    }

}