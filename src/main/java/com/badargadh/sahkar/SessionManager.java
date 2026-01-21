package com.badargadh.sahkar;

import org.springframework.stereotype.Component;

import com.badargadh.sahkar.util.AppLogger;

import javafx.animation.PauseTransition;
import javafx.scene.Scene;
import javafx.util.Duration;

@Component
public class SessionManager {

    private PauseTransition idleTimer;
    private final int IDLE_TIME_MINUTES = 5;

    public void initializeIdleTimer(Scene scene, Runnable onLogout) {
        // 1. Create a timer for 5 minutes
        idleTimer = new PauseTransition(Duration.minutes(IDLE_TIME_MINUTES));
        
        // 2. Define what happens when the timer finishes
        idleTimer.setOnFinished(event -> {
            AppLogger.info("User idle for " + IDLE_TIME_MINUTES + " minutes. Triggering auto-logout.");
            onLogout.run();
        });

        // 3. Add Event Filters to the main scene to catch ALL user activity
        scene.addEventFilter(javafx.scene.input.MouseEvent.ANY, e -> resetTimer());
        scene.addEventFilter(javafx.scene.input.KeyEvent.ANY, e -> resetTimer());
        scene.addEventFilter(javafx.scene.input.ScrollEvent.ANY, e -> resetTimer());

        // Start the timer initially
        idleTimer.play();
    }

    private void resetTimer() {
        if (idleTimer != null) {
            idleTimer.playFromStart();
        }
    }
    
    public void stopTimer() {
        if (idleTimer != null) {
            idleTimer.stop();
        }
    }
}