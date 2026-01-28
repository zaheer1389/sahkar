package com.badargadh.sahkar.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.badargadh.sahkar.util.AppLogger;

import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;

public abstract class BaseController {
	
    @Autowired protected ApplicationContext springContext;

    protected boolean showSecurityGate(String actionDescription) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SecurityPasswordPopup.fxml"));
            loader.setControllerFactory(springContext::getBean);
            
            Stage stage = new Stage();
            stage.setScene(new javafx.scene.Scene(loader.load()));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.initStyle(javafx.stage.StageStyle.UNDECORATED); // No close/min/max buttons

            SecurityPasswordController controller = loader.getController();
            controller.setActionName(actionDescription);

            stage.showAndWait(); // Execution pauses here until popup closes
            return controller.isAuthorized();
            
        } catch (Exception e) {
        	e.printStackTrace();
            AppLogger.error("Error in Password Confirm!!", e);
            return false;
        }
    }
}