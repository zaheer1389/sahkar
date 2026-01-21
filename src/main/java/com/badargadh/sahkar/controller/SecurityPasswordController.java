package com.badargadh.sahkar.controller;

import java.net.URL;
import java.util.ResourceBundle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.badargadh.sahkar.data.AppUser;
import com.badargadh.sahkar.service.UserService;
import com.badargadh.sahkar.util.AppLogger;
import com.badargadh.sahkar.util.NotificationManager;
import com.badargadh.sahkar.util.NotificationManager.NotificationType;
import com.badargadh.sahkar.util.UserSession;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

@Component
public class SecurityPasswordController implements Initializable {
	
	@Autowired private UserService userService;

    @FXML private PasswordField txtPassword;
    @FXML private Label lblActionName;
    @FXML private TextField txtUsername;

    private boolean authenticated = false;

    public void setActionName(String name) {
        lblActionName.setText("Confirming: " + name);
    }

    public boolean isAuthorized() {
        return authenticated;
    }

    @FXML
    private void handleConfirm() {
    	try {
    		AppUser appUser = UserSession.getLoggedInUser();
            if (userService.authenticate(appUser.getUsername(), txtPassword.getText()) != null) {
                authenticated = true;
                closeStage();
            } else {
                authenticated = false;
                NotificationManager.show("Invalid Password. Please try again.", NotificationType.ERROR, Pos.CENTER);
                txtPassword.clear();
            }
    	}
    	catch(Exception e) {
    		AppLogger.error("Password check fail!!", e);
    		NotificationManager.show("Invalid Password. Please try again.", NotificationType.ERROR, Pos.CENTER);
            txtPassword.clear();
    	}
    }

    @FXML
    private void handleCancel() {
        authenticated = false;
        closeStage();
    }

    private void closeStage() {
        Stage stage = (Stage) txtPassword.getScene().getWindow();
        stage.close();
    }

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// TODO Auto-generated method stub
		AppUser appUser = UserSession.getLoggedInUser();
		txtUsername.setText(appUser.getUsername());
		
		// Use Platform.runLater to ensure the scene is rendered before requesting focus
	    javafx.application.Platform.runLater(() -> {
	        txtPassword.requestFocus();
	    });
	}
}