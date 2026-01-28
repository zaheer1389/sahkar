package com.badargadh.sahkar.util;

import java.io.File;

public class FileUtil {
	
	public static File getReportOutputFile(String fileName) {
	    // 1. Get the Current Working Directory (CWD)
	    String workingDir = System.getProperty("user.dir");
	    
	    // 2. Define the 'reports' folder path
	    File reportsDir = new File(workingDir, "reports");
	    
	    // 3. Create the directory if it doesn't exist
	    if (!reportsDir.exists()) {
	        boolean created = reportsDir.mkdirs();
	        if (created) {
	            System.out.println("Reports directory created at: " + reportsDir.getAbsolutePath());
	        }
	    }
	    
	    // 4. Return the full path for the specific file
	    return new File(reportsDir, fileName);
	}

}
