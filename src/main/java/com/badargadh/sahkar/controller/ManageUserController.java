package com.badargadh.sahkar.controller;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

@Component
public class ManageUserController implements Initializable {

    @FXML private TextField txtMemberSearch, txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private ComboBox<Role> cmbRole;
    @FXML private Label lblMemberDetails;
    
    @FXML private TableView<AppUser> tblUsers;
    @FXML private TableColumn<AppUser, String> colMemberNo, colMemberName, colUsername, colRole;

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
    }

    private void refreshTable() {
        tblUsers.setItems(FXCollections.observableArrayList(userService.findAllUsers()));
    }
}