package com.badargadh.sahkar;

import com.badargadh.sahkar.util.AppLogger;

import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.fonts.FontFamily;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;

public class Launcher {

    public static void main(String[] args) {
        // 1. Detect environment
        boolean isJarRun = isRunningFromJar();

        if (isJarRun) {
            AppLogger.info("JAR Mode detected: Initializing production security check...");
            
            Properties props = loadProperties();
            String url = props.getProperty("spring.datasource.url", "jdbc:mysql://localhost:3307/society_db");
            String user = props.getProperty("spring.datasource.username", "sahkar");
            String pass = props.getProperty("spring.datasource.password", "Sahkar@2026");

            // Connect to 'mysql' system db to perform user alterations
            String systemUrl = url.substring(0, url.lastIndexOf("/") + 1) + "mysql";
            initializeDatabasePassword(systemUrl, user, pass);
        } else {
            // Skip everything if running manually from Eclipse/IntelliJ
            AppLogger.info("Manual/IDE Mode detected: Skipping automated password synchronization.");
        }

        List<FontFamily> families = DefaultJasperReportsContext.getInstance().getExtensions(FontFamily.class);
    	for (FontFamily family : families) {
    	    System.out.println("Available Font: " + family.getName());
    	}
    	
        // 3. Launch the actual Application
        SocietyManagementApplication.main(args);
    }

    /**
     * Checks if the application is running from a compiled JAR file.
     */
    private static boolean isRunningFromJar() {
        String className = Launcher.class.getName().replace('.', '/') + ".class";
        String classPath = Launcher.class.getClassLoader().getResource(className).toString();
        return classPath.startsWith("jar:");
    }

    private static Properties loadProperties() {
        Properties props = new Properties();
        // Look for application.properties in the current directory
        File configFile = new File("application.properties");
        
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);
            } catch (IOException e) {
                AppLogger.error("Could not read application.properties", e);
            }
        }
        return props;
    }

    public static void initializeDatabasePassword(String dbUrl, String user, String newPassword) {
        // Try to connect with NO password (the state of a fresh integrated MySQL install)
        try (Connection conn = DriverManager.getConnection(dbUrl, user, "")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER USER 'root'@'localhost' IDENTIFIED BY '" + newPassword + "';");
                stmt.execute("FLUSH PRIVILEGES;");
                AppLogger.info("Production Security: Database password synchronized with config.");
            }
        } catch (Exception e) {
            // If connection fails, it means password is already set or server is not on port 3307
            AppLogger.info("Security Check: Database already secured.");
        }
    }
}