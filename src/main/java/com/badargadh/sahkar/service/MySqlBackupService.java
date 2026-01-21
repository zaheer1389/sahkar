package com.badargadh.sahkar.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.badargadh.sahkar.util.AppLogger;

public class MySqlBackupService {

	private final static String APP_HOME = System.getProperty("user.dir");
    private final static String CONFIG_FILE = APP_HOME + File.separator + "application.properties";
    private final static String SETTINGS_PATH = APP_HOME + File.separator + "sahkar_settings.properties";

    // Variables loaded from file
    private static String dbName;
    private static String dbUser;
    private static String dbPass;
    private static String dbPort;
    private static String mysqlBin;

    private static final String CLOUD_PATH = APP_HOME + File.separator + "Google Drive/SocietyBackups/";
    private static final String LOCAL_PATH = APP_HOME + File.separator + "backups" + File.separator;

    private static void loadSettings() {
        Properties prop = new Properties();
        try (InputStream input = new FileInputStream(CONFIG_FILE)) {
            prop.load(input);
            
            // Extract values (using defaults if keys are missing)
            dbName = prop.getProperty("db.name", "society_db");
            dbUser = prop.getProperty("spring.datasource.username", "root");
            dbPass = prop.getProperty("spring.datasource.password", "$sahkarbadargadh$");
            
            // Extract port from the JDBC URL (e.g., localhost:3307)
            String url = prop.getProperty("spring.datasource.url", "3307");
            dbPort = url.contains("3307") ? "3307" : "3306"; 

            mysqlBin = APP_HOME + File.separator + "mysql" + File.separator + "bin" + File.separator + "mysqldump.exe";
            
            AppLogger.info("Database settings loaded from application.properties");
        } catch (IOException ex) {
            AppLogger.error("Could not load application.properties. Using hardcoded defaults.", ex);
            // Fallbacks
            dbName = "society_db";
            dbUser = "root";
            dbPass = "$sahkarbadargadh$";
            dbPort = "3307";
            mysqlBin = APP_HOME + File.separator + "mysql" + File.separator + "bin" + File.separator + "mysqldump.exe";
        }
    }

    public static void performBackup() {
        loadSettings(); // Ensure we have latest settings
        AppLogger.info("Starting backup for database: " + dbName);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"));
        String sqlFileName = "db_dump_" + timestamp + ".sql";
        String zipFileName = "Sahkar_Backup_" + timestamp + ".zip";

        File localDir = new File(LOCAL_PATH);
        if (!localDir.exists()) localDir.mkdirs();

        File sqlFile = new File(localDir, sqlFileName);
        File zipFile = new File(localDir, zipFileName);

        try {
            ProcessBuilder pb = new ProcessBuilder(
                mysqlBin, 
                "--port=" + dbPort,
                "-u" + dbUser, 
                "-p" + dbPass, 
                dbName, 
                "--result-file=" + sqlFile.getAbsolutePath()
            );
            
            Process process = pb.start();

            // Capture error stream for the app log
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String line;
            while ((line = reader.readLine()) != null) { AppLogger.log("MYSQL", line); }

            if (process.waitFor() == 0) {
            	// 2. Compress to ZIP
                zipFile(sqlFile, zipFile);
                
                // 3. Delete bulky .sql
                sqlFile.delete();

                // 4. Sync to Cloud
                syncToCloud(zipFile);
                
                // 5. Cleanup
                cleanupOldBackups(localDir, 30);
                
                updateLastBackupTime();
                
                AppLogger.info("Backup successful: " + zipFileName);
            } else {
                AppLogger.log("ERROR", "mysqldump failed. Check sahkar_app.log for MySQL errors.");
            }
        } catch (Exception e) {
            AppLogger.error("Backup service critical failure", e);
        }
    }

    private static void zipFile(File source, File target) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(target));
             FileInputStream fis = new FileInputStream(source)) {
            zos.putNextEntry(new ZipEntry(source.getName()));
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) > 0) zos.write(buffer, 0, len);
            zos.closeEntry();
        }
    }

    private static void syncToCloud(File file) {
        try {
            File cloudDestDir = new File(CLOUD_PATH);
            if (!cloudDestDir.exists()) cloudDestDir.mkdirs();
            
            File cloudDest = new File(cloudDestDir, file.getName());
            Files.copy(file.toPath(), cloudDest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
        	AppLogger.error("Cloud sync failed (Path not found): ", e);
        }
    }

    private static void cleanupOldBackups(File dir, int keepCount) {
        File[] files = dir.listFiles((d, name) -> name.endsWith(".zip"));
        if (files != null && files.length > keepCount) {
            java.util.Arrays.sort(files, (f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()));
            for (int i = 0; i < files.length - keepCount; i++) files[i].delete();
        }
    }
    
    public static void updateLastBackupTime() {
        File file = new File(SETTINGS_PATH);
        Properties prop = new Properties();

        if (file.exists()) {
            try (InputStream input = new FileInputStream(file)) {
                prop.load(input);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm a"));
        prop.setProperty("last.backup", now);

        try (OutputStream output = new FileOutputStream(file)) {
            prop.store(output, "System Settings - Integrated MySQL");
        } catch (IOException io) {
            io.printStackTrace();
        }
    }
}