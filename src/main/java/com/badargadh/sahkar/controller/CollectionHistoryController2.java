package com.badargadh.sahkar.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.badargadh.sahkar.data.EmiPayment;
import com.badargadh.sahkar.data.EmiPaymentGroup;
import com.badargadh.sahkar.data.FeePayment;
import com.badargadh.sahkar.data.FinancialMonth;
import com.badargadh.sahkar.data.Member;
import com.badargadh.sahkar.data.MonthlyPayment;
import com.badargadh.sahkar.dto.MonthlyPaymentCollectionDTO;
import com.badargadh.sahkar.enums.CollectionLocation;
import com.badargadh.sahkar.enums.FeeType;
import com.badargadh.sahkar.repository.EmiPaymentRepository;
import com.badargadh.sahkar.repository.FeePaymentRepository;
import com.badargadh.sahkar.service.FinancialMonthService;
import com.badargadh.sahkar.service.PaymentCollectionService;
import com.badargadh.sahkar.service.ReceiptPrintingService;
import com.badargadh.sahkar.util.NotificationManager;
import com.badargadh.sahkar.util.NotificationManager.NotificationType;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

@Component
public class CollectionHistoryController2 {

    @FXML private ComboBox<CollectionLocation> cmbCollectionLocation;
    @FXML private TextField txtSearchHistory;
    @FXML private Label lblGrandTotal, lblTotalPayments;
    @FXML private Label lblTotalFees, lblTotalEmi, lblJoining, lblMumbaiCollection, lblBadargadhCollection;

    @FXML private TableView<MonthlyPaymentCollectionDTO> tblHistory;
    @FXML private TableColumn<MonthlyPaymentCollectionDTO, String> colDate;
    @FXML private TableColumn<MonthlyPaymentCollectionDTO, Integer> colMemberNo;
    @FXML private TableColumn<MonthlyPaymentCollectionDTO, String> colMember;
    @FXML private TableColumn<MonthlyPaymentCollectionDTO, Double> colFees, colEmi, colJoiningFees;
    @FXML private TableColumn<MonthlyPaymentCollectionDTO, Void> colAction;
    @FXML private TableColumn<MonthlyPaymentCollectionDTO, String> colTotal, colLocation;

    @Autowired private ReceiptPrintingService printingService;
    @Autowired private FeePaymentRepository feeRepo;
    @Autowired private FinancialMonthService monthService;
    @Autowired private EmiPaymentRepository emiRepo; // Renamed for clarity
    @Autowired private PaymentCollectionService paymentCollectionService;

    private final ObservableList<MonthlyPaymentCollectionDTO> masterHistoryData = FXCollections.observableArrayList();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private FinancialMonth currentMonth;

    @FXML
    public void initialize() {
        // Cell Value Factories
        colDate.setCellValueFactory(t -> new SimpleStringProperty(
                t.getValue().getLatestTransactionDate() != null ? formatter.format(t.getValue().getLatestTransactionDate()) : ""
        ));
        colMemberNo.setCellValueFactory(new PropertyValueFactory<>("memberNo"));
        colMember.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colFees.setCellValueFactory(new PropertyValueFactory<>("monthlyFeePaid"));
        colEmi.setCellValueFactory(new PropertyValueFactory<>("emiPaid"));
        colJoiningFees.setCellValueFactory(new PropertyValueFactory<>("joiningFeePaid"));
        
        // Calculated Total Column
        colTotal.setCellValueFactory(d -> {
            double total = (d.getValue().getMonthlyFeePaid() != null ? d.getValue().getMonthlyFeePaid() : 0) +
                           (d.getValue().getEmiPaid() != null ? d.getValue().getEmiPaid() : 0) +
                           (d.getValue().getJoiningFeePaid() != null ? d.getValue().getJoiningFeePaid() : 0);
            return new SimpleStringProperty(String.format("%.0f", total));
        });

        colLocation.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getCollectionLocation() != null ? d.getValue().getCollectionLocation().name() : ""
        ));

        setupActionColumn();
        setupSearchAndFilters();
    }

    private void setupActionColumn() {
        colAction.setCellFactory(column -> new TableCell<>() {
            private final FontAwesomeIconView iconView = new FontAwesomeIconView(FontAwesomeIcon.EYE);
            private final Button btnView = new Button("", iconView);
            {
                btnView.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0;");
                iconView.setFill(javafx.scene.paint.Color.web("#3498db"));
                iconView.setGlyphSize(18);
                btnView.setOnAction(event -> {
                    MonthlyPaymentCollectionDTO dto = getTableView().getItems().get(getIndex());
                    if (dto.getPaymentGroupId() != null) {
                        showGroupDetailsDialog(dto.getPaymentGroupId());
                    } else {
                        NotificationManager.show("Direct payment record (No Group ID).", NotificationType.ERROR, Pos.CENTER);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnView);
                setAlignment(Pos.CENTER);
            }
        });
    }

    public void loadHistory() {
        monthService.getActiveMonth().ifPresent(active -> {
            this.currentMonth = active;

            // 1. Fetch data from both repositories
            List<FeePayment> fees = feeRepo.findByFinancialMonth(active);
            List<EmiPayment> emis = emiRepo.findByFinancialMonth(active); // Assuming this method exists

            // 2. Merge Logic using Map
            Map<Long, MonthlyPaymentCollectionDTO> reportMap = new HashMap<>();

            for (FeePayment f : fees) {
                MonthlyPaymentCollectionDTO dto = reportMap.computeIfAbsent(f.getMember().getId(), id -> createBaseDTO(f.getMember()));
                updateDTOWithFee(dto, f);
            }

            for (EmiPayment e : emis) {
                MonthlyPaymentCollectionDTO dto = reportMap.computeIfAbsent(e.getMember().getId(), id -> createBaseDTO(e.getMember()));
                updateDTOWithEmi(dto, e);
            }

            List<MonthlyPaymentCollectionDTO> history = new ArrayList<>(reportMap.values());

            Platform.runLater(() -> {
                masterHistoryData.setAll(history);
                lblTotalPayments.setText(String.valueOf(history.size()));
                updateFooterTotals(masterHistoryData);
            });
        });
    }

    private MonthlyPaymentCollectionDTO createBaseDTO(Member m) {
        MonthlyPaymentCollectionDTO dto = new MonthlyPaymentCollectionDTO();
        dto.setMemberNo(m.getMemberNo());
        dto.setFullName(m.getFullname());
        dto.setMonthlyFeePaid(0.0);
        dto.setEmiPaid(0.0);
        dto.setJoiningFeePaid(0.0);
        return dto;
    }

    private void updateDTOWithFee(MonthlyPaymentCollectionDTO dto, FeePayment f) {
        if (f.getFeeType() == FeeType.MONTHLY_FEE) dto.setMonthlyFeePaid(f.getAmount());
        else if (f.getFeeType() == FeeType.JOINING_FEE) dto.setJoiningFeePaid(f.getAmount());
        
        dto.setLatestTransactionDate(maxDate(dto.getLatestTransactionDate(), f.getTransactionDateTime()));
        dto.setCollectionLocation(f.getCollectionLocation());
        if (f.getPaymentGroup() != null) dto.setPaymentGroupId(f.getPaymentGroup().getId());
    }

    private void updateDTOWithEmi(MonthlyPaymentCollectionDTO dto, EmiPayment e) {
        dto.setEmiPaid(e.getAmountPaid());
        dto.setLatestTransactionDate(maxDate(dto.getLatestTransactionDate(), e.getPaymentDateTime()));
        dto.setCollectionLocation(e.getCollectionLocation());
        if (e.getPaymentGroup() != null) dto.setPaymentGroupId(e.getPaymentGroup().getId());
    }

    private LocalDateTime maxDate(LocalDateTime d1, LocalDateTime d2) {
        if (d1 == null) return d2;
        if (d2 == null) return d1;
        return d1.isAfter(d2) ? d1 : d2;
    }

    private void setupSearchAndFilters() {
        FilteredList<MonthlyPaymentCollectionDTO> filteredData = new FilteredList<>(masterHistoryData, p -> true);

        cmbCollectionLocation.setItems(FXCollections.observableArrayList(CollectionLocation.values()));

        txtSearchHistory.textProperty().addListener((obs, old, newValue) -> {
            applyFilters(filteredData);
        });

        cmbCollectionLocation.valueProperty().addListener((obs, old, newValue) -> {
            applyFilters(filteredData);
        });

        SortedList<MonthlyPaymentCollectionDTO> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tblHistory.comparatorProperty());
        tblHistory.setItems(sortedData);
    }

    private void applyFilters(FilteredList<MonthlyPaymentCollectionDTO> filteredData) {
        String searchText = txtSearchHistory.getText() == null ? "" : txtSearchHistory.getText().toLowerCase();
        CollectionLocation location = cmbCollectionLocation.getValue();

        filteredData.setPredicate(record -> {
            boolean matchesSearch = searchText.isEmpty() || 
                                    record.getFullName().toLowerCase().contains(searchText) || 
                                    String.valueOf(record.getMemberNo()).contains(searchText);
            
            boolean matchesLocation = (location == null) || (record.getCollectionLocation() == location);

            return matchesSearch && matchesLocation;
        });
        updateFooterTotals(filteredData);
    }

    private void updateFooterTotals(List<MonthlyPaymentCollectionDTO> data) {
        double fees = data.stream().mapToDouble(m -> m.getMonthlyFeePaid() != null ? m.getMonthlyFeePaid() : 0).sum();
        double emi = data.stream().mapToDouble(m -> m.getEmiPaid() != null ? m.getEmiPaid() : 0).sum();
        double joining = data.stream().mapToDouble(m -> m.getJoiningFeePaid() != null ? m.getJoiningFeePaid() : 0).sum();

        double mumbai = getCollectionLocationTotal(data, CollectionLocation.MUMBAI);
        double badargadh = getCollectionLocationTotal(data, CollectionLocation.BADARGADH);

        lblTotalFees.setText(String.format("₹ %,.0f", fees));
        lblTotalEmi.setText(String.format("₹ %,.0f", emi));
        lblJoining.setText(String.format("₹ %,.0f", joining));
        lblMumbaiCollection.setText(String.format("₹ %,.0f", mumbai));
        lblBadargadhCollection.setText(String.format("₹ %,.0f", badargadh));
        lblGrandTotal.setText(String.format("₹ %,.0f", fees + emi + joining));
    }

    private double getCollectionLocationTotal(List<MonthlyPaymentCollectionDTO> data, CollectionLocation loc) {
        return data.stream()
                .filter(d -> d.getCollectionLocation() == loc)
                .mapToDouble(m -> (m.getMonthlyFeePaid() != null ? m.getMonthlyFeePaid() : 0) +
                                  (m.getEmiPaid() != null ? m.getEmiPaid() : 0) +
                                  (m.getJoiningFeePaid() != null ? m.getJoiningFeePaid() : 0))
                .sum();
    }

    private void showGroupDetailsDialog(Long groupId) {
        EmiPaymentGroup group = paymentCollectionService.getEmiPaymentGroupByGroupId(groupId);
        group.setMonthlyPayments(paymentCollectionService.getMonthlyPaymentsByGroup(groupId));
        
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Batch Details #" + groupId);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        TableView<MonthlyPayment> detailTable = new TableView<>(FXCollections.observableArrayList(group.getMonthlyPayments()));
        
        TableColumn<MonthlyPayment, String> colMNo = new TableColumn<>("No");
        colMNo.setCellValueFactory(new PropertyValueFactory<>("memberNo"));
        
        TableColumn<MonthlyPayment, String> colMName = new TableColumn<>("Name");
        colMName.setCellValueFactory(new PropertyValueFactory<>("name"));
        
        TableColumn<MonthlyPayment, Double> colMFee = new TableColumn<>("Fees");
        colMFee.setCellValueFactory(new PropertyValueFactory<>("monthlyFees"));
        
        TableColumn<MonthlyPayment, Double> colMEmi = new TableColumn<>("EMI");
        colMEmi.setCellValueFactory(new PropertyValueFactory<>("emiAmount"));

        detailTable.getColumns().addAll(colMNo, colMName, colMFee, colMEmi);

        Button btnPrint = new Button("🖨️ Print Duplicate");
        btnPrint.setOnAction(e -> printingService.printCollectionReceipt(group, true));
        
        double total = group.getMonthlyPayments()
        		.stream()
        		.mapToDouble(p -> (p.getEmiAmount() != null ? p.getEmiAmount() : 0) + p.getMonthlyFees())
        		.sum();
        
        Label Label = new Label("Total : " + total);

        VBox layout = new VBox(10, new Label("Depositor: " + group.getDepositorName()), detailTable, Label, btnPrint);
        layout.setPadding(new Insets(15));
        dialog.getDialogPane().setContent(layout);
        dialog.showAndWait();
    }

    @FXML private void refreshHistory() {
        cmbCollectionLocation.setValue(null);
        txtSearchHistory.clear();
        loadHistory();
    }
}