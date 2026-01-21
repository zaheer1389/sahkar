package com.badargadh.sahkar.util;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.badargadh.sahkar.data.AppConfig;
import com.badargadh.sahkar.data.FinancialMonth;
import com.badargadh.sahkar.enums.MonthStatus;
import com.badargadh.sahkar.exception.BusinessException;
import com.badargadh.sahkar.service.FinancialMonthService;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;

/**
 * Industry-standard Access Guard to enforce financial period constraints
 * on JavaFX View components.
 */
@Component
public class FinancialAccessGuard {

    @Autowired
    private FinancialMonthService monthService;

    /**
     * Enforces a strict lock on a UI container. 
     * If no month is open, the entire 'root' is disabled.
     */
    public void enforceStrictLock(Node root, Label warningLabel) {
        boolean isMonthOpen = monthService.getActiveMonth().isPresent();
        
        Platform.runLater(() -> {
            root.setDisable(!isMonthOpen);
            
            if (!isMonthOpen && warningLabel != null) {
                warningLabel.setText("System Locked: Opening a Financial Month is required for this operation.");
                warningLabel.setStyle("-fx-text-fill: #c0392b; -fx-font-weight: bold; -fx-background-color: #f8d7da; -fx-padding: 5;");
            } else if (warningLabel != null) {
                warningLabel.setText(""); // Clear warning if open
            }
        });
    }

    /**
     * Enforces a partial lock (e.g., for Member page).
     * Keeps the list viewable but disables action buttons.
     */
    public void enforcePartialLock(Label warningLabel, Node... actionNodes) {
        boolean isMonthOpen = monthService.getActiveMonth().isPresent();
        
        Platform.runLater(() -> {
            for (Node node : actionNodes) {
                node.setDisable(!isMonthOpen);
            }
            
            if (!isMonthOpen && warningLabel != null) {
                warningLabel.setText("Modifications disabled. Please open a financial month.");
            }
        });
    }
    
    public boolean isValidDateForFinancialMonth() {
    	// 1. Get the official OPEN period from DB
        FinancialMonth activeMonth = monthService.getActiveMonth()
            .orElseThrow(() -> new BusinessException("No Financial Month is currently OPEN. Transactions blocked."));

        LocalDate pcDate = LocalDate.now();

        // 2. Guard: PC Date must be within the Open Month's Range
        if (pcDate.isBefore(activeMonth.getStartDate()) || pcDate.isAfter(activeMonth.getEndDate())) {
            throw new BusinessException("System Date Error!\n" +
                "The PC date (" + pcDate + ") is outside the current OPEN period: " +
                activeMonth.getMonthName() + " (" + activeMonth.getStartDate() + " to " + activeMonth.getEndDate() + ")");
        }
        
		return true;
    }
}