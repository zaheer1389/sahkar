package com.badargadh.sahkar.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.ResourceBundle;

import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.badargadh.sahkar.PrimaryStageInitializer;
import com.badargadh.sahkar.SessionManager;
import com.badargadh.sahkar.data.AppUser;
import com.badargadh.sahkar.enums.Role;
import com.badargadh.sahkar.event.FinancialStatusChangedEvent;
import com.badargadh.sahkar.event.ShortcutKeyEvent;
import com.badargadh.sahkar.repository.EmiPaymentRepository;
import com.badargadh.sahkar.service.FinancialMonthService;
import com.badargadh.sahkar.service.MemberService;
import com.badargadh.sahkar.service.MySqlBackupService;
import com.badargadh.sahkar.util.AppLogger;
import com.badargadh.sahkar.util.DialogManager;
import com.badargadh.sahkar.util.NotificationManager;
import com.badargadh.sahkar.util.NotificationManager.NotificationType;
import com.badargadh.sahkar.util.Refreshable;
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
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

@Component
public class MainController implements Initializable {
	
	private final String SETTINGS_PATH = System.getProperty("user.dir") + File.separator + "sahkar_settings.properties";
    @FXML private  Button btnFinancialMonths;
    @FXML private  Button btnDashboard;
    @FXML private  StackPane rootPane;
    @FXML private BorderPane mainBorderPane;
    @FXML private  Button btnBackup;
    @FXML private VBox vboxAdminMenu;
	@FXML private StackPane contentArea;
    @FXML private TabPane mainTabPane;
    @FXML private Label lblAdminName;
    @FXML private Label lblFooterInfo;
    @FXML private Label lblLastBackup;
    @FXML private Label lblHeaderPeriod;
    @FXML private Circle statusIndicator;
    @FXML private HBox statusContainer;
    @FXML private VBox globalBlocker;
    @FXML private Label lblGlobalStatus;
    
    @Autowired private ApplicationContext springContext;
    @Autowired private FinancialMonthService financialMonthService;
    @Autowired private MemberService memberService;
    @Autowired private EmiPaymentRepository emiPaymentRepository;
    @Autowired private SessionManager sessionManager;
    @Autowired private PrimaryStageInitializer primaryStageInitializer;
    
    @Autowired private ApplicationEventPublisher eventPublisher;
    
    private PdfViewerController pdfViewerController;
    private Object currentActiveController;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Set info from the session we created earlier
        if (UserSession.getLoggedInMember() != null) {
            lblAdminName.setText(UserSession.getLoggedInMember().getFirstName() + " " + UserSession.getLoggedInMember().getLastName());
        }
        lblFooterInfo.setText("Database: Badargadh_Sahkar | User: " + System.getProperty("user.name"));
        
        updateStatusHeader();
        applySecuritySettings();
        refreshBackupStatus();
        
        //dataService.generateDummyApplications();
        
        //financialMonthService.recalculateAllBalances();
        
        Platform.runLater(() -> {
            Stage stage = primaryStageInitializer.getPrimaryStage();
            stage.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                
                // Detect Ctrl + S
                if (event.isControlDown() && event.getCode() == KeyCode.S) {
                    eventPublisher.publishEvent(new ShortcutKeyEvent(this, "SAVE"));
                    event.consume();
                }
                
                // Detect Alt + F
                if (event.isAltDown() && event.getCode() == KeyCode.F) {
                    eventPublisher.publishEvent(new ShortcutKeyEvent(this, "TOGGLE_FULL_PAYMENT"));
                    event.consume();
                }
                
                // Detect Alt + R
                if (event.isControlDown() && event.getCode() == KeyCode.R) {
                    eventPublisher.publishEvent(new ShortcutKeyEvent(this, "RESET"));
                    event.consume();
                }
            });
        });
    }
    
    @FXML
    private void handleBackupNow() {
    	if(DialogManager.confirm("Database backup", "Do you want to take database backup now?")) {
    		if(MySqlBackupService.performBackup()) {
    			DialogManager.showInfo("Database backup", "Backup Successfull");
    		}
    		else {
    			DialogManager.showError("Database backup", "Error occured while Backup");
    		}
    	}
    }
    
    public void refreshBackupStatus() {
        Properties prop = new Properties();
        File file = new File(SETTINGS_PATH);
        
        if (file.exists()) {
            try (InputStream input = new FileInputStream(file)) {
                prop.load(input);
                String lastTime = prop.getProperty("last.backup", "Never");
                lblLastBackup.setText("Last Backup: " + lastTime);
                
                // Update the color based on age
                if (!lastTime.equals("Never")) {
                    checkIfBackupIsOld(lastTime);
                }
            } catch (IOException ex) {
                lblLastBackup.setText("Last Backup: Error");
                AppLogger.error("Database_Backup_Error", ex);
            }
        }
    }
    
    private void checkIfBackupIsOld(String lastTimeStr) {
        try {
            // 1. Define the same formatter used in the BackupService
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm a");
            
            // 2. Parse the string back to a date object
            LocalDateTime lastBackupDateTime = LocalDateTime.parse(lastTimeStr, formatter);
            LocalDateTime now = LocalDateTime.now();

            // 3. Calculate hours between now and the last backup
            long hoursSinceLastBackup = java.time.Duration.between(lastBackupDateTime, now).toHours();

            if (hoursSinceLastBackup >= 24) {
                // WARNING: Backup is old (Red style)
                lblLastBackup.setStyle("-fx-background-color: #f2dede; " + // Light red bg
                                       "-fx-text-fill: #a94442; " +        // Dark red text
                                       "-fx-padding: 3 10; " + 
                                       "-fx-background-radius: 5; " +
                                       "-fx-font-weight: bold;");
                
                // Optional: Add a tooltip to show exactly how many hours/days old it is
                long days = hoursSinceLastBackup / 24;
                lblLastBackup.setTooltip(new Tooltip("Warning: Last backup was " + days + " days ago!"));
            } else {
                // SUCCESS: Backup is fresh (Green style)
                lblLastBackup.setStyle("-fx-background-color: #dff0d8; " + // Light green bg
                                       "-fx-text-fill: #3c763d; " +        // Dark green text
                                       "-fx-padding: 3 10; " + 
                                       "-fx-background-radius: 5; " +
                                       "-fx-font-weight: bold;");
            }
        } catch (Exception e) {
            // If parsing fails (corrupted string), treat as old/error
            lblLastBackup.setText("Last Backup: Format Error");
            lblLastBackup.setStyle("-fx-background-color: #fcf8e3; -fx-text-fill: #8a6d3b;");
            AppLogger.error("Database_Backup_Status_Load_Error", e);
        }
    }
    
    public void setupMainScene(Scene mainScene) {
        sessionManager.initializeIdleTimer(mainScene, () -> {
            // This code runs on auto-logout
            handleLogout();
        });
    }
    
    private void applySecuritySettings() {
        AppUser currentUser = UserSession.getLoggedInUser();
        System.err.println(currentUser.getRole().name());
        if (currentUser != null) {
            // Only visible if Role is ADMIN
            boolean isAdmin = currentUser.getRole() == Role.ADMIN;
            
            vboxAdminMenu.setVisible(isAdmin);
            vboxAdminMenu.setManaged(isAdmin); // 'Managed' ensures the space is reclaimed when hidden
        }
    }
      
    /**
     * This method runs automatically whenever FinancialStatusChangedEvent is published.
     */
    @EventListener
    public void onFinancialStatusChanged(FinancialStatusChangedEvent event) {
        // Run on JavaFX Thread to avoid "Not on FX application thread" errors
        Platform.runLater(() -> {
            updateStatusHeader(); 
            System.out.println("Header refreshed via Event System");
        });
    }
    
    public void loadDashboard() {
    	handleShowDashboard();
    }
    
    @FXML
    public void openAccountBookModule() {
    	loadModule("Account Books", "/fxml/AccountBook.fxml");
    }

    @FXML
    public void openMemberModule() {
        loadModule("Member Management", "/fxml/MemberView.fxml");
    }

    @FXML
    public void openLoanModule() {
        loadModule("Loan Applications", "/fxml/LoanApplicationView.fxml");
    }

    @FXML
    public void openPaymentModule() {
        loadModule("Monthly Collections", "/fxml/CollectionView.fxml");
    }
    
    @FXML
    private void handleShowDashboard() {
        // Logic to load Dashboard.fxml into your main BorderPane Center
    	loadModule("Dashboard", "/fxml/Dashboard.fxml");
    }
    
    @FXML
    public void openReportModule() {
    	loadModule("", "/fxml/FinancialReportsView.fxml");
    }
    
    @FXML private void openLoanPlanning(ActionEvent event) {
        loadModule("", "/fxml/LoanPlanningView.fxml");
    }
    
    @FXML private void openFinancialMonthModule() {
    	loadModule("Financial Months", "/fxml/FinancialMonthView.fxml");
    }
    
    @FXML private void openSettingsModule() {
    	loadModule("Settings", "/fxml/SettingsView.fxml");
    }
    
    @FXML private void openUserManagementModule() {
    	loadModule("Manage Users", "/fxml/ManageUsers.fxml");
    }

    @FXML private void openExpenseModule() {
        loadModule("Manage Expenses", "/fxml/MonthlyExpenseView.fxml");
    }
    
    public void updateStatusHeader() {
        financialMonthService.getActiveMonth().ifPresentOrElse(
            active -> {
                lblHeaderPeriod.setText(active.getMonthId());
                // Professional Green (Emerald)
                lblHeaderPeriod.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; " +
                                         "-fx-padding: 10 35; -fx-font-size: 12; -fx-font-weight: bold; " +
                                         "-fx-background-radius: 0 4 4 0;");
            },
            () -> {
                lblHeaderPeriod.setText("SYSTEM LOCKED");
                // Professional Red (Alizarin)
                lblHeaderPeriod.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                                         "-fx-padding: 10 35; -fx-font-size: 12; -fx-font-weight: bold; " +
                                         "-fx-background-radius: 0 4 4 0;");
            }
        );
    }
    
    private void loadModule(String title, String fxmlPath) {
        // 1. Create a Loader Overlay
        VBox loaderOverlay = new VBox(15);
        loaderOverlay.setAlignment(Pos.CENTER);
        // Dark semi-transparent background to match your theme
        loaderOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.15); -fx-background-radius: 10;");
        
        javafx.scene.control.ProgressIndicator progress = new javafx.scene.control.ProgressIndicator();
        progress.setPrefSize(50, 50);
        // Make the spinner green to match your emerald theme
        progress.setStyle("-fx-progress-color: #27ae60;"); 
        
        Label lblLoading = new Label("Opening " + (title.isEmpty() ? "Module" : title) + "...");
        lblLoading.setStyle("-fx-text-fill: #2c3e50; -fx-font-weight: bold;");
        
        loaderOverlay.getChildren().addAll(progress, lblLoading);
        
        // 2. Add the loader to the content area immediately
        contentArea.getChildren().add(loaderOverlay);

        // 3. Create a Background Task to handle the heavy lifting
        Task<Parent> loadTask = new Task<>() {
            @Override
            protected Parent call() throws Exception {
                // This runs on a background thread
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                loader.setControllerFactory(springContext::getBean);
                Parent view = loader.load();
                
                Platform.runLater(() -> currentActiveController = loader.getController());
                
                if (currentActiveController instanceof Refreshable) {
                    ((Refreshable) currentActiveController).refresh();
                }
                return view;
            }
        };

        // 4. On Success: Replace content area with the loaded view
        loadTask.setOnSucceeded(event -> {
            Parent view = loadTask.getValue();
            contentArea.getChildren().setAll(view);
        });

        // 5. On Failure: Remove loader and show error notification
        loadTask.setOnFailed(event -> {
            contentArea.getChildren().remove(loaderOverlay);
            Throwable e = loadTask.getException();
            e.printStackTrace();
            NotificationManager.show("Error loading " + title, NotificationType.ERROR, Pos.CENTER);
        });

        // Run the task
        Thread thread = new Thread(loadTask);
        thread.setDaemon(true); // Ensure thread closes when app closes
        thread.start();
    }
    
    @FXML
    public void handleLogout() {
        try {
            // Your logout logic
            UserSession.cleanUserSession();
            
            sessionManager.stopTimer();
            
            // Redirect to Login
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();
            
            Stage stage = primaryStageInitializer.getPrimaryStage();
            
            // Instead of new Scene(root), just update the existing one
            Scene scene = stage.getScene();
            if (scene == null) {
                scene = new Scene(root);
                stage.setScene(scene);
            } else {
                scene.setRoot(root);
            }

            stage.setTitle("Society Management System");
            
            // Force maximize
            stage.setMaximized(true);
            stage.show();
            
            
            //NotificationManager.show("Logged out due to inactivity", NotificationType.WARNING, Pos.CENTER);
            
        } catch (IOException e) {
            e.printStackTrace();
            AppLogger.error("Main_Window_Load_Error", e);
        }
    }

    /**
     * Call this to lock the entire screen and show a message
     */
    public void showBlocker(String message) {
        Platform.runLater(() -> {
            lblGlobalStatus.setText(message);
            globalBlocker.setVisible(true);
        });
    }

    /**
     * Call this to unlock the screen
     */
    public void hideBlocker() {
        Platform.runLater(() -> globalBlocker.setVisible(false));
    }

    public void showLoader(String message) {
        Platform.runLater(() -> {
            lblGlobalStatus.setText(message);
            globalBlocker.setVisible(true);
            globalBlocker.toFront(); // CRITICAL: Brings the loader to the very top layer
        });
    }

    public void hideLoader() {
        Platform.runLater(() -> globalBlocker.setVisible(false));
    }

    
    public StackPane getcontentArea() {
    	return contentArea;
    }
    
    public void showReport(File file) {
        if (file == null || !file.exists()) {
            NotificationManager.show("Error: File not found", NotificationType.ERROR, Pos.CENTER);
            return;
        }

        try {
            // 1. Load FXML immediately (This is fast and MUST be on FX thread)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PdfViewer.fxml"));
            loader.setControllerFactory(springContext::getBean);
            Parent view = loader.load(); 
            
            PdfViewerController controller = loader.getController();

            // 2. Show the view immediately (empty or with a loading state)
            contentArea.getChildren().setAll(view);

            // 3. Perform the heavy PDF processing in a Background Task
            Task<Void> pdfTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    // This simulates the 'heavy lifting' of reading the file
                    // If loadPdf reads bytes and converts to Base64, do that here
                    controller.preparePdfData(file); 
                    return null;
                }
            };

            pdfTask.setOnSucceeded(e -> {
                // 4. Finalize the UI display on the FX Thread
                controller.renderPdf();
            });

            new Thread(pdfTask).start();

        } catch (IOException e) {
            AppLogger.error("FX Thread Violation or IO Error", e);
        }
    }


}