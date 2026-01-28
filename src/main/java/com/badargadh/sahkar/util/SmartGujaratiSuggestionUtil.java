package com.badargadh.sahkar.util;

import java.util.LinkedHashSet;
import java.util.Set;

public final class SmartGujaratiSuggestionUtil {

    private SmartGujaratiSuggestionUtil() {}

    /* =========================
       PUBLIC API
       ========================= */
    public static Set<String> getSuggestions(String englishName) {

        Set<String> gujaratiSuggestions = new LinkedHashSet<>();
        if (englishName == null || englishName.isBlank()) return gujaratiSuggestions;

        englishName = normalize(englishName);

        // 1️⃣ Generate smart English variants
        Set<String> englishVariants = generateEnglishVariants(englishName);

        // 2️⃣ Transliterate each variant to Gujarati
        for (String variant : englishVariants) {
            String guj = GujaratiTransliterator.toGujarati(variant);
            if (guj != null && !guj.isBlank()) {
                gujaratiSuggestions.add(guj);
            }
        }

        return gujaratiSuggestions;
    }

    /* =========================
       ENGLISH VARIANT GENERATION
       ========================= */
    private static Set<String> generateEnglishVariants(String word) {

        Set<String> variants = new LinkedHashSet<>();
        variants.add(word);

        // --- Vowel length ambiguity ---
        variants.add(word.replace("a", "aa"));
        variants.add(word.replace("aa", "a"));

        variants.add(word.replace("i", "ii"));
        variants.add(word.replace("ii", "i"));

        variants.add(word.replace("u", "uu"));
        variants.add(word.replace("uu", "u"));

        variants.add(word.replace("ee", "i"));
        variants.add(word.replace("oo", "u"));

        // --- Arabic / Urdu name patterns ---
        addArabicPatterns(word, variants);

        // --- H sound ambiguity ---
        if (word.contains("h")) {
            variants.add(word.replace("h", ""));
            variants.add(word.replace("h", "hh"));
        }

        // --- Clean invalid variants ---
        variants.removeIf(SmartGujaratiSuggestionUtil::isInvalidVariant);

        return variants;
    }

    /* =========================
       ARABIC / URDU PATTERNS
       ========================= */
    private static void addArabicPatterns(String word, Set<String> variants) {

        // heer / hir / hoor / hur
        if (word.contains("heer")) {
            variants.add(word.replace("heer", "hir"));
            variants.add(word.replace("heer", "hoor"));
            variants.add(word.replace("heer", "hur"));
        }

        if (word.contains("hir")) {
            variants.add(word.replace("hir", "heer"));
            variants.add(word.replace("hir", "hoor"));
            variants.add(word.replace("hir", "hur"));
        }

        if (word.contains("hoor")) {
            variants.add(word.replace("hoor", "heer"));
            variants.add(word.replace("hoor", "hir"));
            variants.add(word.replace("hoor", "hur"));
        }

        if (word.contains("hur")) {
            variants.add(word.replace("hur", "heer"));
            variants.add(word.replace("hur", "hir"));
            variants.add(word.replace("hur", "hoor"));
        }

        // saad / saad-like (aa ↔ a)
        if (word.endsWith("ad") || word.endsWith("ab")) {
            variants.add(word.replace("ad", "aad"));
            variants.add(word.replace("ab", "aab"));
        }
    }

    /* =========================
       HELPERS
       ========================= */
    private static String normalize(String s) {
        return s.toLowerCase()
                .replaceAll("[^a-z]", "")
                .trim();
    }

    private static boolean isInvalidVariant(String v) {
        return v.isBlank()
            || v.contains("aaaa")
            || v.contains("iiii")
            || v.contains("uuuu");
    }
}
