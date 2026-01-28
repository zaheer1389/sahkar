package com.badargadh.sahkar.service;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.*;
import com.badargadh.sahkar.util.AppLogger;

public class MySqlBackupService {

    private final static String APP_HOME = System.getProperty("user.dir");
    private final static String CONFIG_FILE = APP_HOME + File.separator + "application.properties";
    private final static String SETTINGS_PATH = APP_HOME + File.separator + "sahkar_settings.properties";

    private static String dbName, dbUser, dbPass, dbPort, dumpBin, mysqlBin;

    //private static final String CLOUD_PATH = APP_HOME + File.separator + "Google Drive/SocietyBackups/";
    private static final String LOCAL_PATH = APP_HOME + File.separator + "backups" + File.separator;

    private static void loadSettings() {
        Properties prop = new Properties();
        try (InputStream input = new FileInputStream(CONFIG_FILE)) {
            prop.load(input);
            dbName = prop.getProperty("db.name", "society_db");
            dbUser = prop.getProperty("spring.datasource.username", "sahkar");
            dbPass = prop.getProperty("spring.datasource.password", "Sahkar@2026");
            
            String url = prop.getProperty("spring.datasource.url", "");
            dbPort = url.contains("3307") ? "3307" : "3306";

            // Find System Installed MySQL Binaries
            dumpBin = findExecutable("mysqldump");
            mysqlBin = findExecutable("mysql");
            
            AppLogger.info("Database settings loaded. Port: " + dbPort);
        } catch (IOException ex) {
            AppLogger.error("Could not load properties. Using fallbacks.", ex);
            dumpBin = "mysqldump";
            mysqlBin = "mysql";
        }
    }

    private static String findExecutable(String name) {
        String exe = name + (System.getProperty("os.name").toLowerCase().contains("win") ? ".exe" : "");
        // Check system PATH
        try {
            Process p = Runtime.getRuntime().exec(exe + " --version");
            if (p.waitFor() == 0) return exe;
        } catch (Exception ignored) {}

        // Fallback to common Windows install paths
        String[] paths = {
            "C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\",
            "C:\\Program Files\\MySQL\\MySQL Server 8.4\\bin\\",
            "C:\\Program Files\\MySQL\\MySQL Server 5.7\\bin\\"
        };
        for (String path : paths) {
            File file = new File(path + exe);
            if (file.exists()) return file.getAbsolutePath();
        }
        return exe; // Return just the name and hope for the best
    }

    /**
     * BACKUP FUNCTION
     */
    public static boolean performBackup() {
        loadSettings(); 
        AppLogger.info("Starting backup for database: " + dbName);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"));
        String sqlFileName = "db_dump_" + timestamp + ".sql";
        String zipFileName = "Sahkar_Backup_" + timestamp + ".zip";

        try {
            // 1. Ensure Local Backups Directory Exists
            Path localDirPath = Paths.get(LOCAL_PATH);
            if (Files.notExists(localDirPath)) {
                Files.createDirectories(localDirPath);
                AppLogger.info("Created local backup directory: " + LOCAL_PATH);
            }

            // 2. Ensure Cloud Backups Directory Exists
            /*Path cloudPath = Paths.get(CLOUD_PATH);
            if (Files.notExists(cloudPath)) {
                Files.createDirectories(cloudPath);
                AppLogger.info("Created cloud backup directory: " + CLOUD_PATH);
            }*/

            File sqlFile = new File(LOCAL_PATH, sqlFileName);
            File zipFile = new File(LOCAL_PATH, zipFileName);

            // Execute mysqldump
            ProcessBuilder pb = new ProcessBuilder(
                dumpBin, 
                "--port=" + dbPort,
                "-u" + dbUser, 
                "-p" + dbPass, 
                dbName, 
                "--result-file=" + sqlFile.getAbsolutePath()
            );
            
            Process process = pb.start();
            logProcessErrors(process);

            if (process.waitFor() == 0) {
                zipFile(sqlFile, zipFile);
                sqlFile.delete(); // Remove .sql after zipping

                //syncToCloud(zipFile);
                cleanupOldBackups(new File(LOCAL_PATH), 30);
                updateLastBackupTime();
                
                AppLogger.info("Backup successful: " + zipFileName);
                
                return true;
            }
        } catch (Exception e) {
            AppLogger.error("Backup service critical failure", e);
            
            return false;
        }
        
		return false;
    }

    /*private static void syncToCloud(File file) {
        try {
            Path destDir = Paths.get(CLOUD_PATH);
            // Double check/create cloud path just before sync
            if (Files.notExists(destDir)) {
                Files.createDirectories(destDir);
            }
            
            Path destFile = destDir.resolve(file.getName());
            Files.copy(file.toPath(), destFile, StandardCopyOption.REPLACE_EXISTING);
            AppLogger.info("Synced to cloud: " + file.getName());
        } catch (IOException e) {
            AppLogger.error("Cloud sync failed: ", e);
        }
    }*/

    /**
     * RESTORE FUNCTION
     */
    public static boolean performRestore(File selectedZipFile) {
        loadSettings();
        AppLogger.info("Starting restore from: " + selectedZipFile.getName());
        
        File tempSql = new File(LOCAL_PATH, "temp_restore.sql");

        try {
            // 1. Unzip
            unzip(selectedZipFile, tempSql);

            // 2. Execute Restore Command
            // Format: mysql -u user -p pass db_name < file.sql
            ProcessBuilder pb = new ProcessBuilder(
                mysqlBin, "--port=" + dbPort, "-u" + dbUser, "-p" + dbPass, dbName
            );
            pb.redirectInput(tempSql); // Directs SQL file content into MySQL stdin
            
            Process process = pb.start();
            logProcessErrors(process);

            if (process.waitFor() == 0) {
                AppLogger.info("Restore completed successfully.");
                tempSql.delete();
                return true;
            } else {
                AppLogger.error("MySQL Restore process failed.");
            }
        } catch (Exception e) {
            AppLogger.error("Critical failure during restore", e);
        } finally {
            if (tempSql.exists()) tempSql.delete();
        }
        return false;
    }

    private static void logProcessErrors(Process process) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.contains("password on the command line interface")) { // Ignore MySQL warning
                    AppLogger.log("MYSQL", line);
                }
            }
        }
    }

    private static void zipFile(File source, File target) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(target));
             FileInputStream fis = new FileInputStream(source)) {
            zos.putNextEntry(new ZipEntry(source.getName()));
            fis.transferTo(zos);
            zos.closeEntry();
        }
    }

    private static void unzip(File zipSource, File targetSql) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipSource))) {
            ZipEntry entry = zis.getNextEntry();
            if (entry != null) {
                try (FileOutputStream fos = new FileOutputStream(targetSql)) {
                    zis.transferTo(fos);
                }
            }
        }
    }

    private static void cleanupOldBackups(File dir, int keepCount) {
        File[] files = dir.listFiles((d, name) -> name.endsWith(".zip"));
        if (files != null && files.length > keepCount) {
            Arrays.sort(files, Comparator.comparingLong(File::lastModified));
            for (int i = 0; i < files.length - keepCount; i++) files[i].delete();
        }
    }

    public static void updateLastBackupTime() {
        Properties prop = new Properties();
        File file = new File(SETTINGS_PATH);
        try {
            if (file.exists()) try (InputStream in = new FileInputStream(file)) { prop.load(in); }
            prop.setProperty("last.backup", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm a")));
            try (OutputStream out = new FileOutputStream(file)) { prop.store(out, "Settings"); }
        } catch (IOException e) {
            AppLogger.error("Failed to update backup timestamp", e);
        }
    }
}