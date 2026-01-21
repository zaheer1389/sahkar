package com.badargadh.sahkar.util;

public class GujaratiTransliteration {

    public static String convert(String englishName) {
        if (englishName == null) return "";
        
        // Convert to lowercase for matching
        String input = englishName.toLowerCase().trim();
        
        // Basic Phonetic Mapping (Expand this based on common names in your region)
        String output = input
            .replace("sh", "શ")
            .replace("ch", "ચ")
            .replace("th", "થ")
            .replace("kh", "ખ")
            .replace("bh", "ભ")
            .replace("gh", "ઘ")
            .replace("dh", "ધ")
            .replace("ph", "ફ")
            .replace("a", "ા")
            .replace("i", "િ")
            .replace("u", "ુ")
            .replace("e", "ે")
            .replace("o", "ો")
            .replace("k", "ક")
            .replace("r", "ર")
            .replace("j", "જ")
            .replace("s", "સ")
            .replace("n", "ન")
            .replace("m", "મ")
            .replace("t", "ત")
            .replace("p", "પ")
            .replace("v", "વ")
            .replace("l", "લ");
            
        // Note: Real transliteration requires a complex rule engine.
        // For accurate results in a financial app, suggest adding a 
        // 'gujarati_name' field to the Member profile UI.
        
        return output;
    }
}