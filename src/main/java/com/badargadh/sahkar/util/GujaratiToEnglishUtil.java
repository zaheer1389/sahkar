package com.badargadh.sahkar.util;

import java.util.HashMap;
import java.util.Map;

public final class GujaratiToEnglishUtil {

    private GujaratiToEnglishUtil() {}

    /* =========================
       INDEPENDENT VOWELS
       ========================= */
    private static final Map<Character, String> VOWELS = new HashMap<>();
    static {
        VOWELS.put('અ', "a");
        VOWELS.put('આ', "aa");
        VOWELS.put('ઇ', "i");
        VOWELS.put('ઈ', "ii");
        VOWELS.put('ઉ', "u");
        VOWELS.put('ઊ', "uu");
        VOWELS.put('એ', "e");
        VOWELS.put('ઐ', "ai");
        VOWELS.put('ઓ', "o");
        VOWELS.put('ઔ', "au");
    }

    /* =========================
       CONSONANTS (base sounds)
       ========================= */
    private static final Map<Character, String> CONSONANTS = new HashMap<>();
    static {
        CONSONANTS.put('ક',"k");   CONSONANTS.put('ખ',"kh");  CONSONANTS.put('ગ',"g");   CONSONANTS.put('ઘ',"gh");
        CONSONANTS.put('ચ',"ch");  CONSONANTS.put('છ',"chh"); CONSONANTS.put('જ',"j");   CONSONANTS.put('ઝ',"jh");
        CONSONANTS.put('ઝ',"za");
        CONSONANTS.put('ટ',"t");   CONSONANTS.put('ઠ',"th");  CONSONANTS.put('ડ',"d");   CONSONANTS.put('ઢ',"dh");
        CONSONANTS.put('ત',"t");   CONSONANTS.put('થ',"th");  CONSONANTS.put('દ',"d");   CONSONANTS.put('ધ',"dh");
        CONSONANTS.put('પ',"p");   CONSONANTS.put('ફ',"f");   CONSONANTS.put('બ',"b");   CONSONANTS.put('ભ',"bh");
        CONSONANTS.put('મ',"m");   CONSONANTS.put('ય',"y");   CONSONANTS.put('ર',"r");   CONSONANTS.put('લ',"l");
        CONSONANTS.put('વ',"v");   CONSONANTS.put('શ',"sh");  CONSONANTS.put('ષ',"sh");  CONSONANTS.put('સ',"s");
        CONSONANTS.put('હ',"h");   CONSONANTS.put('ળ',"l");
        CONSONANTS.put('ણ',"n");   CONSONANTS.put('ન',"n");
    }

    /* =========================
       MATRAS (override vowel)
       ========================= */
    private static final Map<Character, String> MATRAS = new HashMap<>();
    static {
        MATRAS.put('ા', "aa");
        MATRAS.put('િ', "i");
        MATRAS.put('ી', "ii");
        MATRAS.put('ુ', "u");
        MATRAS.put('ૂ', "uu");
        MATRAS.put('ે', "e");
        MATRAS.put('ૈ', "ai");
        MATRAS.put('ો', "o");
        MATRAS.put('ૌ', "au");
        MATRAS.put('્', "");     // halant
    }

    /* =========================
       DIACRITICS
       ========================= */
    private static final Map<Character, String> DIACRITICS = new HashMap<>();
    static {
        DIACRITICS.put('ં', "n");   // anusvara (nasal)
        DIACRITICS.put('ઁ', "n");   // chandrabindu
        DIACRITICS.put('ઃ', "h");   // visarga
    }

    /* =========================
       MAIN TRANSLITERATION
       ========================= */
    public static String toEnglish(String gujarati) {

        if (gujarati == null || gujarati.isBlank()) return "";

        StringBuilder out = new StringBuilder();
        char[] chars = gujarati.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            char ch = chars[i];

            // Diacritics
            if (DIACRITICS.containsKey(ch)) {
                out.append(DIACRITICS.get(ch));
                continue;
            }

            // Independent vowel
            if (VOWELS.containsKey(ch)) {
                out.append(VOWELS.get(ch));
                continue;
            }

            // Consonant
            if (CONSONANTS.containsKey(ch)) {
                String base = CONSONANTS.get(ch);
                String vowel = "a"; // implicit a

                // Lookahead for matra
                if (i + 1 < chars.length && MATRAS.containsKey(chars[i + 1])) {
                    vowel = MATRAS.get(chars[i + 1]);
                    i++; // consume matra
                }

                out.append(base).append(vowel);
            }
        }

        return out.toString();
    }
}
