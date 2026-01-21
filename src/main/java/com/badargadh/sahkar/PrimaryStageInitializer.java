package com.badargadh.sahkar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

@Component
public class PrimaryStageInitializer implements ApplicationListener<StageReadyEvent> {

    @Autowired private ApplicationContext context;
    
    private Stage primaryStage;

    public PrimaryStageInitializer() {
    	
    }

    @Override
    public void onApplicationEvent(StageReadyEvent event) {
        try {
            // Get the stage from the event
        	primaryStage = event.getStage();
        	primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/static/images/icon.png")));
            
            // Load Login FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            
            // This line allows Spring to inject Repositories into JavaFX Controllers
            loader.setControllerFactory(context::getBean);
            
            Parent root = loader.load();
            primaryStage.setScene(new Scene(root));
            primaryStage.setTitle("Society Management System - Login");
            primaryStage.centerOnScreen();
            primaryStage.setMaximized(true);
            primaryStage.show();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public Stage getPrimaryStage() {
    	return primaryStage;
    }
}