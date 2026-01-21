package com.badargadh.sahkar.util;

import com.ibm.icu.text.Transliterator;

public class GujaratiMapper {
    public static String convert(String englishName) {
        // "Latin-Gujarati" is the built-in rule for phonetic conversion
        Transliterator transliterator = Transliterator.getInstance("Latin-Gujarati");
        return transliterator.transliterate(englishName);
    }
}

// Example: "Rajesh" -> "રાજેશ"