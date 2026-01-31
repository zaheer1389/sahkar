package com.badargadh.sahkar.controller;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javafx.application.Platform;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.badargadh.sahkar.data.AppConfig;
import com.badargadh.sahkar.data.FinancialMonth;
import com.badargadh.sahkar.dto.EmiCounterDTO;
import com.badargadh.sahkar.dto.MemberSummaryDTO;
import com.badargadh.sahkar.dto.PendingMonthlyCollectionDTO;
import com.badargadh.sahkar.repository.MemberRepository;
import com.badargadh.sahkar.service.AppConfigService;
import com.badargadh.sahkar.service.FinancialMonthService;
import com.badargadh.sahkar.service.MemberService;
import com.badargadh.sahkar.service.report.JasperReportService;
import com.badargadh.sahkar.service.report.ReportService;
import com.badargadh.sahkar.util.DialogManager;
import com.badargadh.sahkar.util.FileUtil;
import com.badargadh.sahkar.util.NotificationManager;
import com.badargadh.sahkar.util.NotificationManager.NotificationType;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

@Component
public class CollectionPendingController {
	
	@FXML private TextField txtSearchPending;
	@FXML private TableView<PendingMonthlyCollectionDTO> tblPending;
    @FXML private TableColumn<PendingMonthlyCollectionDTO, Integer> colMemberNo;
    @FXML private TableColumn<PendingMonthlyCollectionDTO, String> colName;
    @FXML private TableColumn<PendingMonthlyCollectionDTO, String> colFeesAmoount, colEmiAmount;
    @FXML private Label lblTotalFees, lblTotalEmi, lblGrandTotal, lblTotalPendingPayments;
    @FXML private Label lblFeesCount, lbl100EmiCount, lbl200EmiCount, lbl300EmiCount, lbl400EmiCount;
    
    @Autowired private MainController mainController;
    @Autowired private MemberRepository memberRepo;
    @Autowired private FinancialMonthService monthService;
    @Autowired private AppConfigService appConfigService;
    @Autowired private JasperReportService jasperReportService;
    
    private AppConfig appConfig;
    private FinancialMonth month;
    private ObservableList<PendingMonthlyCollectionDTO> masterPendingData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
    	
    	appConfig = appConfigService.getSettings();
    	
        colMemberNo.setCellValueFactory(new PropertyValueFactory<>("memberNo"));
        colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFullName()));
        colEmiAmount.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEmiAmountDue()+""));
        colFeesAmoount.setCellValueFactory(data -> new SimpleStringProperty(appConfig.getMonthlyFees()+""));
        
        setupSearch();
    }
    
    private void setupSearch() {
        // Wrap the observable list in a FilteredList
        FilteredList<PendingMonthlyCollectionDTO> filteredData = new FilteredList<>(masterPendingData, p -> true);

        // Set the filter Predicate whenever the search text changes
        txtSearchPending.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(member -> {
                if (newValue == null || newValue.isEmpty()) return true;

                String lowerCaseFilter = newValue.toLowerCase();

                // Search by Name or Member Number
                if (member.getFullName().toLowerCase().contains(lowerCaseFilter)) return true;
                if (String.valueOf(member.getMemberNo()).contains(lowerCaseFilter)) return true;
                
                return false;
            });
        });

        // Wrap the FilteredList in a SortedList so users can still sort columns
        SortedList<PendingMonthlyCollectionDTO> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tblPending.comparatorProperty());
        
        tblPending.setItems(sortedData);
    }
    
    @FXML
    public void refreshPendingList() {
    	monthService.getActiveMonth().ifPresent(active -> {
            month = active;
            List<PendingMonthlyCollectionDTO> pending = memberRepo.findActiveMembersPendingForMonth1(active.getId());
            
            // Update the master list which automatically updates the filtered list
            masterPendingData.setAll(pending);
            
            EmiCounterDTO counterDTO = new EmiCounterDTO();
            
            pending.forEach(dto -> {
                if (dto.getEmiAmountDue() == 0) {
                	counterDTO.setFeesCount(counterDTO.getFeesCount() + 1);
                	return;
                };
                
                Double emiAmount = dto.getLoanPendingAmount() < dto.getEmiAmountDue() ? dto.getLoanPendingAmount() : dto.getEmiAmountDue();

                switch (emiAmount.intValue()) {
                    case 100 -> counterDTO.setEmi100(counterDTO.getEmi100() + 1);
                    case 200 -> counterDTO.setEmi200(counterDTO.getEmi200() + 1);
                    case 300 -> counterDTO.setEmi300(counterDTO.getEmi300() + 1);
                    case 400 -> counterDTO.setEmi400(counterDTO.getEmi400() + 1);
                    // ignore other values safely
                }
            });

            /*lblFeesCount.setText(counterDTO.getFeesCount()+"");
            lbl100EmiCount.setText(counterDTO.getEmi100()+"");
            lbl200EmiCount.setText(counterDTO.getEmi200()+"");
            lbl300EmiCount.setText(counterDTO.getEmi300()+"");
            lbl400EmiCount.setText(counterDTO.getEmi400()+"");*/
            
            // Calculate Totals for Big Font Labels
            double totalFees = (long) pending.size() * appConfig.getMonthlyFees();
            double totalEmi = pending.stream()
                    .mapToDouble(m -> m.getEmiAmountDue() != null && m.getEmiAmountDue() < m.getLoanPendingAmount() ? m.getEmiAmountDue() : m.getLoanPendingAmount())
                    .sum();

            Platform.runLater(() -> {
                lblTotalFees.setText(String.format("₹ %,.0f", totalFees));
                lblTotalEmi.setText(String.format("₹ %,.0f", totalEmi));
                lblGrandTotal.setText(String.format("₹ %,.0f", (totalEmi + totalFees)));
                lblTotalPendingPayments.setText(pending.size()+"");
                txtSearchPending.clear();
            });
        });
    }
    
    @FXML
    private void handleExportPdf() {
        if (masterPendingData.isEmpty()) {
            NotificationManager.show("No data available to export", NotificationType.WARNING, Pos.CENTER);
            return;
        }
        
        if(month == null) {
        	return;
        }
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    	String fileName = "Monthly_Collection_Pending_Report_" + timestamp + ".pdf";
    	File targetFile = FileUtil.getReportOutputFile(fileName);

        if (targetFile != null) {
            try {
            	List<PendingMonthlyCollectionDTO> pending = memberRepo.findActiveMembersPendingForMonth1(month.getId());
            	jasperReportService.generatePendingCollectionReport(pending, month.getMonthId(), targetFile.getAbsolutePath());
                NotificationManager.show("Report Saved Successfully", NotificationType.SUCCESS, Pos.CENTER);
                mainController.showReport(targetFile);
            } catch (Exception e) {
                NotificationManager.show("Export Error: " + e.getMessage(), NotificationType.ERROR, Pos.CENTER);
            }
        }
    }
}