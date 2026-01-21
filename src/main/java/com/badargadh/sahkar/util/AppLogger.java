package com.badargadh.sahkar.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AppLogger {
    private static final String LOG_FILE = System.getProperty("user.dir") + File.separator + "sahkar_app.log";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void log(String level, String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        String logEntry = String.format("[%s] %s: %s", timestamp, level.toUpperCase(), message);

        // Print to console for development
        System.out.println(logEntry);

        // Write to file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE, true))) {
            writer.write(logEntry);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Could not write to log file: " + e.getMessage());
        }
    }

    public static void error(String message, Exception e) {
        log("ERROR", message + " | Exception: " + e.getMessage());
        if (e.getStackTrace().length > 0) {
            log("DEBUG", "Stacktrace: " + e.getStackTrace()[0].toString());
        }
    }
    
    public static void error(String message) {
        log("ERROR", message );
    }
    
    public static void info(String message) {
        log("INFO", message);
    }
}