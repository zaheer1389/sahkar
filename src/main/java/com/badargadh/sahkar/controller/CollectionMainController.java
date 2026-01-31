package com.badargadh.sahkar.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.TabPane;
import javafx.scene.layout.VBox;

@Component
public class CollectionMainController {

    @FXML private TabPane collectionTabPane;

    // Reference to the global blocker from your MainController
    @Autowired private MainController mainController;

    @Autowired private CollectionPendingController pendingListController;
    @Autowired private CollectionHistoryController historyReportController;
    @Autowired private MonthlyStatementController statementController;
    @Autowired private FeesRefundController feesRefundController;
    @Autowired private MonthlyExpenseController monthlyExpenseController;

    @FXML
    public void initialize() {
        collectionTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                loadTabData(newTab.getText());
            }
        });
    }

    private void loadTabData(String tabTitle) {
        // 1. Show loader on the UI thread immediately
        mainController.showLoader("Fetching " + tabTitle + "...");

        Task<Void> loadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // 2. DATABASE WORK (Background Thread)
                // We invoke the logic here.
                // NOTE: If these methods internally try to update a TableView,
                // they will still throw the "Not on FX Thread" error.
                // You should ideally have "fetchData()" methods separate from "updateUI()".

                switch (tabTitle) {
                    case "Pending Payments" -> pendingListController.refreshPendingList();
                    case "Received History" -> historyReportController.loadHistory();
                    case "Monthly Expenses" -> monthlyExpenseController.refreshTable();
                    case "Member Fees Refunds" -> feesRefundController.refreshTable();
                    case "Monthly Statement" -> statementController.refresh();
                }
                return null;
            }
        };

        loadTask.setOnSucceeded(e -> {
            // 3. HIDE LOADER (Back on FX Thread)
            mainController.hideLoader();
        });

        loadTask.setOnFailed(e -> {
            mainController.hideLoader();
            Throwable ex = e.getSource().getException();
            ex.printStackTrace();
            // Log the error so you can see which specific sub-controller failed
        });

        // Start the background thread
        new Thread(loadTask).start();
    }
}