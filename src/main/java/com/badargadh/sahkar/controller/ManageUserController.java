package com.badargadh.sahkar.controller;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import com.badargadh.sahkar.data.MemberFeesRefundDTO;
import com.badargadh.sahkar.dto.MemberSummaryDTO;
import com.badargadh.sahkar.enums.MemberStatus;
import com.badargadh.sahkar.util.DialogManager;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.badargadh.sahkar.data.AppUser;
import com.badargadh.sahkar.data.Member;
import com.badargadh.sahkar.enums.Role;
import com.badargadh.sahkar.service.MemberService;
import com.badargadh.sahkar.service.UserService;
import com.badargadh.sahkar.util.AppLogger;
import com.badargadh.sahkar.util.NotificationManager;
import com.badargadh.sahkar.util.NotificationManager.NotificationType;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.cell.PropertyValueFactory;

@Component
public class ManageUserController extends BaseController implements Initializable {

    @FXML private TextField txtMemberSearch, txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private ComboBox<Role> cmbRole;
    @FXML private Label lblMemberDetails;
    @FXML private Button btnSave;

    @FXML private TableView<AppUser> tblUsers;
    @FXML private TableColumn<AppUser, String> colMemberNo, colMemberName, colUsername, colRole;
    @FXML private TableColumn<AppUser, Boolean> colStatus;
    @FXML private TableColumn<AppUser, Void> colAction;

    @Autowired private UserService userService;
    @Autowired private MemberService memberService;

    private Member selectedMember;
    private AppUser selectedUser;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cmbRole.setItems(FXCollections.observableArrayList(Role.values()));
        setupTable();
        refreshTable();
    }

    private void setupTable() {
        colMemberNo.setCellValueFactory(data -> 
            new SimpleStringProperty(String.valueOf(data.getValue().getMember() != null ? data.getValue().getMember().getMemberNo() : "")));
        
        colMemberName.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getMember() != null ? data.getValue().getMember().getFullname() : "System Admin"));
            
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("active"));

        setupStatusColors();
        setupActionColumn();
    }

    private void setupStatusColors() {
        colStatus.setCellFactory(column -> new TableCell<AppUser, Boolean>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if(empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                }
                else {
                    Label badge = new Label(item.toString());

                    // Base styles for all badges
                    String baseStyle = "-fx-padding: 2 10 2 10; " +
                            "-fx-background-radius: 12; " +
                            "-fx-font-weight: bold; " +
                            "-fx-font-size: 11px; ";

                    if (!item) {
                        badge.setText("Disabled");
                        badge.setStyle(baseStyle + "-fx-background-color: #e74c3c; -fx-text-fill: white;");
                    } else {
                        badge.setText("Active");
                        badge.setStyle(baseStyle + "-fx-background-color: #27ae60; -fx-text-fill: white;");
                    }

                    setGraphic(badge);
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    setStyle("-fx-alignment: CENTER;"); // Center the badge in the column
                }

            }
        });
    }

    private void setupActionColumn() {
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnEdit = new Button();
            private final Button btnToggle = new Button();
            private final HBox container = new HBox(15, btnEdit, btnToggle);

            {
                // Edit Button (Deep Teal)
                btnEdit.getStyleClass().add("icon-button");
                FontAwesomeIconView iconEdit = new FontAwesomeIconView(FontAwesomeIcon.PENCIL);
                iconEdit.setFill(javafx.scene.paint.Color.web("#005461"));
                iconEdit.setGlyphSize(18);
                btnEdit.setGraphic(iconEdit);

                // Toggle Button (Dynamic Color)
                btnToggle.getStyleClass().add("icon-button");
                container.setAlignment(Pos.CENTER);

                String iconOnlyStyle = "-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0;";
                btnEdit.setStyle(iconOnlyStyle);
                btnToggle.setStyle(iconOnlyStyle);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    AppUser user = getTableView().getItems().get(getIndex());

                    // Change icon based on active status
                    FontAwesomeIconView iconToggle = new FontAwesomeIconView(
                            user.getActive() ? FontAwesomeIcon.TOGGLE_ON : FontAwesomeIcon.TOGGLE_OFF
                    );
                    iconToggle.setGlyphSize(18);
                    iconToggle.setFill(user.getActive() ?
                            javafx.scene.paint.Color.web("#059669") : // Green
                            javafx.scene.paint.Color.web("#dc2626")   // Red
                    );
                    btnToggle.setGraphic(iconToggle);

                    btnEdit.setOnAction(e -> handleEditUser(user));
                    btnToggle.setOnAction(e -> handleDisableUser(user));

                    setGraphic(container);
                }
            }
        });
    }

    @FXML
    private void handleFetchMember() {
        try {
            Long mNo = Long.parseLong(txtMemberSearch.getText());
            Optional<Member> optional = memberService.findByMemberNo(mNo.intValue());
            if (optional.isPresent()) {
            	selectedMember = optional.get();
                lblMemberDetails.setText("Linked: " + selectedMember.getFullname());
            }
        } catch (Exception e) {
            NotificationManager.show("Invalid Member Number", NotificationType.ERROR,Pos.CENTER);
            AppLogger.error("Manage_User_Handle_Fetch_Member_Error", e);
        }
    }

    @FXML
    private void handleSave() {
        if (selectedMember == null || txtUsername.getText().isEmpty()) {
            NotificationManager.show("Please link a member and provide username", NotificationType.WARNING, Pos.CENTER);
            return;
        }

        AppUser user = (selectedUser == null) ? new AppUser() : selectedUser;
        user.setUsername(txtUsername.getText());
        user.setRole(cmbRole.getValue());
        user.setMember(selectedMember);
        
        // In a real app, hash this password before saving
        if (!txtPassword.getText().isEmpty()) {
            user.setPassword(txtPassword.getText());
        }

        userService.saveAppUser(user);
        NotificationManager.show("User Access Updated", NotificationType.SUCCESS, Pos.CENTER);
        handleClear();
        refreshTable();
    }

    @FXML
    private void handleClear() {
        selectedMember = null;
        selectedUser = null;
        txtUsername.clear();
        txtPassword.clear();
        txtMemberSearch.clear();
        lblMemberDetails.setText("No member linked");
        cmbRole.getSelectionModel().clearSelection();
        txtUsername.setDisable(false);
        cmbRole.setDisable(false);
        btnSave.setText("CREATE ACCOUNT");
    }

    private void refreshTable() {
        List<AppUser> appUsers = userService.findAllUsers().stream()
                .filter(u -> !u.getUsername().equals("admin"))
                .toList();
        tblUsers.setItems(FXCollections.observableArrayList(appUsers));
    }

    private void setUserFormData(AppUser appUser) {
        selectedMember = appUser.getMember();
        selectedUser = appUser;
        txtUsername.setText(appUser.getUsername());
        txtUsername.setDisable(true);
        cmbRole.setValue(appUser.getRole());
        cmbRole.setDisable(true);
        if(appUser.getMember() != null) {
            txtMemberSearch.setText(appUser.getMember().getMemberNo().toString());
            handleFetchMember();
        }
        btnSave.setText("UPDATE ACCOUNT");
    }

    private void handleEditUser(AppUser appUser) {
        setUserFormData(appUser);
    }

    private void handleDisableUser(AppUser appUser) {
        String status = (appUser.getActive() ? "deactivate" : "activate");
        String msg = "Do you really want to " + status + " user " + appUser.getUsername();

        if (DialogManager.confirm("User Action", msg)) {
            if (showSecurityGate("Please confirm your identity to change user status")) {
                appUser.setActive(!appUser.getActive());

                // FIX: Use the repository directly for status changes
                // to bypass the password encoding logic in the Service
                userService.saveAppUser(appUser);

                status = appUser.getActive() ? "activated" : "deactivated";
                DialogManager.showInfo("User Action", "User " + status + " successfully");
                refreshTable();
            }
        }
    }

}