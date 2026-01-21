package com.badargadh.sahkar.controller;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.badargadh.sahkar.data.Member;
import com.badargadh.sahkar.data.PaymentRemark;
import com.badargadh.sahkar.repository.PaymentRemarkRepository;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

@Component
public class MemberRemarkController implements Initializable {

    @FXML private TableView<PaymentRemark> tblRemarks;
    @FXML private TableColumn<PaymentRemark, String> colMonth;
    @FXML private TableColumn<PaymentRemark, String> colIssued;
    @FXML private TableColumn<PaymentRemark, String> colType;
    @FXML private TableColumn<PaymentRemark, String> colStatus;
    @FXML private TableColumn<PaymentRemark, String> colClearedAction;
    @FXML private Label lblActiveCount;

    @Autowired
    private PaymentRemarkRepository remarkRepository;

    private final ObservableList<PaymentRemark> remarkData = FXCollections.observableArrayList();
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
    }

    /**
     * Call this method from the Member Profile Controller when a member is selected
     */
    public void loadMemberRemarks(Member member) {
        if (member == null) return;

        List<PaymentRemark> remarks = remarkRepository.getRemarksByMember(member);
        remarkData.setAll(remarks);
        tblRemarks.setItems(remarkData);

        // Update the Active Penalty Counter
        long activeCount = remarks.stream().filter(r -> !r.isCleared()).count();
        lblActiveCount.setText(String.valueOf(activeCount));
    }

    private void setupTableColumns() {
        // 1. Financial Month Column
        colMonth.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getFinancialMonth() != null ? data.getValue().getFinancialMonth().getMonthName() : "N/A"));

        // 2. Issued Timestamp Column
        colIssued.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getIssuedDate().format(dtf)));

        // 3. Penalty Type Column
        colType.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getRemarkType().name()));

        // 4. Status Column with Dynamic Badges
        colStatus.setCellFactory(column -> new TableCell<PaymentRemark, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    PaymentRemark remark = getTableRow().getItem();
                    Label badge = new Label();
                    
                    if (remark.isCleared()) {
                        badge.setText("CLEARED");
                        badge.setStyle("-fx-background-color: #d4edda; -fx-text-fill: #155724; " +
                                     "-fx-padding: 3 10; -fx-background-radius: 12; -fx-font-weight: bold;");
                    } else {
                        badge.setText("ACTIVE PENALTY");
                        badge.setStyle("-fx-background-color: #f8d7da; -fx-text-fill: #721c24; " +
                                     "-fx-padding: 3 10; -fx-background-radius: 12; -fx-font-weight: bold;");
                    }
                    setGraphic(badge);
                }
            }
        });

        // 5. Resolution Detail Column
        colClearedAction.setCellValueFactory(data -> {
            String action = data.getValue().getClearingReason();
            return new SimpleStringProperty(action != null ? action : "Pending Resolution");
        });
    }
}