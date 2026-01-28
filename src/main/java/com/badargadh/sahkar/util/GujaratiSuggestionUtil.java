package com.badargadh.sahkar.util;

import java.util.LinkedHashSet;
import java.util.Set;

public final class GujaratiSuggestionUtil {

    private GujaratiSuggestionUtil() {}

    /**
     * Generate all reasonable Gujarati spellings for a name.
     */
    public static Set<String> getSuggestions(String english) {

        Set<String> suggestions = new LinkedHashSet<>();
        if (english == null || english.isBlank()) return suggestions;

        english = english.toLowerCase().trim();

        // 1️⃣ Base transliteration (short a default)
        suggestions.add(GujaratiTransliterator.toGujarati(english));

        // 2️⃣ Long vowel variants (aa, ii, uu)
        suggestions.add(
            GujaratiTransliterator.toGujarati(
                english.replace("a", "aa")
            )
        );

        // 3️⃣ Explicit 'અ' insertion (Arabic names)
        // saad → sa'ad → سَأَد style
        if (english.contains("aa") || english.endsWith("ad") || english.endsWith("ab")) {
            suggestions.add(
                insertGujaratiA(
                    GujaratiTransliterator.toGujarati(english)
                )
            );
        }

        // 4️⃣ h sound ambiguity (zaheer)
        if (english.contains("h")) {
            suggestions.add(
                GujaratiTransliterator.toGujarati(
                    english.replace("h", "")
                )
            );
            suggestions.add(
                GujaratiTransliterator.toGujarati(
                    english.replace("h", "hh")
                )
            );
        }

        // 5️⃣ Remove duplicates & blanks
        suggestions.removeIf(s -> s == null || s.isBlank());

        return suggestions;
    }

    /**
     * Insert explicit 'અ' after first consonant
     * સાદ → સાઅદ, સઅદ
     */
    private static String insertGujaratiA(String guj) {
        if (guj.length() < 2) return guj;
        return guj.substring(0, 1) + "અ" + guj.substring(1);
    }
}
