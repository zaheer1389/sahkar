package com.badargadh.sahkar.util;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class NotificationManager {

    public enum NotificationType {
        SUCCESS("#27ae60"), ERROR("#e74c3c"), INFO("#2980b9"), WARNING("#ffcc00");
        final String color;
        NotificationType(String color) { this.color = color; }
    }

    public static void show(String message, NotificationType type, Pos position) {
        Stage toastStage = new Stage();
        toastStage.initStyle(StageStyle.TRANSPARENT);

        // UI Design
        Label label = new Label(message);
        label.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 15; -fx-font-size: 14;");
        
        HBox container = new HBox(label);
        container.setStyle("-fx-background-color: " + type.color + "; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 5);");

        Scene scene = new Scene(container);
        scene.setFill(Color.TRANSPARENT);
        toastStage.setScene(scene);

        // Dynamic Positioning Logic
        toastStage.setOnShown(e -> {
            // Get current screen bounds or main window bounds
        	// 1. First, make the stage calculate its own size
        	toastStage.show(); 

        	// 2. Get screen bounds
        	Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
        	double screenWidth = visualBounds.getWidth();
        	double screenHeight = visualBounds.getHeight();

        	// 3. Get ACTUAL stage dimensions (now that it is shown)
        	double toastWidth = toastStage.getWidth();
        	double toastHeight = toastStage.getHeight();

        	double x = 0, y = 0;

        	// 4. Use break statements to prevent fall-through
        	switch (position) {
        	    case TOP_RIGHT:
        	        x = screenWidth - toastWidth - 20;
        	        y = 50;
        	        break;
        	    case BOTTOM_RIGHT:
        	        x = screenWidth - toastWidth - 20;
        	        y = screenHeight - toastHeight - 50;
        	        break;
        	    case BOTTOM_CENTER:
        	        x = (screenWidth - toastWidth) / 2;
        	        y = screenHeight - toastHeight - 50;
        	        break;
        	    case TOP_CENTER:
        	        x = (screenWidth - toastWidth) / 2;
        	        y = 50;
        	        break;
        	    case CENTER:
        	        x = (screenWidth - toastWidth) / 2;
        	        y = (screenHeight - toastHeight) / 2;
        	        break;
        	    default: // TOP_LEFT
        	        x = 20;
        	        y = 50;
        	        break;
        	}

        	// 5. Update position
        	toastStage.setX(x);
        	toastStage.setY(y);
        });

        // Animations
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), container);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        PauseTransition delay = new PauseTransition(Duration.seconds(3));
        
        FadeTransition fadeOut = new FadeTransition(Duration.millis(500), container);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> toastStage.close());

        toastStage.show();
        fadeIn.play();
        fadeIn.setOnFinished(e -> delay.play());
        delay.setOnFinished(e -> fadeOut.play());
    }
}