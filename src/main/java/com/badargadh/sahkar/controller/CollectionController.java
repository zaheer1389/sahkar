package com.badargadh.sahkar.controller;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.badargadh.sahkar.component.DateTimePicker;
import com.badargadh.sahkar.data.AppConfig;
import com.badargadh.sahkar.data.EmiConfig;
import com.badargadh.sahkar.data.EmiPaymentGroup;
import com.badargadh.sahkar.data.FinancialMonth;
import com.badargadh.sahkar.data.LoanAccount;
import com.badargadh.sahkar.data.Member;
import com.badargadh.sahkar.data.MonthlyPayment;
import com.badargadh.sahkar.enums.CollectionLocation;
import com.badargadh.sahkar.enums.MemberStatus;
import com.badargadh.sahkar.event.ShortcutKeyEvent;
import com.badargadh.sahkar.exception.BusinessException;
import com.badargadh.sahkar.repository.EmiPaymentRepository;
import com.badargadh.sahkar.repository.FeePaymentRepository;
import com.badargadh.sahkar.service.AppConfigService;
import com.badargadh.sahkar.service.DateSecurityService;
import com.badargadh.sahkar.service.FinancialMonthService;
import com.badargadh.sahkar.service.LoanService;
import com.badargadh.sahkar.service.MemberService;
import com.badargadh.sahkar.service.PaymentCollectionService;
import com.badargadh.sahkar.service.ReceiptPrintingService;
import com.badargadh.sahkar.util.AppLogger;
import com.badargadh.sahkar.util.DialogManager;
import com.badargadh.sahkar.util.NotificationManager;
import com.badargadh.sahkar.util.NotificationManager.NotificationType;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;

@Component
public class CollectionController extends BaseController implements Initializable {

	@FXML private ComboBox<CollectionLocation> cmbCollectionLocation;
    @FXML private DateTimePicker paymentDateTimePicker;
    @FXML private TextField txtMemberNumber, txtMonthlyFees;
    @FXML private Label lblMemberName, lblLoanStatus, lblBalanceInfo, lblMemberStatus;
    @FXML private VBox vboxStatus, vboxCollectionForm;
    @FXML private ComboBox<EmiConfig> cmbEmiAmount;
    @FXML private ToggleButton btnFullPayment, btnToggleRemarks;
    @FXML private TextArea txtRemarks;
    @FXML private Button btnSubmit;
    
    @FXML private TextField txtDepositorMemberNo, txtDepositorName;
    @FXML private Button btnDepositAll;
    
    @FXML private Label lblTotalFees, lblTotalEmi, lblGrandTotal, lblFullPayCount, lblPartialPayCount, lblTotalFullAmt, lblTotalAmt;
    
    @FXML private TableView<MonthlyPayment> tblCollections;
    
    @FXML private TableColumn<MonthlyPayment, Void> colSrNo;
    @FXML private TableColumn<MonthlyPayment, String> colMemberNo;
    @FXML private TableColumn<MonthlyPayment, String> colMember;
    @FXML private TableColumn<MonthlyPayment, Integer> colEmi;
    @FXML private TableColumn<MonthlyPayment, Integer> colFees;
    @FXML private TableColumn<MonthlyPayment, Integer> colTotal;
    @FXML private TableColumn<MonthlyPayment, String> colStatus;
    @FXML private TableColumn<MonthlyPayment, Void> colAction;

    @Autowired private MemberService memberService;
    @Autowired private LoanService loanService;
    @Autowired private DateSecurityService dateGuard;
    @Autowired private AppConfigService appConfigService;
    @Autowired private ReceiptPrintingService printingService;
    @Autowired private PaymentCollectionService paymentCollectionService;
    @Autowired private FinancialMonthService monthService;
    @Autowired private EmiPaymentRepository emiPaymentRepository;
    @Autowired private FeePaymentRepository feePaymentRepository;
    
    private ObservableList<MonthlyPayment> monthlyPayments = FXCollections.observableArrayList();

    private Member selectedMember;
    private LoanAccount activeLoan;
    private AppConfig appConfig;
    
    private Member selectedDepositor;
    
    // Counter starts at 1
    private Long idCounter = 1L;
    
    @EventListener
    public void handleGlobalShortcuts(ShortcutKeyEvent event) {
        // IMPORTANT: Check if this module is actually the one being displayed
        // We check if the main container is currently visible and attached
        if (vboxCollectionForm != null && vboxCollectionForm.getScene() != null && vboxCollectionForm.isVisible()) {
            
            Platform.runLater(() -> {
                if (event.getCommand().equals("SAVE")) {
                	AppLogger.info("Collection Module: Received SAVE Event");
                    handleSavePayment();
                } else if (event.getCommand().equals("TOGGLE_FULL_PAYMENT")) {
                    btnFullPayment.setSelected(!btnFullPayment.isSelected());
                    handleFullPaymentToggle();
                }
            });
        }

    }
    
    @Override
	public void initialize(URL location, ResourceBundle resources) {
		// TODO Auto-generated method stub
    	setGUI();
    	
	}
    
    public void setCollectionLocation(CollectionLocation location) {
    	cmbCollectionLocation.setItems(FXCollections.observableArrayList(CollectionLocation.values()));
        cmbCollectionLocation.setValue(location);
        //cmbCollectionLocation.setDisable(true);
    }
    
    private void setGUI() {
    	
    	cmbCollectionLocation.setItems(FXCollections.observableArrayList(CollectionLocation.values()));
        //cmbCollectionLocation.setValue(CollectionLocation.MUMBAI);
        
        
    	// Setup EMI Dropdown values
    	setupEmiComboBox();
        
        // Default date to today
        paymentDateTimePicker.setValue(LocalDateTime.now());
        //paymentDateTimePicker.setDisable(true);

        // Bind Remarks visibility to toggle
        txtRemarks.visibleProperty().bind(btnToggleRemarks.selectedProperty());
        txtRemarks.managedProperty().bind(btnToggleRemarks.selectedProperty());
        
        appConfig = appConfigService.getSettings();
        
        txtMemberNumber.textProperty().addListener(new ChangeListener<String>() {

			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				// TODO Auto-generated method stub
				lblMemberStatus.setText("");
			}
		});
        
     // Logic to update Toggle Text and Color
        btnFullPayment.selectedProperty().addListener((obs, oldVal, newVal) -> {
            btnFullPayment.setText(newVal ? "YES" : "NO");
            btnFullPayment.setStyle(newVal ? "-fx-background-color: #27ae60; -fx-text-fill: white;" : "");
        });

        btnToggleRemarks.selectedProperty().addListener((obs, oldVal, newVal) -> {
            btnToggleRemarks.setText(newVal ? "YES" : "NO");
            btnToggleRemarks.setStyle(newVal ? "-fx-background-color: #3498db; -fx-text-fill: white;" : "");
            txtRemarks.setVisible(newVal);
            txtRemarks.setManaged(newVal);
        });

        // Setup Shortcuts after the scene is loaded
        Platform.runLater(() -> {
        	// Inside setGUI() or initialize()
        	vboxCollectionForm.parentProperty().addListener((obs, oldParent, newParent) -> {
        	    if (newParent != null) {
        	        // Wait a tiny bit for the Scene to stabilize after the loader finishes
        	        Platform.runLater(() -> {
        	            Scene scene = vboxCollectionForm.getScene();
        	            if (scene != null) {
        	                //setupShortcuts(scene);
        	            }
        	        });
        	    }
        	});
        });
        
        // Bind Table Columns
        colSrNo.setCellFactory(column -> {
            return new TableCell<MonthlyPayment, Void>() {
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setText(null);
                    } else {
                        // Display the row index + 1
                        setText(String.valueOf(getIndex() + 1));
                    }
                }
            };
        });
        
        colMemberNo.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMember().getMemberNo()+""));
        colMember.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMember().getGujFullname()));
        colEmi.setCellValueFactory(new PropertyValueFactory<>("emiAmount"));
        colFees.setCellValueFactory(new PropertyValueFactory<>("monthlyFees"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("fullAmount"));
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isFullPayment() ? "YES" : "NO"));

        colAction.setCellFactory(column -> {
            return new TableCell<>() {
                private final Button btnDelete = new Button("Delete");
                {
                    btnDelete.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand;");
                    btnDelete.setOnAction(event -> {
                        MonthlyPayment data = getTableView().getItems().get(getIndex());
                        handleDeleteRow(data);
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        setGraphic(btnDelete);
                        setAlignment(Pos.CENTER);
                    }
                }
            };
        });
        
        setupTableContextMenu();
        
        tblCollections.setItems(monthlyPayments);

        // Auto-update footer when list changes
        //monthlyPayments.addListener((ListChangeListener<MonthlyPayment>) c -> calculateTotals());
        
    }
    
    private void setupEmiComboBox() {
        // 1. Load all configs (Active + Inactive)
        cmbEmiAmount.getItems().setAll(appConfigService.getAllEmiConfigs());

        // 2. Set the CellFactory (Controls how items look in the DROPDOWN list)
        cmbEmiAmount.setCellFactory(lv -> new ListCell<EmiConfig>() {
            @Override
            protected void updateItem(EmiConfig item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setDisable(false);
                } else {
                    setText(String.format("₹ %.0f", item.getAmount()));
                    
                    if (item.isActive()) {
                        setStyle("-fx-text-fill: black; -fx-font-weight: normal;");
                        setDisable(false); // Can be selected
                    } else {
                        setText(getText() + " (Disabled)");
                        setStyle("-fx-text-fill: #a0a0a0; -fx-font-style: italic;");
                        setDisable(true); // CANNOT be selected
                    }
                }
            }
        });

        // 3. Set the ButtonCell (Controls how the SELECTED value looks when closed)
        cmbEmiAmount.setButtonCell(new ListCell<EmiConfig>() {
            @Override
            protected void updateItem(EmiConfig item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("₹ %.0f", item.getAmount()));
                }
            }
        });
    }
    
    @FXML
    private void handleFetchDepositor() {
        String memberNo = txtDepositorMemberNo.getText().trim();
        if (memberNo.isEmpty()) return;

        try {
            selectedDepositor = memberService.findByMemberNumber(Integer.parseInt(memberNo));
            txtDepositorName.setText(selectedDepositor.getFullname());
            txtDepositorName.setStyle("-fx-text-fill: #2c3e50; -fx-font-weight: bold; -fx-background-color: #f8f9fa;");
        } catch (Exception e) {
            txtDepositorName.clear();
            NotificationManager.show("Depositor member not found!", NotificationType.ERROR, Pos.CENTER);
        }
    }

    // Add the method to handle the final deposit action
    @FXML
    private void handleDepositAll() {
        // 1. Validation
        if (monthlyPayments.isEmpty()) {
            NotificationManager.show("No payments in the list to deposit!", NotificationType.WARNING, Pos.CENTER);
            return;
        }
        
        /*if (selectedDepositor == null) {
            NotificationManager.show("Please specify a valid Depositor Member first.", NotificationType.WARNING, Pos.CENTER);
            txtDepositorMemberNo.requestFocus();
            return;
        }*/
        
        if(txtDepositorName.getText().isEmpty()) {
        	NotificationManager.show("Please specify a valid Depositor Member first.", NotificationType.WARNING, Pos.CENTER);
        	txtDepositorName.requestFocus();
            return;
        }
        
        if(cmbCollectionLocation.getValue() == null) {
        	NotificationManager.show("Please select deposit location", NotificationType.WARNING, Pos.CENTER);
        	return;
        }

        // 2. Confirmation
        String msg = String.format("Confirm deposit of ₹%s from %d members?\nDeposited by: %s", 
                      lblTotalAmt.getText().split("₹")[1], 
                      monthlyPayments.size(),
                      txtDepositorName.getText());

        if (DialogManager.confirm("Confirm Deposit", msg)) {
            try {
                
                EmiPaymentGroup group = paymentCollectionService.processMonthlyCollection(monthlyPayments, selectedDepositor, txtDepositorName.getText(), cmbCollectionLocation.getValue());
                group.setMonthlyPayments(paymentCollectionService.getMonthlyPaymentsByGroup(group.getId()));
                
                printingService.printCollectionReceipt(group, false);
                
                NotificationManager.show("Collections deposited & Receipt printed!", NotificationType.SUCCESS, Pos.CENTER);
                
                boolean wantAnother = DialogManager.confirm(
                        "Print Duplicate?", 
                        "Would you like to print a duplicate copy or re-print due to paper issues?"
                    );

                if (wantAnother) {
                    printingService.printCollectionReceipt(group, true);
                    NotificationManager.show("Duplicate Receipt printed!", NotificationType.SUCCESS, Pos.CENTER);
                }
                
                // 3. Clear everything after success
                monthlyPayments.clear();
                txtDepositorMemberNo.clear();
                txtDepositorName.clear();
                selectedDepositor = null;
                calculateTotals();
                resetForm();
                
            } catch (Exception e) {
            	e.printStackTrace();
            	AppLogger.error("Monthly_Collection_Deposit_Error", e);
                NotificationManager.show("Error processing deposit: " + e.getMessage(), NotificationType.ERROR, Pos.CENTER);
            }
        }
    }
    
    private void setupTableContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem deleteItem = new MenuItem("Delete Record");
        
        deleteItem.setOnAction(event -> {
            MonthlyPayment selected = tblCollections.getSelectionModel().getSelectedItem();
            if (selected != null) {
                handleDeleteRow(selected);
            }
        });

        contextMenu.getItems().add(deleteItem);

        // Show menu only when a row is right-clicked
        tblCollections.setRowFactory(tv -> {
            TableRow<MonthlyPayment> row = new TableRow<>();
            row.contextMenuProperty().bind(
                javafx.beans.binding.Bindings.when(row.emptyProperty())
                .then((ContextMenu) null)
                .otherwise(contextMenu)
            );
            return row;
        });
    }

    private void handleDeleteRow(MonthlyPayment payment) {
        if (DialogManager.confirm("Confirm Delete", 
            "Are you sure you want to remove the payment for " + payment.getMember().getFullname() + "?")) {
            
            monthlyPayments.remove(payment);
            calculateTotals(); // Recalculate footers after removal
            NotificationManager.show("Record removed from list.", NotificationType.INFO, Pos.CENTER);
        }
    }
    
    private void calculateTotals() {
        // 1. Calculate Sums using mapToInt and sum()
        int totalFees = monthlyPayments.stream()
                .mapToInt(MonthlyPayment::getMonthlyFees)
                .sum();

        int totalEmi = monthlyPayments.stream()
                .mapToInt(MonthlyPayment::getEmiAmount)
                .sum();

        int totalFullAmt = monthlyPayments.stream()
                .mapToInt(MonthlyPayment::getFullAmount)
                .sum();

        // 2. Calculate Counts using filter and count()
        long fullPayCount = monthlyPayments.stream()
                .filter(MonthlyPayment::isFullPayment)
                .count();

        long partialPayCount = monthlyPayments.stream()
                .filter(p -> !p.isFullPayment())
                .count();

        // 3. Update UI Labels
        lblTotalFees.setText(String.format("Total Fees: ₹%d", totalFees));
        lblTotalEmi.setText(String.format("Total EMI: ₹%d", totalEmi));
        lblGrandTotal.setText(String.format("Grand Total: ₹%d", (totalFees + totalEmi)));
        
        lblFullPayCount.setText("Full Payments: " + fullPayCount);
        lblPartialPayCount.setText("Partial Payments: " + partialPayCount);
        lblTotalFullAmt.setText(String.format("Sum of Full Amts: ₹%d", totalFullAmt));
        
        lblTotalAmt.setText(String.format("Total Payments need to collect: ₹%d", (totalFees + totalEmi +totalFullAmt)));
    }

    @FXML
    private void handleFetchMember() {
        String memberNo = txtMemberNumber.getText().trim();
        System.err.println(memberNo);
        if (memberNo.isEmpty()) return;

        try {
            selectedMember = memberService.findByMemberNumber(Integer.parseInt(memberNo));
            
            if(selectedMember.getStatus() == MemberStatus.CANCELLED) {
            	lblMemberStatus.setText("Member is cancelled!");
            	return;
            }
            
            FinancialMonth activeMonth = monthService.getActiveMonth()
                    .orElseThrow(() -> new BusinessException("No active financial month found."));
            
            if(paymentCollectionService.isMonthlyMemberEmiorFeesPaid(selectedMember, activeMonth)) {
            	String msg = "Payment already received for " + activeMonth.getMonthName();
            	lblMemberStatus.setVisible(true);
                lblMemberStatus.setText(msg);
                lblMemberStatus.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                //resetForm(); // Disable the form so no duplicate can be added
                return; 
            }

        	activeLoan = loanService.findMemberActiveLoan(selectedMember);

            // 1. Update Member Info labels
            lblMemberName.setText("Name: " + selectedMember.getFullname());
            txtMonthlyFees.setText(String.valueOf(appConfig.getMonthlyFees().intValue()));
            
            // 2. Handle Loan Logic
            if (activeLoan != null) {
                lblLoanStatus.setText("LOAN: ACTIVE");
                lblLoanStatus.setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold;");
                lblBalanceInfo.setText("• Current Balance: ₹" + activeLoan.getPendingAmount());
                
                // EMI Dropdown Logic: Enable if it's the first EMI (fixed amount not set)
                if (activeLoan.getEmiAmount() == null || activeLoan.getEmiAmount() == 0) {
                    cmbEmiAmount.setDisable(false);
                    cmbEmiAmount.setPromptText("Select EMI");
                } else {
                    cmbEmiAmount.setValue(appConfigService.findByEmiAmount(activeLoan.getEmiAmount()));
                    cmbEmiAmount.setDisable(true);
                }
            } else {
                lblLoanStatus.setText("LOAN: NONE");
                lblLoanStatus.setStyle("-fx-text-fill: #7f8c8d;");
                lblBalanceInfo.setText("No active loan");
                cmbEmiAmount.setDisable(true);
                cmbEmiAmount.setValue(null);
                btnFullPayment.setDisable(true);
            }

            // Show status box and enable form
            vboxStatus.setVisible(true);
            vboxCollectionForm.setDisable(false);
            
        } catch (Exception e) {
        	e.printStackTrace();
        	AppLogger.error("Monthly_Collection_Fetch_Member_Error", e);
            resetForm();
        	lblMemberStatus.setText("Member not found!");
            //NotificationManager.show("Member not found!", NotificationType.ERROR, Pos.CENTER);
        }
    }
    
    public void triggerSaveShortcut() {
        Platform.runLater(() -> {
            if (!vboxCollectionForm.isDisabled()) { // Only save if module is active
                handleSavePayment();
            }
        });
    }
    
    public void triggerFullPaymentShortcut() {
        Platform.runLater(() -> {
            if (!vboxCollectionForm.isDisabled()) { // Only save if module is active
            	 btnFullPayment.setSelected(!btnFullPayment.isSelected());
                 handleFullPaymentToggle();
            }
        });
    }
    
    private void setupShortcuts(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown() && event.getCode() == KeyCode.S) {
                handleSavePayment();
                event.consume(); // This stops the event from going anywhere else
            }
            if (event.isAltDown() && event.getCode() == KeyCode.F) {
                btnFullPayment.setSelected(!btnFullPayment.isSelected());
                handleFullPaymentToggle();
                event.consume();
            }
        });
    }
    
    @FXML
    private void handleFullPaymentToggle() {
        if (btnFullPayment.isSelected()) {
            // Logic for auto-filling total pending balance
            System.out.println("Full Payment Enabled");
        }
    }

    @FXML
    private void handleRemarksToggle() {
        if (!btnToggleRemarks.isSelected()) {
            txtRemarks.clear(); // Clear remarks if user toggles back to NO
        }
    }


    @FXML
    private void handleSavePayment() {
    	try {
    		
    		if(txtMemberNumber.getText() == null  || txtMemberNumber.getText().isEmpty()) {
    			return;
    		}
    		
    		if(activeLoan != null && cmbEmiAmount.getValue() == null) {
            	NotificationManager.show("Please select EMI amount", 
                        NotificationType.ERROR, Pos.CENTER);
            	cmbEmiAmount.requestFocus();
        		return; // Stop execution
            }
    		
    		// 1. Validation: Check if this member is already in the current list
            boolean alreadyExists = monthlyPayments.stream()
                    .anyMatch(p -> p.getMember().getId().equals(selectedMember.getId()));

            if (alreadyExists) {
                NotificationManager.show("Duplicate Entry!\nPayment for " + selectedMember.getFullname() + " is already in the list.", 
                                        NotificationType.INFO, Pos.CENTER);
                return; // Stop execution
            }
            
            LocalDateTime paymentDateTime = paymentDateTimePicker.getValue();

            
    		MonthlyPayment payment = new MonthlyPayment();
    		payment.setId(idCounter++);
            payment.setMember(selectedMember);
            payment.setEmiDate(paymentDateTime);
            payment.setMonthlyFees(Integer.parseInt(txtMonthlyFees.getText()));
            
         // --- NEW LOGIC: Handle EMI Amount vs Pending Balance ---
            Integer selectedEmi = cmbEmiAmount.getValue().getAmount().intValue();
            int actualEmiToRecord = 0;

            if (activeLoan != null && selectedEmi != null) {
                double pendingBalance = activeLoan.getPendingAmount();
                
                // If pending is 100 but selected EMI is 300, record 100
                if (pendingBalance < selectedEmi) {
                    actualEmiToRecord = (int) pendingBalance;
                    NotificationManager.show("Capped EMI: Balance (₹" + actualEmiToRecord + ") is less than selected EMI.", 
                                            NotificationType.INFO, Pos.CENTER);
                } else {
                    actualEmiToRecord = selectedEmi;
                }
            }
            
            payment.setEmiAmount(actualEmiToRecord);
            
            // Logic: fullAmount = Pending - Current EMI
            int pending = activeLoan != null ? activeLoan.getPendingAmount().intValue() : 0;
            payment.setFullAmount(btnFullPayment.isSelected() ? pending - payment.getEmiAmount() : 0);
            
            payment.setFullPayment(btnFullPayment.isSelected());
            payment.setRemarkAdded(btnToggleRemarks.isSelected());
            
            monthlyPayments.add(0, payment);
            
            resetForm();
            
            calculateTotals();
            
            
        } catch (Exception e) {
        	e.printStackTrace();
        	AppLogger.error("Monthly_Collection_Save_Error", e);
            NotificationManager.show(e.getMessage(), NotificationType.ERROR, Pos.CENTER);
        }
    }


    @FXML
    private void dropPayments() {
        if(DialogManager.confirm("Drop Payments?", "Do you really want to drop all payments?")) {
        	monthlyPayments.clear();
        	resetForm();
        }
    }
    
    @FXML
    private void handleReset() {
    	resetForm();
    }

    private void resetForm() {
        txtMemberNumber.clear();
        vboxStatus.setVisible(false);
        vboxCollectionForm.setDisable(true);
        cmbEmiAmount.setValue(null);
        btnFullPayment.setSelected(false);
        btnFullPayment.setDisable(false);
        btnToggleRemarks.setSelected(false);
        txtRemarks.clear();
        cmbCollectionLocation.setValue(null);
        txtMemberNumber.requestFocus();
    }

	
}