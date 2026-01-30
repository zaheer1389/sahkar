package com.badargadh.sahkar.controller;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.badargadh.sahkar.data.Member;
import com.badargadh.sahkar.dto.MemberSummaryDTO;
import com.badargadh.sahkar.enums.CancellationReason;
import com.badargadh.sahkar.enums.MemberOnboardingType;
import com.badargadh.sahkar.enums.MemberStatus;
import com.badargadh.sahkar.exception.BusinessException;
import com.badargadh.sahkar.repository.MemberRepository;
import com.badargadh.sahkar.service.AppConfigService;
import com.badargadh.sahkar.service.FeeService;
import com.badargadh.sahkar.service.FinancialMonthService;
import com.badargadh.sahkar.service.MemberOnboardingService;
import com.badargadh.sahkar.service.MemberService;
import com.badargadh.sahkar.service.ReceiptPrintingService;
import com.badargadh.sahkar.service.report.JasperReportService;
import com.badargadh.sahkar.util.AppLogger;
import com.badargadh.sahkar.util.DialogManager;
import com.badargadh.sahkar.util.DialogManager.ChoiceWithRemark;
import com.badargadh.sahkar.util.FileUtil;
import com.badargadh.sahkar.util.FullBarakhadiEngine;
import com.badargadh.sahkar.util.GujaratiTransliterator;
import com.badargadh.sahkar.util.NotificationManager;
import com.badargadh.sahkar.util.NotificationManager.NotificationType;
import com.badargadh.sahkar.util.Refreshable;
import com.badargadh.sahkar.util.SurnameUtil;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

@Component
public class MemberController extends BaseController implements Initializable, Refreshable {

	@FXML private Label lblTotalMembers;
	@FXML private TextField txtSearch, txtMemberNo, txtFirstName, txtMiddleName, txtEmi;
	@FXML private TextField txtGujFirstName, txtGujMiddleName, txtGujLastName, txtGujUpName;
	@FXML private ComboBox<String> cmbLastName, cmbBranchName;
    @FXML private ComboBox<String> cmbVillage;
    @FXML private ComboBox<MemberStatus> cmbStatus;
    
    @FXML private TableView<MemberSummaryDTO> tblMembers;
    @FXML private TableColumn<MemberSummaryDTO, Integer> colMemberNo;
    @FXML private TableColumn<MemberSummaryDTO, String> colFullName;
    @FXML private TableColumn<MemberSummaryDTO, Integer> colTotalFees, colPendingLoan, colEmi;
    @FXML private TableColumn<MemberSummaryDTO, MemberStatus> colStatus;
    @FXML private TableColumn<MemberSummaryDTO, Void> colAction;
    @FXML private TableColumn<MemberSummaryDTO, String> colCancelledDate;
    
    private ContextMenu gujFirstNameMenu = new ContextMenu();
    private ContextMenu gujMiddleNameMenu = new ContextMenu();

    @Autowired private MemberService memberService;
    @Autowired private MemberOnboardingService memberOnboardingService;
    @Autowired private FinancialMonthService financialMonthService;
    @Autowired private JasperReportService jasperReportService;
    @Autowired private FeeService feeService;
    @Autowired private ReceiptPrintingService printingService;
    @Autowired private AppConfigService appConfigService;
    
    @Autowired MainController mainController;
    
    private Member selectedMember = null;

    private ObservableList<MemberSummaryDTO> masterData = FXCollections.observableArrayList();
    
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    private Long joiningFee;
    
    private static final List<String> SURNAMES = new ArrayList<String>();
    
	static {
		SURNAMES.add("KHORAJIYA");
		SURNAMES.add("NONSOLA");
		SURNAMES.add("VARALIYA");
		SURNAMES.add("KADIVAL");
		SURNAMES.add("CHAUDHARI");
		SURNAMES.add("MALPARA");
		SURNAMES.add("NODOLIYA");
		SURNAMES.add("BHORANIYA");
		SURNAMES.add("AGLODIYA");
	    SURNAMES.add("MANASIYA");
	    SURNAMES.add("MAREDIYA");
	    SURNAMES.add("SHERU");
	    SURNAMES.add("SUNASARA");
	}

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    	setGUI();
    	joiningFee = appConfigService.getSettings().getNewMemberFees();
    }
    
    @Override
    public void refresh() {
    	// TODO Auto-generated method stub
    	setGUI();
    }
    
    private void setGUI() {
    	tblMembers.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    	
    	cmbVillage.setItems(FXCollections.observableArrayList("Badargadh", "Navisna"));
    	cmbVillage.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
    	    if (isNowFocused) {
    	        // Platform.runLater ensures the popup opens correctly during layout/focus transitions
    	        Platform.runLater(() -> cmbVillage.show());
    	    }
    	});
    	
    	cmbLastName.setItems(FXCollections.observableArrayList(SURNAMES));
    	cmbLastName.setOnAction(e -> {
    	    String surname = cmbLastName.getValue();
    	    if(surname != null && !surname.isEmpty() ) {
    	    	txtGujLastName.setText(SurnameUtil.getGujSurname(surname));
    	    	cmbBranchName.setItems(FXCollections.observableArrayList(SurnameUtil.getBrnachName(surname)));
    	    }
    	    
    	});
    	
    	cmbBranchName.setOnAction(e -> {
    		String surname = cmbBranchName.getValue();
    	    txtGujUpName.setText(SurnameUtil.getGujBranchName(surname));
    	});

    	txtFirstName.textProperty().addListener((obs, o, n) ->
	        txtGujFirstName.setText(GujaratiTransliterator.toGujarati(n))
	    );
	
	    txtMiddleName.textProperty().addListener((obs, o, n) ->
	        txtGujMiddleName.setText(GujaratiTransliterator.toGujarati(n))
	    );

    	
        colMemberNo.setCellValueFactory(new PropertyValueFactory<>("memberNo"));
        colFullName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        //colFullName.setCellValueFactory(new PropertyValueFactory<>("gujaratiName"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colTotalFees.setCellValueFactory(new PropertyValueFactory<>("totalFees"));
        colPendingLoan.setCellValueFactory(new PropertyValueFactory<>("pendingLoan"));
        colEmi.setCellValueFactory(new PropertyValueFactory<>("emiAmount"));
        colCancelledDate.setCellValueFactory(m -> new SimpleStringProperty(m.getValue().getCancelledDate() != null ? 
        		formatter.format(m.getValue().getCancelledDate()) : ""));
        
        // Apply zero-handling to financial columns
        setupIntegerColumn(colTotalFees);
        setupIntegerColumn(colPendingLoan);
        setupIntegerColumn(colEmi);
        
        tblMembers.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null && newSelection.getStatus() == MemberStatus.ACTIVE) {
                fillForm(newSelection);
            }
        });

        setupActionColumn();
        setupStatusColors();
        loadDynamicData();
        //setupSearch();
        
        lblTotalMembers.setText(masterData.stream().filter(m -> m.getStatus() == MemberStatus.ACTIVE).count()+"");
        
        setupGujaratiSuggestions( txtFirstName, txtGujFirstName, gujFirstNameMenu );
        setupGujaratiSuggestions( txtMiddleName, txtGujMiddleName, gujMiddleNameMenu );
    }
 	
    private void setupActionColumn() {
        colAction.setCellFactory(column -> new TableCell<>() {
            // 1. Initialize FontAwesome Icons
            private final FontAwesomeIconView iconView = new FontAwesomeIconView(FontAwesomeIcon.EYE);
            private final FontAwesomeIconView iconEdit = new FontAwesomeIconView(FontAwesomeIcon.PENCIL);
            private final FontAwesomeIconView iconCancel = new FontAwesomeIconView(FontAwesomeIcon.TIMES);

            // 2. Create Buttons with Icons only
            private final Button btnView = new Button("", iconView);
            private final Button btnEdit = new Button("", iconEdit);
            private final Button btnCancel = new Button("", iconCancel);
            
            private final HBox container = new HBox(12, btnView, btnEdit, btnCancel);

            {
                // 3. Transparent Styling for "Icon-Only" Look
                // We use transparent backgrounds but change icon colors to match app theme
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

                container.setAlignment(Pos.CENTER);

                // 4. Action Handlers
                //btnView.setOnAction(event -> handleOpenRemarkPopup(getTableView().getItems().get(getIndex())));
                btnView.setOnAction(event -> openPopup(getTableView().getItems().get(getIndex())));
                btnEdit.setOnAction(event -> fillForm(getTableView().getItems().get(getIndex())));
                btnCancel.setOnAction(event -> handleCancelMember(getTableView().getItems().get(getIndex())));
                
                // Add Tooltips since there is no text
                btnView.setTooltip(new Tooltip("View Details"));
                btnEdit.setTooltip(new Tooltip("Edit Member"));
                btnCancel.setTooltip(new Tooltip("Cancel Membership"));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    MemberSummaryDTO member = getTableView().getItems().get(getIndex());
                    
                    // 5. Hide Cancel Icon if already cancelled
                    if (member.getStatus() == MemberStatus.CANCELLED) {
                        btnCancel.setVisible(false);
                        btnCancel.setManaged(false);
                    } else {
                        btnCancel.setVisible(true);
                        btnCancel.setManaged(true);
                    }
                    
                    setGraphic(container);
                }
            }
        });
    }
    
    public void setupSearch() {
        FilteredList<MemberSummaryDTO> filteredData = new FilteredList<>(masterData, p -> true);

        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
        	//System.err.println(newValue);
            filteredData.setPredicate(member -> {
                if (newValue == null || newValue.isEmpty()) return true;

                String lowerCaseFilter = newValue.toLowerCase();

                if (member.getFullName().toLowerCase().contains(lowerCaseFilter)) return true;
                if (String.valueOf(member.getMemberNo()).contains(lowerCaseFilter)) return true;
                if (member.getVillage().toLowerCase().contains(lowerCaseFilter)) return true;
                
                return false;
            });
        });

        SortedList<MemberSummaryDTO> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tblMembers.comparatorProperty());
        tblMembers.setItems(sortedData);
    }
    
    /**
     * Helper method to format Integer columns: 
     * 1. Right-aligns text
     * 2. Hides '0' values
     */
    private void setupIntegerColumn(TableColumn<MemberSummaryDTO, Integer> column) {
        //column.setCellValueFactory(new PropertyValueFactory<>(column.getId().replace("col", "").toLowerCase())); 
        // Note: Ensure your DTO field names match (e.g., totalFees, pendingLoan, emiAmount)

        column.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item == 0) {
                    setText("");
                    setStyle(""); 
                } else {
                    setText(String.valueOf(item));
                    //setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-family: 'monospace';");
                    setStyle("-fx-alignment: CENTER-RIGHT;");
                }
            }
        });
    }
    
    private void setupStatusColors() {
        colStatus.setCellFactory(column -> new TableCell<MemberSummaryDTO, MemberStatus>() {
            @Override
            protected void updateItem(MemberStatus item, boolean empty) {
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

                    if (item == MemberStatus.CANCELLED) {
                        // Red background with white text for Cancelled
                        badge.setStyle(baseStyle + "-fx-background-color: #e74c3c; -fx-text-fill: white;");
                    } else {
                        // Green background with white text for Active/Others
                    	badge.setText(MemberStatus.ACTIVE.name());
                        badge.setStyle(baseStyle + "-fx-background-color: #27ae60; -fx-text-fill: white;");
                    }

                    setGraphic(badge);
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    setStyle("-fx-alignment: CENTER;"); // Center the badge in the column
                }
            }
        });
    }

    public void loadDynamicData() {
        // This triggers the aggregated database query
        List<MemberSummaryDTO> summaries = memberService.findActiveMembers();
        masterData.setAll(summaries);
        tblMembers.setItems(masterData);
        
        setupSearch();
        
        txtSearch.clear();
    }
    
    private void fillForm(MemberSummaryDTO member) {
    	
    	this.selectedMember = memberService.findByMemberNo(member.getMemberNo()).get();
    	
    	txtMemberNo.setDisable(true);
        txtMemberNo.setText(String.valueOf(member.getMemberNo()));
        txtFirstName.setText(member.getFirstName());
        txtMiddleName.setText(member.getMiddleName());
        cmbLastName.setValue(member.getLastName());
        cmbBranchName.setValue(member.getBranchName());
        cmbVillage.setValue(member.getVillage());
        
        txtGujFirstName.setText(selectedMember.getFirstNameGuj());
        txtGujMiddleName.setText(selectedMember.getMiddleNameGuj());
        txtGujLastName.setText(selectedMember.getLastNameGuj());
        txtGujUpName.setText(selectedMember.getBranchNameGuj());
       
    }
    
    @FXML
    public void handleSave(ActionEvent event) {
        // Logic to save/update member
        System.out.println("Saving member...");
        if(isValidForm()) {
        	try {
        		if(selectedMember == null) {
        			
        			if(memberOnboardingService.isMemberWasPastMember(Integer.parseInt(txtMemberNo.getText()))) {
        				Long totalFee = feeService.getTotalMonthlyFeesDepositedInSociety();
        				
        				boolean proceed = DialogManager.confirm("Past Member Found", 
        			            "This member was previously part of the society.\n\n" +
        			            "To rejoin, they must pay 50% of total society fees: ₹ " + 
        			            String.format("%,.2f", totalFee) + 
        			            "\n\nDo you wish to proceed with the rejoining process?");
        				
        				if(proceed) {
        					if(showSecurityGate("Please confirm your accound identity to add member")) {
                				onBoardNewMember(MemberOnboardingType.REJOIN);
                			}
        				}
        				else {
        					resetForm(false);
        				}
        			        
        			}
        			else {
        				if(DialogManager.confirm("Please Confirm!", 
                				"Do you really want to add new member? Have you collected joining fees of Rs "+memberService.getMemberJoiningFeeDetails())) {
                			if(showSecurityGate("Please confirm your accound identity to add member")) {
                				onBoardNewMember(MemberOnboardingType.NEW);
                			}
                			else {
                				resetForm(false);
                			}
                		}
        			}

            	}
            	else {
            		if(DialogManager.confirm("Please Confirm!", "Do you really want to update member?")) {
            			updateMember();
            		}
            	}
        	}
        	catch(Exception e) {
        		DialogManager.showError("Registartion Failed", e.getMessage());
                AppLogger.error("Member_Creation_Error", e);
        		resetForm(false);
        	}
        }
    }
    

    @FXML
    public void handleClear(ActionEvent event) {
    	resetForm(false);
    }
    
    public boolean isValidForm() {
    	
    	if(txtMemberNo.getText().isEmpty()) {
            DialogManager.showError("Input Required", "Member number must not be null");
            return false;
        }
    	
    	if (txtFirstName.getText().isEmpty() || txtMiddleName.getText().isEmpty() || cmbLastName.getValue() == null) {
            DialogManager.showError("Input Required", "Please fill in all mandatory fields.");
            return false;
        }
    	
    	if (cmbVillage.getValue().isEmpty()) {
            DialogManager.showError("Input Required", "Please select village");
            return false;
        }
    	
    	return true;
    }
    
    public void resetForm(boolean reloadData) {
    	
    	selectedMember = null;
    	
    	txtMemberNo.setDisable(false);
        txtMemberNo.clear();
        txtFirstName.clear();
        txtMiddleName.clear();
        cmbLastName.getSelectionModel().clearSelection();
        cmbBranchName.getSelectionModel().clearSelection();
        cmbVillage.getSelectionModel().clearSelection();
        //cmbStatus.getSelectionModel().clearSelection();
        
        txtGujFirstName.clear();
        txtGujMiddleName.clear();
        txtGujLastName.clear();
        txtGujUpName.clear();
        
        if(reloadData) {
        	loadDynamicData();
        }
    }
    
    public void onBoardNewMember(MemberOnboardingType type) {
    	Member member = new Member();
    	member.setMemberNo(Integer.parseInt(txtMemberNo.getText()));
    	member.setFirstName(txtFirstName.getText().toUpperCase());
    	member.setMiddleName(txtMiddleName.getText().toUpperCase());
    	member.setLastName(cmbLastName.getValue().toUpperCase());
    	if(cmbBranchName.getValue() != null) {
    		member.setBranchName(cmbBranchName.getValue().toUpperCase());
    	}
    	member.setVillage(cmbVillage.getValue());
    	member.setFinancialMonth(financialMonthService.getActiveMonth().get());
    	
    	member = memberOnboardingService.registerNewMember(member, type);
    	
    	printingService.printMemberJoiningReceipt(member, joiningFee, false);

        boolean wantAnother = DialogManager.confirm(
                "Print Duplicate?", 
                "Would you like to print a duplicate copy or re-print due to paper issues?"
            );

        if (wantAnother) {
            printingService.printMemberJoiningReceipt(member, joiningFee, true);
            NotificationManager.show("Duplicate Receipt printed!", NotificationType.SUCCESS, Pos.CENTER);
        }
    	
    	NotificationManager.show(
                "Member with member no "+member.getMemberNo()+" created successfully!", 
                NotificationManager.NotificationType.SUCCESS, 
                Pos.TOP_RIGHT
            );
    	
    	resetForm(true);
    }
    
    public void updateMember() {
    	selectedMember.setFirstName(txtFirstName.getText().toUpperCase());
    	selectedMember.setMiddleName(txtMiddleName.getText().toUpperCase());
    	selectedMember.setLastName(cmbLastName.getValue().toUpperCase());
    	if(cmbBranchName.getValue() != null) {
    		selectedMember.setBranchName(cmbBranchName.getValue().toUpperCase());
    	}
    	
    	selectedMember.setFirstNameGuj(txtGujFirstName.getText());
    	selectedMember.setMiddleNameGuj(txtGujMiddleName.getText());
    	selectedMember.setLastNameGuj(txtGujLastName.getText());
    	if(cmbBranchName.getValue() != null) {
    		selectedMember.setBranchNameGuj(txtGujUpName.getText());
    	}
    	
    	selectedMember.setVillage(cmbVillage.getValue());
    	selectedMember = memberService.save(selectedMember);

    	NotificationManager.show(
                "Member with member no "+selectedMember.getMemberNo()+" updated successfully!", 
                NotificationManager.NotificationType.SUCCESS, 
                Pos.TOP_RIGHT
            );
    	
    	resetForm(true);
    }
    
    private void handleViewMember(MemberSummaryDTO dto) {
        // Logic to show member details (e.g., in a popup or separate tab)
        System.out.println("Viewing Member: " + dto.getFullName());
    }

 // Inside MemberController.java

    private void handleCancelMember(MemberSummaryDTO dto) {
    	
    	try {
    		memberService.cancelMembershipValidationChecks(dto.getMemberNo());
        	
            if (DialogManager.confirm("Confirm Cancellation", "Are you sure you want to cancel membership for " + dto.getFullName() + "?")) {
                
            	List<CancellationReason> reasons = Arrays.asList(CancellationReason.values());
                /*CancellationReason selectedReason = DialogManager.showChoiceDialog(
                        "Cancellation Reason", 
                        "Select reason for " + dto.getFullName() +" ("+dto.getMemberNo()+")", 
                        reasons
                );*/
                
                ChoiceWithRemark<CancellationReason> result = DialogManager.showChoiceDialogWithRemark("Cancellation Reason", "Select reason for " + dto.getFullName() +" ("+dto.getMemberNo()+")", reasons);

                if (result == null) return;
                
            	if(showSecurityGate("Please confirm your identity to cancel member.")) {
            		try {
                        // 3. Delegate to Service
                        memberService.cancelMembership(dto.getMemberNo(), result.getSelection(), result.getRemark());

                        NotificationManager.show("Membership cancelled successfully.", 
                                               NotificationType.SUCCESS, Pos.TOP_RIGHT);
                        
                        loadDynamicData(); // Refresh Table
                    } catch (BusinessException e) {
                        DialogManager.showError("Cancellation Denied", e.getMessage());
                        AppLogger.error("Member_Cancellation_Error", e);
                    } catch (Exception e) {
                        DialogManager.showError("Error", "An unexpected error occurred: " + e.getMessage());
                        AppLogger.error("Member_Cancellation_Error", e);
                    }

            	}
            }
    	}
    	catch(BusinessException e) {
    		DialogManager.showError("Cancellation Denied", e.getMessage());
            AppLogger.error("Member_Cancellation_Error", e);
    	}
    	catch (Exception e) {
            DialogManager.showError("Error", "An unexpected error occurred: " + e.getMessage());
            AppLogger.error("Member_Cancellation_Error", e);
        }
    }
    
    @FXML
    private void handleExportPdf() {

    	if (masterData.isEmpty()) {
            NotificationManager.show("No records to export.", NotificationType.WARNING, Pos.TOP_RIGHT);
            return;
        }
    	
    	String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    	String fileName = "Member_Details_Report_" + timestamp + ".pdf";
    	File targetFile = FileUtil.getReportOutputFile(fileName);

        if (targetFile != null) {
            try {
            	jasperReportService.generateMemberReport(memberService.findActiveMembersForReport(), targetFile.getAbsolutePath());
                NotificationManager.show("Report generated: " + targetFile.getName(), NotificationType.SUCCESS, Pos.TOP_RIGHT);
                
                mainController.showReport(targetFile);
                
            } catch (Exception e) {
                e.printStackTrace();
                DialogManager.showError("Export Error", "Could not generate PDF: " + e.getMessage());
                AppLogger.error("Member_Export_PDF_Error", e);
            }
        }
    }
    
    // Inside your main Member List controller
    private void openPopup(MemberSummaryDTO member) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MemberInsightsPopup.fxml"));
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();
            
            // Access the controller AFTER loading the FXML
            MemberInsightsController controller = loader.getController();
            controller.initData(member);
            
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("મેમ્બર ડેશબોર્ડ - " + member.getFullGujName());
            stage.initModality(Modality.APPLICATION_MODAL); // Blocks main window
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void handleOpenRemarkPopup(MemberSummaryDTO dto) {
        try {
        	
        	Member member = memberService.findByMemberNumber(dto.getMemberNo());
        	
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MemberRemarks.fxml"));
            // This line is crucial to allow Spring @Autowired in the Remark Controller
            loader.setControllerFactory(springContext::getBean);
            
            Parent root = loader.load();

            // Get the controller and load the data for this specific member
            MemberRemarkController controller = loader.getController();
            controller.loadMemberRemarks(member);

            Stage stage = new Stage();
            stage.setTitle("Remark Records - " + member.getFullname());
            stage.initModality(Modality.APPLICATION_MODAL); // Blocks interaction with main window
            stage.setScene(new Scene(root));
            
            // Optional: Add a CSS file for the table styling
            // stage.getScene().getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            AppLogger.error("Member_Remark_List_Popup_Error", e);
            NotificationManager.show("Error opening popup: " + e.getMessage(), NotificationType.ERROR, Pos.CENTER);
        }
    }
    
    private void setupGujaratiSuggestions( TextField englishField, TextField gujaratiField, ContextMenu menu ) {

        englishField.textProperty().addListener((obs, oldVal, newVal) -> {

            menu.getItems().clear();

            if (newVal == null || newVal.isBlank()) {
                menu.hide();
                gujaratiField.clear();
                return;
            }

            // Generate suggestions
            //Set<String> suggestions = GujaratiSuggestionUtil.getSuggestions(newVal);
            
            //Set<String> suggestions = SmartGujaratiSuggestionUtil.getSuggestions(newVal);
            
            Set<String> suggestions = FullBarakhadiEngine.getSuggestions(newVal);

            if (suggestions.isEmpty()) {
                menu.hide();
                return;
            }

            // Default autofill (first suggestion)
            gujaratiField.setText(suggestions.iterator().next());

            // Build popup menu
            for (String s : suggestions) {
                MenuItem item = new MenuItem(s);
                item.setOnAction(e -> {
                    gujaratiField.setText(s);
                    menu.hide();
                });
                menu.getItems().add(item);
            }

            // Show popup below Gujarati field
            if (!menu.isShowing()) {
                menu.show(gujaratiField, Side.BOTTOM, 0, 0);
            }
        });

        // Hide menu when Gujarati field loses focus
        gujaratiField.focusedProperty().addListener((o, was, is) -> {
            if (!is) menu.hide();
        });
    }

    
}