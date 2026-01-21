package com.badargadh.sahkar.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.badargadh.sahkar.enums.CollectionLocation;

import javafx.fxml.FXML;
import javafx.scene.control.TabPane;

@Component
public class CollectionMainController {
	
    @FXML private TabPane collectionTabPane;
    
    // Spring automatically injects the controllers of the included FXMLs
    @Autowired private CollectionPendingController pendingListController;
    @Autowired private CollectionHistoryController historyReportController;
    @Autowired private MonthlyStatementController statementController;
    @Autowired private FeesRefundController feesRefundController;
    @Autowired private MonthlyExpenseController monthlyExpenseController;
    @Autowired private CollectionController collectionController;

    @FXML
    public void initialize() {
        collectionTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab.getText().equals("Pending Payments")) {
                pendingListController.refreshPendingList();
            } else if (newTab.getText().equals("Mumbai Collection")) {
            	//collectionController.setCollectionLocation(CollectionLocation.MUMBAI);
            } else if (newTab.getText().equals("Received History")) {
                historyReportController.loadHistory();
            } else if (newTab.getText().equals("Monthly Expenses")) {
            	monthlyExpenseController.refreshTable();
            } else if (newTab.getText().equals("Member Fees Refunds")) {
            	feesRefundController.refreshTable();
            } else if (newTab.getText().equals("Monthly Statement")) {
            	statementController.refresh();
            }
            
        });
        
        
    }
}