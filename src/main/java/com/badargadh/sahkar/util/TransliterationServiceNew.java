package com.badargadh.sahkar.util;

import com.ibm.icu.text.Transliterator;

public class TransliterationServiceNew {
	// "Any-Gujarati" or "Latin-Gujarati" is the ID for English to Gujarati
    private static final String GUJARATI_ID = "Any-Gujarati";

    public static String convertToGujarati(String englishName) {
        if (englishName == null || englishName.isEmpty()) return "";
        Transliterator transliterator = Transliterator.getInstance(GUJARATI_ID);
        return transliterator.transliterate(englishName);
    }
}
