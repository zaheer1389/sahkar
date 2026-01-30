package com.badargadh.sahkar.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.badargadh.sahkar.data.FinancialMonth;
import com.badargadh.sahkar.data.Member;
import com.badargadh.sahkar.data.MemberFeesRefundDTO;
import com.badargadh.sahkar.repository.PaymentRemarkRepository;
import com.badargadh.sahkar.service.AppConfigService;
import com.badargadh.sahkar.service.FeeService;
import com.badargadh.sahkar.service.FinancialMonthService;
import com.badargadh.sahkar.service.ReceiptPrintingService;
import com.badargadh.sahkar.service.RefundService;
import com.badargadh.sahkar.util.AppLogger;
import com.badargadh.sahkar.util.DialogManager;
import com.badargadh.sahkar.util.NotificationManager;
import com.badargadh.sahkar.util.NotificationManager.NotificationType;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.util.Callback;

@Component
public class FeesRefundController extends BaseController {

	@FXML private Label lblTotalRefundAmount;
	
    @FXML private TableView<MemberFeesRefundDTO> tblRefunds;
    @FXML private TableColumn<MemberFeesRefundDTO, String> colMemberNo, colName, colReason, colCancelDate, colAmount;
    @FXML private TableColumn<MemberFeesRefundDTO, Void> colAction;
    @FXML private TableColumn<MemberFeesRefundDTO, String> colRefundDelayedReason, colFinalRefundDate;

    @Autowired private RefundService refundService;
    @Autowired private FeeService feeService;
    @Autowired private PaymentRemarkRepository paymentRemarkRepository;
    @Autowired private AppConfigService appConfigService;
    @Autowired private FinancialMonthService monthService;
    @Autowired private ReceiptPrintingService printingService;
    
    FinancialMonth financialMonth;
    
    private ObservableList<MemberFeesRefundDTO> masterData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
    	Optional<FinancialMonth> optional = monthService.getActiveMonth();
    	if(optional.isPresent()) {
    		financialMonth = optional.get();
    		setupColumns();
            refreshTable();
            updateTotalLabel();
    	}
        
    }
    
    
    private void updateTotalLabel() {
        double total = masterData.stream()
                .mapToDouble(MemberFeesRefundDTO::getFeesRefundAmount)
                .sum();
        lblTotalRefundAmount.setText(String.format("₹ %,.2f", total));
    }

    private void setupColumns() {
        colMemberNo.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getMember().getMemberNo()+""));
        colName.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getMember().getFullname()+""));
        colReason.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getMember().getCancellationReason() != null ?
        		cd.getValue().getMember().getCancellationReason().name() : ""));
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        colCancelDate.setCellValueFactory(cd -> new SimpleStringProperty(
            cd.getValue().getMember().getCancellationDateTime() != null ? cd.getValue().getMember().getCancellationDateTime().format(formatter) : ""
        ));

        colAmount.setCellValueFactory(cd -> new SimpleStringProperty(
            String.format("₹ %.0f", cd.getValue().getFeesRefundAmount())
        ));
        
        colFinalRefundDate.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getFinalRefundDate()+""));
        
        colRefundDelayedReason.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getRefundBlockedReason()));


        setupActionColumn();
    }

    private void setupActionColumn() {
        Callback<TableColumn<MemberFeesRefundDTO, Void>, TableCell<MemberFeesRefundDTO, Void>> cellFactory = param -> new TableCell<>() {
            private final FontAwesomeIconView iconView = new FontAwesomeIconView(FontAwesomeIcon.CHECK_SQUARE);
            private final Button btn = new Button("", iconView);
            {
                String iconOnlyStyle = "-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0;";
                btn.setStyle(iconOnlyStyle);
                btn.setOnAction(event -> {
                    MemberFeesRefundDTO member = getTableView().getItems().get(getIndex());
                    promptForRefund(member);
                });
                iconView.setFill(javafx.scene.paint.Color.web("#3498db")); // Primary Blue
                iconView.setGlyphSize(18);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                // 1. If the cell is empty or the row is null, show nothing
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    // 2. Get the specific member for this row
                    MemberFeesRefundDTO memberDto = getTableView().getItems().get(getIndex());
                    
                    // 3. Check eligibility logic
                    if (isEligibleForRefund(memberDto.getMember())) {
                        setGraphic(btn);
                        btn.setTooltip(new Tooltip("Click to Process Refund"));
                    } else {
                        // 4. IMPORTANT: Clear the button for ineligible members
                        setGraphic(null); 
                    }
                }
            }
        };
        colAction.setCellFactory(cellFactory);
    }

    private void promptForRefund(MemberFeesRefundDTO member) {
        // 1. Confirmation Dialog
        if (!DialogManager.confirm("Confirm Refund", 
            "Are you sure you want to process refund for: " + member.getMember().getFullname() + "?")) {
            return;
        }

        if(showSecurityGate("Please enter your password to issue fees refund!")) {
        	processRefundAction(member, "SELF");
        }
    }

    private void processRefundAction(MemberFeesRefundDTO member, String nominee) {
        try {
        	
        	member.setMemberNo(member.getMember().getMemberNo());
        	
            refundService.processRefund(member.getMember(), nominee);
            
            printingService.printFeesRefundReceipt(member, member.getFeesRefundAmount(), 
            		member.getMember().getCancellationReason().name(), false);

            boolean wantAnother = DialogManager.confirm(
                    "Print Duplicate?", 
                    "Would you like to print a duplicate copy or re-print due to paper issues?"
                );

            if (wantAnother) {
            	printingService.printFeesRefundReceipt(member, member.getFeesRefundAmount(), 
                		member.getMember().getCancellationReason().name(), false);
                NotificationManager.show("Duplicate Receipt printed!", NotificationType.SUCCESS, Pos.CENTER);
            }
            
            NotificationManager.show("Refund Issued Successfully", NotificationType.SUCCESS, Pos.CENTER);
            refreshTable();
        } catch (Exception e) {
            DialogManager.showError("Refund Error", e.getMessage());
            AppLogger.error("Member_Fee_Refund_Error", e);
        }
    }

    @FXML
    public void refreshTable() {
    	masterData.clear();
    	if(financialMonth != null) {
    		List<MemberFeesRefundDTO> masterList = refundService.getEligibleMembers(financialMonth.getEndDate())
	    		.stream()
	    		.map(member -> getFeesRefundDTO(member))
	    		.collect(Collectors.toList());
	    	
	    	masterData.addAll(masterList);
	        tblRefunds.getItems().setAll(masterData);
	        
	        if(masterList.size() == 0) {
	        	tblRefunds.setPlaceholder(new Label("No members currently eligible for refund."));
	        }
    	}
    }
    
    public MemberFeesRefundDTO getFeesRefundDTO(Member member) {
    	MemberFeesRefundDTO dto = new MemberFeesRefundDTO();
    	dto.setMember(member);
    	dto.setFeesRefundAmount(feeService.getMemberTotalFees(member));
    	dto.setRefundEligible(isEligibleForRefund(member));
    	dto.setFinalRefundDate(getFinalRefundDate(member));
    	dto.setRefundBlockedReason(getRefundBlockReason(member));
    	return dto;
    }
    
    private boolean isEligibleForRefund(Member member) {
    	LocalDate eligibleDate = getFinalRefundDate(member);
        return financialMonth.getEndDate().isAfter(eligibleDate);
    }
    
    private LocalDate getFinalRefundDate(Member member) {
    	long remarkCount = paymentRemarkRepository.countByMemberAndIsClearedFalse(member);
        int coolingMonths = member.getCancellationDateTime()
        		.isBefore(LocalDateTime.of(LocalDate.of(2025, 12, 31), LocalTime.now()))
        		? 3 : appConfigService.getSettings().getFeesRefundCoolingPeriod();
        LocalDate eligibleDate = member.getCancellationDateTime().toLocalDate().plusMonths(coolingMonths).plusMonths((int)remarkCount);
        return eligibleDate;
    }
    
    private String getRefundBlockReason(Member member) {
    	long remarkCount = paymentRemarkRepository.countByMemberAndIsClearedFalse(member);
        return remarkCount > 0 ? "Refund blocked beacuse of pending remarks" : "cooling period";
    }
}