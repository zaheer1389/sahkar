package com.badargadh.sahkar.controller;

import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.management.Notification;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.scene.control.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.badargadh.sahkar.data.EmiPayment;
import com.badargadh.sahkar.data.EmiPaymentGroup;
import com.badargadh.sahkar.data.FeePayment;
import com.badargadh.sahkar.data.FinancialMonth;
import com.badargadh.sahkar.data.MonthlyPayment;
import com.badargadh.sahkar.dto.EmiCounterDTO;
import com.badargadh.sahkar.dto.MonthlyPaymentCollectionDTO;
import com.badargadh.sahkar.enums.CollectionLocation;
import com.badargadh.sahkar.repository.EmiPaymentGroupRepository;
import com.badargadh.sahkar.repository.FeePaymentRepository;
import com.badargadh.sahkar.repository.MemberRepository;
import com.badargadh.sahkar.service.FinancialMonthService;
import com.badargadh.sahkar.service.PaymentCollectionService;
import com.badargadh.sahkar.service.ReceiptPrintingService;
import com.badargadh.sahkar.util.AppLogger;
import com.badargadh.sahkar.util.NotificationManager;
import com.badargadh.sahkar.util.NotificationManager.NotificationType;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

@Component
public class CollectionHistoryController {
	
	@FXML private ComboBox<CollectionLocation> cmbCollectionLocation;
	@FXML private TextField txtSearchHistory;
	@FXML private Label lblGrandTotal, lblTotalPayments; // New label for combined total
	@FXML private Label lblFeesCount, lbl100EmiCount, lbl200EmiCount, lbl300EmiCount, lbl400EmiCount;
	
    @FXML private TableView<MonthlyPaymentCollectionDTO> tblHistory;
    @FXML private TableColumn<MonthlyPaymentCollectionDTO, String> colDate;
    @FXML private TableColumn<MonthlyPaymentCollectionDTO, Integer> colMemberNo;
    @FXML private TableColumn<MonthlyPaymentCollectionDTO, String> colMember;
    @FXML private TableColumn<MonthlyPaymentCollectionDTO, Integer> colFees, colEmi, colJoiningFees;
    @FXML private TableColumn<MonthlyPaymentCollectionDTO, Void> colAction;
    @FXML private TableColumn<MonthlyPaymentCollectionDTO, String> colTotal, colLocation;
    
    @FXML private Label lblTotalFees, lblTotalEmi, lblJoining, lblDeductions, lblMumbaiCollection, lblBadargadhCollection;

    @Autowired private ReceiptPrintingService printingService;
    @Autowired private MemberRepository memberRepo;
    @Autowired private FeePaymentRepository feeRepo;
    @Autowired private FinancialMonthService monthService;
    @Autowired private EmiPaymentGroupRepository emiPaymentGroupRepository;
    @Autowired private PaymentCollectionService paymentCollectionService;
    
    private ObservableList<MonthlyPaymentCollectionDTO> masterHistoryData = FXCollections.observableArrayList();
    
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    
    FinancialMonth month;

    @FXML
    public void initialize() {
    	
    	colDate.setCellValueFactory(t -> new SimpleStringProperty(formatter.format(t.getValue().getLatestTransactionDate())));
        colMemberNo.setCellValueFactory(new PropertyValueFactory<>("memberNo"));
        colMember.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colFees.setCellValueFactory(new PropertyValueFactory<>("monthlyFeePaid"));
        colEmi.setCellValueFactory(new PropertyValueFactory<>("emiPaid"));
        colJoiningFees.setCellValueFactory(new PropertyValueFactory<>("joiningFeePaid"));
        colTotal.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTotalCollection()+""));
        colLocation.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCollectionLocation() != null ? d.getValue().getCollectionLocation().name() : ""));
        colAction.setCellFactory(column -> {
            return new TableCell<>() {
                private final FontAwesomeIconView iconView = new FontAwesomeIconView(FontAwesomeIcon.EYE);
                private final Button btnView = new Button("", iconView);
                {
                    String iconOnlyStyle = "-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0;";
                    btnView.setStyle(iconOnlyStyle);
                    btnView.setTooltip(new Tooltip("View Details"));
                    iconView.setFill(javafx.scene.paint.Color.web("#3498db")); // Primary Blue
                    iconView.setGlyphSize(18);
                	btnView.setOnAction(event -> {
                	    MonthlyPaymentCollectionDTO dto = getTableView().getItems().get(getIndex());
                	    Long groupId = dto.getPaymentGroupId(); 
                	    
                	    if (groupId != null) {
                	        showGroupDetailsDialog(groupId);
                	    } else {
                	        // Fallback for old records without a group
                	    	System.out.println("No Group ID found for this payment.");
                	        AppLogger.info("No Group ID found for this payment.");
                	        NotificationManager.show("No Group ID found for this payment.", NotificationType.ERROR, Pos.CENTER);
                	    }
                	});
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        setGraphic(btnView);
                        setAlignment(Pos.CENTER);
                    }
                }
            };
        });
        
        // Calculate Row Total (Fees + EMI)
        setupSearch();
        
    }

    public void loadHistory() {
        monthService.getActiveMonth().ifPresent(active -> {
        	
        	month = active;
        	
            // 1. Fetch data from DB
            List<MonthlyPaymentCollectionDTO> history = memberRepo.findReceivedPaymentsByMonthWithGroup(active.getId());
            
            // 2. CORRECT: Update the MASTER list, NOT the TableView directly
            masterHistoryData.setAll(history); 
            
            lblTotalPayments.setText(history.size()+"");
            
            EmiCounterDTO counterDTO = new EmiCounterDTO();
            
            history.forEach(dto -> {
                if (dto.getEmiPaid() == 0) {
                	counterDTO.setFeesCount(counterDTO.getFeesCount() + 1);
                	return;
                };

                switch (dto.getEmiPaid().intValue()) {
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
            lbl400EmiCount.setText(counterDTO.getEmi400()+"");
            */
            // 3. Update footer labels based on the new data
            updateFooterTotals(masterHistoryData);
        });
    }
    
    private void setupSearch() {
        FilteredList<MonthlyPaymentCollectionDTO> filteredData = new FilteredList<>(masterHistoryData, p -> true);

        cmbCollectionLocation.setItems(FXCollections.observableArrayList(CollectionLocation.values()));
        
        txtSearchHistory.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(record -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();

                // Search by Member Name or Date string
                if (record.getFullName().toLowerCase().contains(lowerCaseFilter)) return true;
                //if (record.getDate().toString().contains(lowerCaseFilter)) return true;
                if (String.valueOf(record.getMemberNo()).contains(lowerCaseFilter)) return true;
                return false;
            });
            updateFooterTotals(filteredData); // Update totals as user filters
        });
        
        cmbCollectionLocation.valueProperty().addListener((observable, oldValue, newValue) -> {
        	filteredData.setPredicate(record -> {
        		if (newValue == null) return true;        		
        		return record.getCollectionLocation() == newValue;
        	});    
        	
        	updateFooterTotals(filteredData); // Update totals as user filters
        });

        SortedList<MonthlyPaymentCollectionDTO> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tblHistory.comparatorProperty());
        tblHistory.setItems(sortedData);
    }

    private void updateFooterTotals(List<MonthlyPaymentCollectionDTO> data) {
    	double fees = data.stream()
                .mapToDouble(m -> m.getMonthlyFeePaid() != null ? m.getMonthlyFeePaid() : 0)
                .sum();

        double emi = data.stream()
                .mapToDouble(m -> m.getEmiPaid() != null ? m.getEmiPaid() : 0)
                .sum();
        
        double joining = data.stream()
                .mapToDouble(m -> m.getJoiningFeePaid() != null ? m.getJoiningFeePaid() : 0)
                .sum();
        
        double mumbaiCollection = getCollectionLocationTotal(data, CollectionLocation.MUMBAI);
        double badargadhCollection = getCollectionLocationTotal(data, CollectionLocation.BADARGADH);

        double grandTotal = fees + emi + joining ;
        
        // Formatting the Labels
        lblTotalFees.setText(String.format("₹ %,.0f", fees));
        lblTotalEmi.setText(String.format("₹ %,.0f", emi));
        lblJoining.setText(String.format("₹ %,.0f", joining));	
        
        lblMumbaiCollection.setText(String.format("₹ %,.0f", mumbaiCollection));
        lblBadargadhCollection.setText(String.format("₹ %,.0f", badargadhCollection));
        
        lblGrandTotal.setText(String.format("₹ %,.0f", grandTotal));
    }
    
    private double getCollectionLocationTotal(List<MonthlyPaymentCollectionDTO> data, CollectionLocation collectionLocation) {
    	return data.stream().filter(d -> d.getCollectionLocation() != null && d.getCollectionLocation() == collectionLocation)
				.mapToDouble(m -> {
					double total = 0;
					total = (m.getMonthlyFeePaid() != null ? m.getMonthlyFeePaid() : 0) +
							(m.getEmiPaid() != null ? m.getEmiPaid() : 0) +
							(m.getJoiningFeePaid() != null ? m.getJoiningFeePaid() : 0);		
					return total;
				})
		        .sum();
    }
    
    @FXML
    private void refreshHistory() {
    	cmbCollectionLocation.setValue(null);
    	txtSearchHistory.clear();
    	loadHistory();
    }
    
    private void showGroupDetailsDialog(Long groupId) {
        // 1. Fetch the Group and its payments from Service
        EmiPaymentGroup group = paymentCollectionService.getEmiPaymentGroupByGroupId(groupId);
        group.setMonthlyPayments(paymentCollectionService.getMonthlyPaymentsByGroup(groupId));
        
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Collection Batch Details - #" + groupId);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        // 2. Create the Table for the Dialog
        TableView<MonthlyPayment> detailTable = new TableView<>();
        
        TableColumn<MonthlyPayment, String> colMemberNo= new TableColumn<>("Member No");
        colMemberNo.setCellValueFactory(new PropertyValueFactory<>("memberNo"));
        
        TableColumn<MonthlyPayment, String> colName = new TableColumn<>("Member Name");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        
        TableColumn<MonthlyPayment, Double> colAmount = new TableColumn<>("Amount");
        colAmount.setCellValueFactory(new PropertyValueFactory<>("emiAmount"));
        
        TableColumn<MonthlyPayment, String> colFee = new TableColumn<>("Fees");
        colFee.setCellValueFactory(new PropertyValueFactory<>("monthlyFees")); // "EMI" or "FEE"
        
        TableColumn<MonthlyPayment, String> colFullpayment = new TableColumn<>("Full Payment");
        colFullpayment.setCellValueFactory(new PropertyValueFactory<>("fullAmount"));

        detailTable.getColumns().addAll(colMemberNo, colName, colFee, colAmount, colFullpayment);
        
        // Combine EMIs and Fees into a generic list for the table
        ObservableList<MonthlyPayment> data = FXCollections.observableArrayList();
        data.addAll(group.getMonthlyPayments());
        
        detailTable.setItems(data);

        // 3. Print Button
        Button btnPrintBatch = new Button("🖨️ Re-Print Receipt");
        btnPrintBatch.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        btnPrintBatch.setOnAction(e -> {
        	printingService.printCollectionReceipt(group, true); // true for duplicate copy
        });

        // 4. Layout
        VBox container = new VBox(15, 
            new Label("Batch Depositor: " + group.getDepositorName()),
            detailTable, 
            btnPrintBatch
        );
        container.setPadding(new Insets(20));
        container.setPrefSize(800, 400);
        
        dialog.getDialogPane().setContent(container);
        dialog.showAndWait();
    }
    
    public static class GenericPaymentDTO {
        private final String memberName;
        private final Double amount;
        private final String type;

        public GenericPaymentDTO(EmiPayment emi) {
            this.memberName = emi.getMember().getFullname();
            this.amount = emi.getAmountPaid();
            this.type = "LOAN EMI";
        }

        public GenericPaymentDTO(FeePayment fee) {
            this.memberName = fee.getMember().getFullname();
            this.amount = fee.getAmount();
            this.type = "FEE";
        }

		public String getMemberName() {
			return memberName;
		}

		public Double getAmount() {
			return amount;
		}

		public String getType() {
			return type;
		}
        
        
    }
}