package com.badargadh.sahkar.util;

public class GujaratiNumericUtils {
	public static String toGujarati(Object input) {
	    if (input == null) return "0";
	    
	    String s;
	    if (input instanceof Number) {
	        // Convert to long to drop any decimal parts (123.45 -> 123)
	        s = String.valueOf(((Number) input).longValue());
	    } else {
	        // If it's already a String, remove decimals using regex
	        s = input.toString().split("\\.")[0];
	    }

	    StringBuilder sb = new StringBuilder();
	    for (char c : s.toCharArray()) {
	        if (Character.isDigit(c)) {
	            sb.append((char) (c - '0' + '\u0AE6'));
	        } else {
	            sb.append(c);
	        }
	    }
	    return sb.toString();
	}
}