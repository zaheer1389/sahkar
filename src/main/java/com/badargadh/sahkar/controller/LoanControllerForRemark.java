package com.badargadh.sahkar.controller;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.badargadh.sahkar.component.DateTimePicker;
import com.badargadh.sahkar.data.AppConfig;
import com.badargadh.sahkar.data.FinancialMonth;
import com.badargadh.sahkar.data.LoanAccount;
import com.badargadh.sahkar.data.LoanApplication;
import com.badargadh.sahkar.data.LoanWitness;
import com.badargadh.sahkar.data.Member;
import com.badargadh.sahkar.data.PaymentRemark;
import com.badargadh.sahkar.enums.CollectionType;
import com.badargadh.sahkar.enums.LoanApplicationStatus;
import com.badargadh.sahkar.enums.RemarkType;
import com.badargadh.sahkar.exception.BusinessException;
import com.badargadh.sahkar.repository.LoanApplicationRepository;
import com.badargadh.sahkar.repository.LoanWitnessRepository;
import com.badargadh.sahkar.repository.PaymentRemarkRepository;
import com.badargadh.sahkar.service.AppConfigService;
import com.badargadh.sahkar.service.FinancialMonthService;
import com.badargadh.sahkar.service.LoanDisbursementService;
import com.badargadh.sahkar.service.LoanService;
import com.badargadh.sahkar.service.MemberService;
import com.badargadh.sahkar.util.AppLogger;
import com.badargadh.sahkar.util.DialogManager;
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
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

@Component
public class LoanControllerForRemark extends BaseController implements Initializable {

	@FXML private DateTimePicker loanDateTimePicker;
	@FXML private ComboBox<LoanApplicationStatus> cmbStatusFilter; // Use your LoanStatus Enum
	@FXML private ComboBox<FinancialMonth> cmbMonths;
    @FXML private TextField txtApplicationNo, txtSearch; // New Field
    @FXML private TextField txtMemberNumber;
    @FXML private TextField txtAmount;
    @FXML private Label lblMemberName, lblEligibility, lblCoolingInfo, lblBalanceInfo;
    @FXML private VBox vboxStatus, vboxAppForm;
    @FXML private TableView<LoanApplication> tblLoans;
    @FXML private TableColumn<LoanApplication, String> colMemberNo, colMember, colAmount, colDate;
    @FXML private TableColumn<LoanApplication, LoanApplicationStatus> colStatus;
    @FXML private TableColumn<LoanApplication, Void> colActions;
    
    @Autowired private LoanApplicationRepository loanApplicationRepository;
    @Autowired private LoanService loanService;
    @Autowired private MemberService memberService;
    @Autowired private AppConfigService configService;
    @Autowired private LoanDisbursementService disbursementService;
    @Autowired private FinancialMonthService financialMonthService;
    @Autowired private PaymentRemarkRepository remarkRepository;
    @Autowired private LoanWitnessRepository witnessRepository;
    
    @Autowired private ApplicationContext springContext;

    private ObservableList<LoanApplication> loanData = FXCollections.observableArrayList();
    SortedList<LoanApplication> sortedData;
    
    private Member currentSelectedMember;
    
    private AppConfig appConfig;
    
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    	appConfig = configService.getSettings();
    	txtApplicationNo.requestFocus();

        // 2. Fetch on Enter Key
        txtMemberNumber.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleFetchMember();
            }
        });

        // Numeric filters for all numeric fields
        applyNumericFilter(txtApplicationNo);
        applyNumericFilter(txtMemberNumber);
        applyNumericFilter(txtAmount);
        
        txtAmount.setText(appConfig.getLoanAmount()+"");
        txtAmount.setDisable(false);
        
        cmbStatusFilter.getItems().addAll(LoanApplicationStatus.values());
        
        List<FinancialMonth> allMonths = financialMonthService.getAllMonths();

	     // Use Stream to sort by start date in reverse
	     List<FinancialMonth> sortedMonths = allMonths.stream()
	         .sorted(Comparator.comparing(FinancialMonth::getStartDate).reversed())
	         .collect(Collectors.toList());

	     cmbMonths.setItems(FXCollections.observableArrayList(sortedMonths));
        
        setupTable();
    }
    
    private void setupTable() {
    	
    	tblLoans.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    	
    	colMemberNo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMember().getMemberNo()+""));
        colMember.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMember().getFullname()));
        colAmount.setCellValueFactory(d -> new SimpleStringProperty(String.format("%.0f", d.getValue().getAppliedAmount())));
        colDate.setCellValueFactory(d -> new SimpleStringProperty(formatter.format(d.getValue().getApplicationDateTime())));

        setupStatusColumn();
        setupActionColumn();
        setupSearch();
        loadDynamicData();
    }
    
    private void setupActionColumn() {
    	colActions.setCellFactory(column -> new TableCell<>() {
            // 1. Initialize FontAwesome Icons
            private final FontAwesomeIconView iconView = new FontAwesomeIconView(FontAwesomeIcon.EYE);
            private final FontAwesomeIconView iconEdit = new FontAwesomeIconView(FontAwesomeIcon.CHECK);
            private final FontAwesomeIconView iconCancel = new FontAwesomeIconView(FontAwesomeIcon.TIMES);
            private final FontAwesomeIconView iconAdd = new FontAwesomeIconView(FontAwesomeIcon.PLUS);

            // 2. Create Buttons with Icons only
            private final Button btnView = new Button("", iconView);
            private final Button btnEdit = new Button("", iconEdit);
            private final Button btnCancel = new Button("", iconCancel);
            private final Button btnAdd = new Button("", iconAdd);

            private final HBox container = new HBox(12, btnView, btnEdit, btnCancel, btnAdd);

            {
                String iconOnlyStyle = "-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0;";
                
                btnView.setStyle(iconOnlyStyle);
                iconView.setFill(javafx.scene.paint.Color.web("#3498db")); // Primary Blue
                iconView.setGlyphSize(18);

                btnEdit.setStyle(iconOnlyStyle);
                iconEdit.setFill(javafx.scene.paint.Color.web("#27ae60")); // Success Green
                iconEdit.setGlyphSize(18);

                btnCancel.setStyle(iconOnlyStyle);
                iconCancel.setFill(javafx.scene.paint.Color.web("#e74c3c")); // Alert Red
                iconCancel.setGlyphSize(18);
                
                btnAdd.setStyle(iconOnlyStyle);
                iconAdd.setFill(javafx.scene.paint.Color.web("#e74c3c")); // Alert Red
                iconAdd.setGlyphSize(18);

                container.setAlignment(Pos.CENTER);
                
                btnView.setTooltip(new Tooltip("View Details"));
                btnEdit.setTooltip(new Tooltip("Approve Loan"));
                btnCancel.setTooltip(new Tooltip("Reject Application"));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    LoanApplication app = getTableView().getItems().get(getIndex());
                    LoanApplicationStatus status = app.getStatus();

                    // 1. SET VISIBILITY LOGIC
                    
                    // Check if the status is one of the "Active" ones that require full management
                    boolean isActionable = (status == LoanApplicationStatus.APPLIED || 
                                            status == LoanApplicationStatus.WAITING || 
                                            status == LoanApplicationStatus.SELECTED_IN_DRAW);

                    if (isActionable) {
                        // SHOW: Edit (Approve) and Cancel | HIDE: View
                        btnView.setVisible(true);
                        btnView.setManaged(true);
                        
                        btnEdit.setVisible(true);
                        btnEdit.setManaged(true);
                        
                        btnCancel.setVisible(true);
                        btnCancel.setManaged(true);
                    } else {
                        // For all other statuses (DISBURSED, REJECTED, NO_SHOW, etc.)
                        // SHOW: View only | HIDE: Edit and Cancel
                        btnView.setVisible(true);
                        btnView.setManaged(true);
                        
                        btnEdit.setVisible(false);
                        btnEdit.setManaged(false);
                        
                        btnCancel.setVisible(false);
                        btnCancel.setManaged(false);
                        
                        btnAdd.setVisible(true);
                        btnAdd.setManaged(true);
                    }

                    // 2. ASSIGN BUTTON ACTIONS
                    btnView.setOnAction(e -> handleViewApp(app));
                    btnEdit.setOnAction(e -> handleApproveApp(app)); // Using Edit button as the trigger for Approval screen
                    btnCancel.setOnAction(e -> handleRejectApp(app));
                    btnAdd.setOnAction(e -> handleAddWitness(app));

                    setGraphic(container);
                }
            }
        });
    }
    
    private void setupStatusColumn() {
    	
    	colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
    	
    	colStatus.setCellFactory(column -> new TableCell<LoanApplication, LoanApplicationStatus>() {
            @Override
            protected void updateItem(LoanApplicationStatus item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle(""); 
                } else {
                    // Create a label to act as the "Badge"
                    Label badge = new Label(item.toString());
                    
                    // Base styles for all badges
                    String baseStyle = "-fx-padding: 2 10 2 10; " +
                                      "-fx-background-radius: 12; " + 
                                      "-fx-font-weight: bold; " +
                                      "-fx-font-size: 11px; ";

                    if (item == LoanApplicationStatus.REJECTED) {
                        // Red: Immediate visual stop
                        badge.setText("REJECTED");
                        badge.setStyle(baseStyle + "-fx-background-color: #e74c3c; -fx-text-fill: white;");
                    } else if (item == LoanApplicationStatus.DISBURSED) {
                        // Green: Success/Completed process
                        badge.setText("DISBURSED");
                        badge.setStyle(baseStyle + "-fx-background-color: #27ae60; -fx-text-fill: white;");
                    } else if (item == LoanApplicationStatus.APPLIED) {
                        // Blue: New entry / Needs attention
                        badge.setText("APPLIED");
                        badge.setStyle(baseStyle + "-fx-background-color: #3498db; -fx-text-fill: white;");
                    } else if (item == LoanApplicationStatus.SELECTED_IN_DRAW) {
                        // Amber/Orange: Exciting transition state
                        badge.setText("SELECTED");
                        badge.setStyle(baseStyle + "-fx-background-color: #f39c12; -fx-text-fill: white;");
                    } else if (item == LoanApplicationStatus.WAITING) {
                        // Grey: Idle state / Not yet processed
                        badge.setText("WAITING");
                        badge.setStyle(baseStyle + "-fx-background-color: #95a5a6; -fx-text-fill: white;");
                    } else if (item == LoanApplicationStatus.NO_SHOW) {
                        // Deep Purple: Indicates a missed opportunity/non-responsive state
                        badge.setText("NO SHOW");
                        badge.setStyle(baseStyle + "-fx-background-color: #8e44ad; -fx-text-fill: white;");
                    } else if (item == LoanApplicationStatus.REJECTED_FOR_REMARK) {
                        // Darker Red/Brown: Specific rejection type
                        badge.setText("REMARK REJECT");
                        badge.setStyle(baseStyle + "-fx-background-color: #c0392b; -fx-text-fill: white;");
                    }

                    setGraphic(badge);
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    setStyle("-fx-alignment: CENTER;"); // Center the badge in the column
                }
            }
        });
    }

    public void setupSearch() {
        FilteredList<LoanApplication> filteredData = new FilteredList<>(loanData, p -> true);

        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            // A. Apply Filtering
            filteredData.setPredicate(application -> {
                if (newValue == null || newValue.isEmpty()) return true;
                
                String filter = newValue.toLowerCase();
                Member m = application.getMember();
                
                return m.getFullname().toLowerCase().contains(filter) || 
                       String.valueOf(m.getMemberNo()).contains(filter);
            });

            // B. Apply Conditional Sorting         
            if (newValue == null || newValue.isEmpty()) {
                sortedData.setComparator(Comparator.comparing(LoanApplication::getApplicationDateTime, 
                    Comparator.nullsLast(Comparator.reverseOrder())));
            } else if (newValue.matches("\\d+")) {
                // FIX: Use a null-safe comparator for Member Number
                sortedData.setComparator(Comparator.comparing(
                    app -> app.getMember().getMemberNo(), 
                    Comparator.nullsLast(Comparator.naturalOrder())
                ));
            } else {
                sortedData.setComparator(Comparator.comparing(
                    app -> app.getMember().getFullname(), 
                    Comparator.nullsLast(Comparator.naturalOrder())
                ));
            }
        });
        
        cmbMonths.valueProperty().addListener((observable, oldValue, newValue) -> {
        	filteredData.setPredicate(application -> {
                if (newValue == null) return true;
                LocalDate date = application.getApplicationDateTime().toLocalDate();
                if(date.isAfter(newValue.getStartDate()) &&  date.isBefore(newValue.getEndDate())) return true;
                return false;
            });
        });
        
        cmbStatusFilter.valueProperty().addListener((observable, oldValue, newValue) -> {
        	filteredData.setPredicate(application -> {
                if (newValue == null) return true;
                if(newValue == application.getStatus()) return true;
                return false;
            });
        });

        SortedList<LoanApplication> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tblLoans.comparatorProperty());
        tblLoans.setItems(sortedData);
    }
    
    public void loadDynamicData() {
        // This triggers the aggregated database query
        //List<LoanApplication> summaries = loanService.getRecentApplications(LocalDateTime.now().minusMonths(4));
    	
    	// 1. Fetch your data
    	List<LoanApplication> summaries = loanService.findActiveLoansWithoutWitness();
    	loanData.setAll(summaries); // masterData

    	// 2. Create the FilteredList
    	FilteredList filteredData = new FilteredList<>(loanData, p -> true);

    	// 3. Create the SortedList and set the default Date sort
    	sortedData = new SortedList<>(filteredData);
    	sortedData.setComparator(Comparator.comparing(LoanApplication::getApplicationDateTime).reversed()); 

    	// 4. Set the TableView items ONLY ONCE to the sortedData
    	tblLoans.setItems(sortedData);
    
        setupSearch();
        
        handleResetFilters();
    }
    
    @FXML
    private void handleResetFilters() {
    	txtSearch.clear();
        cmbMonths.getSelectionModel().clearSelection();
        cmbStatusFilter.getSelectionModel().clearSelection();
    }

    @FXML
    public void loadTableData() {
        loadDynamicData();
    }
    
    @FXML
    private void refreshTableData() {
    	loadDynamicData();
    }

    @FXML
    private void handleFetchMember() {
        
        // Reset state before new fetch
        vboxStatus.setVisible(false);
        vboxAppForm.setDisable(true);
        currentSelectedMember = null;

        if (txtMemberNumber.getText().trim().isEmpty()) {
            NotificationManager.show("Please enter a member number", NotificationType.INFO, Pos.TOP_RIGHT);
            return;
        }
        
        Integer memberNo = Integer.parseInt(txtMemberNumber.getText().trim());

        try {
            // 1. Check if Member Exists
            currentSelectedMember = memberService.findByMemberNumber(memberNo);
            
            // 2. If exists, show their name and status box
            lblMemberName.setText("Name: " + currentSelectedMember.getFullname());
            vboxStatus.setVisible(true);

            // 3. Check Eligibility (Cooling Period & Balance)
            loanService.validateEligibility(currentSelectedMember, loanDateTimePicker.getValue());
            
            long remarkCount = remarkRepository.countByMemberAndIsClearedFalse(currentSelectedMember);
            if (remarkCount > 0) {
                lblEligibility.setText("REMARKS EXIST (" + remarkCount + ")");
                lblEligibility.setStyle("-fx-text-fill: #e67e22;");
                lblCoolingInfo.setText("Applying will consume 1 remark but the loan will be REJECTED.");
                // We still enable the VBox so they can click "Apply" to "consume" the remark
                vboxAppForm.setDisable(false); 
                
                return;
            }
            
            // 4. On Success: Enable Application Form
            lblEligibility.setText("✔ ELIGIBLE");
            lblEligibility.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
            lblCoolingInfo.setText("• Subscriptions: Verified");
            lblBalanceInfo.setText("• Balance: Verified");
            
            vboxAppForm.setDisable(false);
            //txtAmount.requestFocus();

        } catch (BusinessException e) {
            // This handles BOTH "Member Not Found" AND "Ineligible"
            vboxStatus.setVisible(true);
            lblMemberName.setText(currentSelectedMember != null ? "Name: " + currentSelectedMember.getFullname() : "Member Not Found");
            
            lblEligibility.setText("✘ BLOCKED");
            lblEligibility.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            lblCoolingInfo.setText("• " + e.getMessage());
            lblBalanceInfo.setText("");
            
            vboxAppForm.setDisable(true);
            AppLogger.error("Loan_Application_Error", e);
        }
    }

    @FXML
    private void handleApply() {
        if (currentSelectedMember == null) return;
        
        if(DialogManager.confirm("Please confirm!", "Do you really want to apply for loan?")) {
        	try {
                String appNo = txtApplicationNo.getText().trim();
                String amount = txtAmount.getText().trim();

                if (appNo.isEmpty() || amount.isEmpty()) {
                    throw new BusinessException("Application No. and Amount are required.");
                }

                LoanApplication application = new LoanApplication();
                application.setApplicationNumber(appNo); // Ensure this field exists in Entity
                application.setMember(currentSelectedMember);
                application.setAppliedAmount(Double.parseDouble(amount));
                application.setApplicationDateTime(loanDateTimePicker.getValue());
                loanService.submitApplication(application);

                NotificationManager.show("Loan Registered: #" + appNo, NotificationType.SUCCESS, Pos.TOP_RIGHT);
                resetForm();
                loadTableData();

            } catch (BusinessException e) {
                NotificationManager.show(e.getMessage(), NotificationType.ERROR, Pos.BOTTOM_CENTER);
                AppLogger.error("Loan_Application_Error", e);
            }
        }

        
    }

    private void resetForm() {
        txtApplicationNo.clear();
        txtMemberNumber.clear();
        //txtAmount.clear();
        vboxStatus.setVisible(false);
        vboxAppForm.setDisable(true);
        currentSelectedMember = null;
        txtApplicationNo.requestFocus();
    }

    private void applyNumericFilter(TextField field) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                field.setText(newVal.replaceAll("[^\\d]", ""));
            }
        });
    }
    
    private void handleViewApp(LoanApplication app) {
        // Logic for Stage 2: Show Witness details in popup
        System.out.println("Viewing details for: " + app.getApplicationNumber());
        LoanApplication application = loanService.findByIdWithWitnesses(app.getId()).orElse(null);
        showDisbursementDetails(application, loanService.getLoanAccountForApplication(application));
    }

    private void handleApproveApp(LoanApplication app) {
        if (app.getStatus() == LoanApplicationStatus.DISBURSED) return;

        try {
        	
        	disbursementService.validateDisbursementEligibility(app.getMember());
        	
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LoanDisbursementPopup.fxml"));
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();

            LoanDisbursementController controller = loader.getController();
            controller.initData(app);

            Stage stage = new Stage();
            stage.setTitle("Confirm Loan Disbursement");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();
            
            loadTableData(); // Refresh main table after closing popup
        } catch (BusinessException e) {
            // Show eligibility error and block the popup
            NotificationManager.show(e.getMessage(), NotificationType.ERROR, Pos.CENTER);
            AppLogger.error("Loan_Application_Error", e);
        } catch (IOException e) {
            e.printStackTrace();
            AppLogger.error("Loan_Application_Error", e);
        }
    }
    
    private void handleRejectApp(LoanApplication app) {
        // 1. Setup the Dropdown
        ComboBox<String> cmbReason = new ComboBox<>();
        cmbReason.getItems().addAll("NOT SELECTED IN DRAW", "APPLICANT NO SHOW");
        cmbReason.setPromptText("Select Reason");
        cmbReason.setMaxWidth(Double.MAX_VALUE);
        cmbReason.setPrefHeight(35);

        // 2. Setup the Layout
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setPrefWidth(350);
        content.getChildren().addAll(
            new Label("Application: #" + app.getApplicationNumber()),
            new Label("Member: " + app.getMember().getFullname()),
            new Separator(),
            new Label("Reason for Rejection:"),
            cmbReason
        );

        // 3. Open Dialog
        Optional<ButtonType> result = DialogManager.showCustomDialog("Reject Application", content);

        if (result.isPresent() && result.get().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
            String selectedReason = cmbReason.getValue();
            
            if (selectedReason == null) {
                NotificationManager.show("Please select a reason first!", NotificationType.WARNING, Pos.CENTER);
                return; // Exit or re-show dialog
            }

            // 4. Verification Gate
            if(showSecurityGate("Confirming Rejection: " + selectedReason)) {
                try {
                    // Determine status based on reason
                    LoanApplicationStatus finalStatus = selectedReason.equals("APPLICANT NO SHOW") 
                                                      ? LoanApplicationStatus.NO_SHOW 
                                                      : LoanApplicationStatus.REJECTED;
                    
                    loanService.rejectApplication(app, finalStatus, selectedReason);
                    
                    loadTableData();
                    
                    NotificationManager.show("Status updated to " + finalStatus, NotificationType.SUCCESS, Pos.TOP_RIGHT);
                } catch (Exception e) {
                    DialogManager.showError("Update Failed", e.getMessage());
                    AppLogger.error("Loan_Application_Error", e);
                }
            }
        }
    }
    
    public void showDisbursementDetails(LoanApplication app, LoanAccount acc) {
        VBox container = new VBox(15);
        container.setPadding(new Insets(20));
        container.setPrefWidth(550);
        container.setStyle("-fx-background-color: white;");

        // Header
        Label head = new Label("VERIFY DISBURSEMENT & WITNESSES");
        head.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        Label subHead = new Label("Member: " + app.getMember().getFullname().toUpperCase());
        subHead.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 14;");
        container.getChildren().addAll(head, subHead, new Separator());

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(12);

        int currentRow = 0;

        // 1. Basic Application Data
        addRow(grid, currentRow++, "Application No:", app.getApplicationNumber());
        addRow(grid, currentRow++, "Loan Amount:", "₹ " + app.getAppliedAmount());

        if (acc != null) {
            // 2. Witnesses Section Header
            Label lblWitness = new Label("WITNESS DETAILS");
            lblWitness.setStyle("-fx-font-weight: bold; -fx-text-fill: #2980b9; -fx-padding: 10 0 0 0;");
            grid.add(lblWitness, 0, currentRow++, 2, 1);
            
            // Loop through witnesses with dynamic row indexing
            if (app.getWitnesses() != null && !app.getWitnesses().isEmpty()) {
                for(LoanWitness loanWitness : app.getWitnesses()) {
                    Member member = loanWitness.getWitnessMember();
                    addRow(grid, currentRow++, "Witness Name:", member != null ? member.getFullname() : "NOT ASSIGNED");
                }
            } else {
                addRow(grid, currentRow++, "Witness:", "NO WITNESSES FOUND");
            }
            
            // 3. Payout Information
            Label lblPayout = new Label("PAYOUT DETAILS");
            lblPayout.setStyle("-fx-font-weight: bold; -fx-text-fill: #2980b9; -fx-padding: 10 0 0 0;");
            grid.add(lblPayout, 0, currentRow++, 2, 1);

            String receivedBy = (app.getCollectionType() == CollectionType.SELF) ? "SELF (MEMBER)" : "AUTHORITY";
            
            Label valReceived = new Label(receivedBy);
            valReceived.setStyle("-fx-font-weight: bold; -fx-text-fill: " + 
                (app.getCollectionType() == CollectionType.SELF ? "#27ae60" : "#e67e22"));
            
            grid.add(new Label("Received By:"), 0, currentRow);
            grid.add(valReceived, 1, currentRow++);
            
            if (app.getCollectionType() == CollectionType.AUTHORITY) {
                Label lblRem = new Label("Auth Remark:");
                Label valRem = new Label(app.getCollectionRemarks() != null ? app.getCollectionRemarks() : "N/A");
                valRem.setWrapText(true);
                valRem.setMaxWidth(300);
                valRem.setStyle("-fx-background-color: #fff3e0; -fx-padding: 8; -fx-border-color: #ffcc80; -fx-text-fill: #d35400;");
                grid.add(lblRem, 0, currentRow);
                grid.add(valRem, 1, currentRow++);
            }
        } else {
            Label lblNoAcc = new Label("⚠️ NO LOAN ACCOUNT CREATED YET");
            lblNoAcc.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            grid.add(lblNoAcc, 0, currentRow++, 2, 1);
        }

        container.getChildren().add(grid);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Disbursement View");
        dialog.getDialogPane().setContent(container);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private static void addRow(GridPane grid, int row, String label, String value) {
        Label l = new Label(label);
        l.setStyle("-fx-font-weight: bold; -fx-text-fill: #34495e;");
        Label v = new Label(value);
        v.setStyle("-fx-font-family: 'Verdana'; -fx-text-fill: #2c3e50;");
        grid.add(l, 0, row);
        grid.add(v, 1, row);
    }
    
    public void handleAddWitness(LoanApplication app) {
        Dialog<WitnessInfo> dialog = new Dialog<>();
        dialog.setTitle("Add Witness");
        dialog.setHeaderText("Search Member by Name or Number");

        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        // Holder for the final selected member
        final Member[] selectedMemberRef = { null };

        GridPane grid = new GridPane();
        grid.setPrefWidth(350);
        grid.setPrefHeight(300);
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20, 50, 10, 10));

        TextField memberNoField = new TextField();
        memberNoField.setPromptText("Auto-filled");
        
        // Searchable ComboBox
        ComboBox<Member> memberSearchCombo = new ComboBox<>();
        memberSearchCombo.setEditable(true);
        memberSearchCombo.setPromptText("Type name to search...");
        memberSearchCombo.setMaxWidth(Double.MAX_VALUE);
        
        // Custom cell factory to show names in the dropdown
        memberSearchCombo.setConverter(new StringConverter<Member>() {
            @Override public String toString(Member m) { return m == null ? "" : m.getMemberNo() + " - " + m.getFullname(); }
            @Override public Member fromString(String s) { return null; } // Selection handled by listener
        });

        TextField remarkDate = new TextField();
        TextField memberName = new TextField();

        grid.add(new Label("Search Name:"), 0, 0);
        grid.add(memberSearchCombo, 1, 0);
        grid.add(new Label("Member No:"), 0, 1);
        grid.add(memberNoField, 1, 1);
        grid.add(new Label("Member Name:"), 0, 2);
        grid.add(memberName, 1, 2);
        grid.add(new Label("Remark Date:"), 0, 3);
        grid.add(remarkDate, 1, 3);
        
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(30);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(70); // Give the label column more space
        grid.getColumnConstraints().addAll(col1, col2);
        
        memberSearchCombo.requestFocus();

        memberSearchCombo.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            // Only search if the user has typed at least 2 characters to avoid database lag
            if (newVal == null || newVal.trim().length() < 2) {
                return;
            }
            
            // 2. IMPORTANT: Check if the text change is exactly the same as the 
            // currently selected item. If so, it means a selection just happened, 
            // and we should NOT trigger a new search (which causes the 'Space' bug).
            Member selected = memberSearchCombo.getSelectionModel().getSelectedItem();
            if (selected != null && selected.getFullname().equals(newVal)) {
                return; 
            }
            
            // Run the search
            List<Member> filtered = memberService.searchByName(newVal.trim());
            
            // Update the dropdown UI
            Platform.runLater(() -> {
                memberSearchCombo.getItems().setAll(filtered);
                if (!filtered.isEmpty()) {
                    // Keep the editor's text so the space isn't swallowed
                    String currentText = memberSearchCombo.getEditor().getText();
                    
                    if (!memberSearchCombo.isShowing()) {
                        memberSearchCombo.show();
                    }
                    
                    // Restore the cursor position after the items change
                    memberSearchCombo.getEditor().setText(currentText);
                    memberSearchCombo.getEditor().positionCaret(currentText.length());
                }
            });
        });

        // 2. Logic: Handle Selection
        memberSearchCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selectedMember) -> {
            if (selectedMember != null) {
                selectedMemberRef[0] = selectedMember;
                memberNoField.setText(String.valueOf(selectedMember.getMemberNo()));
                memberNoField.setStyle("-fx-border-color: green;");
                memberName.setText(selectedMember.getFullname());
            }
        });

        // 3. Logic: Sync Member No Field (Manual entry still works)
        memberNoField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isEmpty() && !newVal.equals("null")) {
                memberService.findByMemberNo(Integer.parseInt(newVal)).ifPresent(m -> {
                    selectedMemberRef[0] = m;
                    memberSearchCombo.getEditor().setText(m.getFullname());
                });
            }
        });

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == addButtonType && selectedMemberRef[0] != null) {
                String dateStr = remarkDate.getText();
                return new WitnessInfo(selectedMemberRef[0], memberName.getText(), dateStr);
            }
            return null;
        });

        Optional<WitnessInfo> result = dialog.showAndWait();
        
        // Save logic (same as your previous implementation)
        result.ifPresent(info -> {
            saveWitnessData(app, info); 
        });
    }
    
    DateTimeFormatter formatterDate = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    public void saveWitnessData(LoanApplication app, WitnessInfo witnessInfo) {
    	boolean added = false;
    	if(witnessInfo.getMember() != null) {
    		LoanWitness loanWitness = new LoanWitness();
    		loanWitness.setLoanApplication(app);
    		loanWitness.setWitnessMember(witnessInfo.getMember());
    		loanWitness = witnessRepository.save(loanWitness);
    		
    		app.setWitnesses(Arrays.asList(loanWitness));
    		loanApplicationRepository.save(app);
    		
    		added = true;
    	}
    	else if(witnessInfo.getName() != null && !witnessInfo.getName().isEmpty()) {
    		LoanWitness loanWitness = new LoanWitness();
    		loanWitness.setLoanApplication(app);
    		loanWitness.setWintessName(witnessInfo.getName().toUpperCase());
    		loanWitness = witnessRepository.save(loanWitness);
    		
    		app.setWitnesses(Arrays.asList(loanWitness));
    		loanApplicationRepository.save(app);
    		
    		added = true;
    	}
    	
    	if(witnessInfo.getRemarkDate() != null && !witnessInfo.getRemarkDate().isEmpty()) {
    		PaymentRemark remark = new PaymentRemark();
    		remark.setMember(app.getMember());
    		remark.setIssuedDate(LocalDate.parse(witnessInfo.getRemarkDate(), formatterDate));
    		remark.setRemarkType(RemarkType.LATE_EMI);
    		remark.setFinancialMonth(financialMonthService.getMonthFromMonthAndYear("FEBRUARY", 2025).get());
    		remarkRepository.save(remark);
    		
    		added = true;
    	}
    	
    	if(added) {
    		DialogManager.showInfo("Added", "Added");
    	}
    }

    public static class WitnessInfo {
        public final Member member;
        public final String name;
        public final String remarkDate;

        public WitnessInfo(Member member, String name, String remarkDate) {
            this.member = member;
            this.name = name;
            this.remarkDate = remarkDate;
        }

		public Member getMember() {
			return member;
		}

		public String getName() {
			return name;
		}

		public String getRemarkDate() {
			return remarkDate;
		}
        
        
    }
}