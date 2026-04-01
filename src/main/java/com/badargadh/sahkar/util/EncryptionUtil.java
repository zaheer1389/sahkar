package com.badargadh.sahkar.util;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;

public class EncryptionUtil {

	public static String generateChecksum(File file) {
	    if (file == null) return "";
	    try {
	        MessageDigest digest = MessageDigest.getInstance("SHA-256");
	        FileInputStream fis = new FileInputStream(file);
	        
	        byte[] byteArray = new byte[1024];
	        int bytesCount = 0;
	        
	        while ((bytesCount = fis.read(byteArray)) != -1) {
	            digest.update(byteArray, 0, bytesCount);
	        }
	        fis.close();
	        
	        // Convert the hash bytes to Hex format
	        StringBuilder sb = new StringBuilder();
	        for (byte b : digest.digest()) {
	            sb.append(String.format("%02x", b));
	        }
	        return sb.toString();
	        
	    } catch (Exception e) {
	        e.printStackTrace();
	        return "";
	    }
	}
}
