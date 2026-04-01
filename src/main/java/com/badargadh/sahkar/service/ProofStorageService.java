package com.badargadh.sahkar.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.badargadh.sahkar.enums.ProofType;

@Service
public class ProofStorageService {
	
	@Autowired
	private AppConfigService appConfigService;

    /**
     * Stores the scanned letter in an organized directory structure.
     * * @param memberId The ID of the member
     * @param scannedFile The temporary file from the scanner
     * @param isLoanProof True for Loan Collection, False for Fee Refund
     * @return The absolute path of the stored file to save in the Database
     */
    public String storeProofLetter(Integer memberId, File scannedFile, ProofType proofType) throws IOException {
        if (scannedFile == null || !scannedFile.exists()) {
            throw new IOException("Scanned file not found.");
        }
        
        String baseDirPath = appConfigService.getSettings().getStoragePath();
        if(baseDirPath == null || baseDirPath.isEmpty()) {
        	throw new IOException("Storage directory not exist.");
        }
        
        Path path = Paths.get(baseDirPath);
        if (!Files.isDirectory(path)) {
        	throw new IOException("Storage directory not exist.");
        }

        // 1. Get current Year and Month name
        LocalDate now = LocalDate.now();
        String year = String.valueOf(now.getYear());
        String month = now.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);

        // 2. Build the directory path: BasePath/Year/Month
        Path targetDirectory = Paths.get(baseDirPath, "Proofs", year, month);

        // Create directories if they don't exist
        if (Files.notExists(targetDirectory)) {
            Files.createDirectories(targetDirectory);
        }

        // 3. Create a descriptive file name
        String prefix = proofType.name()+"_";
        String fileName = prefix + memberId + "_" + System.currentTimeMillis() + getFileExtension(scannedFile);
        
        Path targetPath = targetDirectory.resolve(fileName);

        // 4. Move file from scanner's temp location to our secure storage
        Files.copy(scannedFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        // Return the path so you can store it in the DB
        return targetPath.toString();
    }

    private String getFileExtension(File file) {
        String name = file.getName();
        int lastIndexOf = name.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return ".jpg"; // Default extension
        }
        return name.substring(lastIndexOf);
    }
}