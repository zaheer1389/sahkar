package com.badargadh.sahkar.controller;

import java.net.URL;
import java.util.ResourceBundle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.badargadh.sahkar.PrimaryStageInitializer;
import com.badargadh.sahkar.data.AppUser;
import com.badargadh.sahkar.repository.UserRepository;
import com.badargadh.sahkar.service.UserService;
import com.badargadh.sahkar.util.AppLogger;
import com.badargadh.sahkar.util.UserSession;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

@Component
public class LoginController implements Initializable {

	@FXML private TextField txtPasswordVisible;
	@FXML private Button btnTogglePass;
	
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblMessage;
    @FXML private StackPane rootPane;

    @Autowired private UserRepository userRepo;
    @Autowired private ApplicationContext context;
    @Autowired private UserService userService;
    
    @Autowired private PrimaryStageInitializer primaryStageInitializer;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initial animation or focus
        Platform.runLater(() -> txtUsername.requestFocus());
        
        // Fix: Ensure Enter key also moves focus to the password field
        txtUsername.setOnAction(event -> txtPassword.requestFocus());
        
        // Fix: If using the visible password field toggle, synchronize their focus actions
        txtPassword.setOnAction(event -> handleLogin(event));
        txtPasswordVisible.setOnAction(event -> handleLogin(event));
        
        txtUsername.textProperty().addListener((observable, oldValue, newValue) -> lblMessage.setText(""));
        txtPassword.textProperty().addListener((observable, oldValue, newValue) -> lblMessage.setText(""));

        txtPassword.textProperty().bindBidirectional(txtPasswordVisible.textProperty());
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = txtUsername.getText();
        String password = txtPassword.getText();

        if (username.isEmpty() || password.isEmpty()) {
            lblMessage.setText("Please enter both username and password");
            return;
        }

        try {
        	// Now perform your login logic with the 'password' variable
            AppUser appUser = userService.authenticate(username, password);
            System.out.println(appUser);
            if (appUser != null) {
            	if(appUser.getActive()) {
                    UserSession.setLoggedInMember(appUser);
                    startDashboardLoadingTask();
                }
                else {
                    lblMessage.setText("User account is disabled. Please contact admin to activate it.");
                }
            } else {
                lblMessage.setText("Invalid username or password");
            }
        }
        catch(Exception e) {
        	lblMessage.setText("Invalid username or password");
        	AppLogger.error("Login Failed!", e);
        }
    }

   /*private String getActivePassword() {
        // If PasswordField is visible/managed, use it. Otherwise use the TextField.
        if (txtPassword.isManaged()) {
            return txtPassword.getText();
        } else {
            return txtPasswordVisible.getText();
        }
    }*/

    private void startDashboardLoadingTask() {
        // 1. Create a Loader Overlay
        VBox loaderOverlay = new VBox(20);
        loaderOverlay.setAlignment(Pos.CENTER);
        loaderOverlay.setStyle("-fx-background-color: rgba(255, 255, 255, 0.8);"); // Semi-transparent
        
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setPrefSize(80, 80);
        
        Label loadingLabel = new Label("Loading System...");
        loadingLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        loaderOverlay.getChildren().addAll(progressIndicator, loadingLabel);
        
        // 2. Add loader to the StackPane
        rootPane.getChildren().add(loaderOverlay);
        
        // 3. Background Task to load FXML
        Task<Parent> loadTask = new Task<>() {
            @Override
            protected Parent call() throws Exception {
                // Simulate a small delay or pre-fetching if necessary
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Main.fxml"));
                loader.setControllerFactory(context::getBean);
                return loader.load();
            }
        };

        // 4. On Success: Switch Scenes
        loadTask.setOnSucceeded(e -> {
            Parent root = loadTask.getValue();
            Stage stage = primaryStageInitializer.getPrimaryStage();
            Scene scene = stage.getScene();
            
            if (scene == null) {
                stage.setScene(new Scene(root));
            } else {
                scene.setRoot(root);
            }
            
            stage.setTitle("Society Management System");
            stage.setMaximized(true);
            stage.show();
        });

        // 5. On Failure: Remove loader and show error
        loadTask.setOnFailed(e -> {
            rootPane.getChildren().remove(loaderOverlay);
            lblMessage.setText("Failed to load dashboard.");
            
            loadTask.getException().printStackTrace();
        });

        new Thread(loadTask).start();
    }
    
    @FXML
    private void clearUsername() {
        txtUsername.clear();
    }

    @FXML
    private void clearPassword() {
        txtPassword.clear();
        txtPasswordVisible.clear();
    }

    @FXML
    private void handleReset() {
        txtUsername.clear();
        txtPassword.clear();
        txtPasswordVisible.clear();
        lblMessage.setText("");
    }

    @FXML
    private void togglePassword() {
        boolean isUsernameFocused = txtUsername.isFocused();
        boolean isPasswordFocused = txtPassword.isFocused() || txtPasswordVisible.isFocused();

        if (txtPassword.isVisible()) {
            txtPasswordVisible.setText(txtPassword.getText());
            txtPasswordVisible.setVisible(true);
            txtPasswordVisible.setManaged(true);
            txtPassword.setVisible(false);
            txtPassword.setManaged(false);
            
            // Fix: Maintain focus if the user was already typing in the password field
            if (isPasswordFocused) txtPasswordVisible.requestFocus();
        } else {
            txtPassword.setText(txtPasswordVisible.getText());
            txtPassword.setVisible(true);
            txtPassword.setManaged(true);
            txtPasswordVisible.setVisible(false);
            txtPasswordVisible.setManaged(false);
            
            if (isPasswordFocused) txtPassword.requestFocus();
        }
    }
}