package com.badargadh.sahkar.util;

import java.util.*;

public class GujaratiNameEngine {
    private static final Map<String, String> CONSONANTS = new LinkedHashMap<>();
    private static final Map<String, String> VOWELS = new LinkedHashMap<>();
    private static final String VIRAMA = "્"; // The Halant

    static {
        // Order matters: place multi-letter combinations first
        CONSONANTS.put("kh", "ખ"); CONSONANTS.put("gh", "ઘ");
        CONSONANTS.put("ch", "ચ"); CONSONANTS.put("chh", "છ");
        CONSONANTS.put("jh", "ઝ"); CONSONANTS.put("th", "થ");
        CONSONANTS.put("dh", "ધ"); CONSONANTS.put("ph", "ફ");
        CONSONANTS.put("bh", "ભ"); CONSONANTS.put("sh", "શ");
        CONSONANTS.put("k", "ક");  CONSONANTS.put("g", "ગ");
        CONSONANTS.put("j", "જ");  CONSONANTS.put("t", "ત");
        CONSONANTS.put("d", "દ");  CONSONANTS.put("n", "ન");
        CONSONANTS.put("p", "પ");  CONSONANTS.put("b", "બ");
        CONSONANTS.put("m", "મ");  CONSONANTS.put("y", "ય");
        CONSONANTS.put("r", "ર");  CONSONANTS.put("l", "લ");
        CONSONANTS.put("v", "વ");  CONSONANTS.put("s", "સ");
        CONSONANTS.put("h", "હ");
        
        // Vowel Marks (Dependent)
        VOWELS.put("aa", "ા"); VOWELS.put("ai", "ૈ");
        VOWELS.put("ee", "ી"); VOWELS.put("oo", "ૂ");
        VOWELS.put("au", "ૌ"); VOWELS.put("a", ""); // Inherent 'a'
        VOWELS.put("i", "િ");  VOWELS.put("u", "ુ");
        VOWELS.put("e", "ે");  VOWELS.put("o", "ો");
    }

    public static String transliterate(String input) {
        if (input == null || input.isEmpty()) return "";
        
        StringBuilder output = new StringBuilder();
        String text = input.toLowerCase();
        int i = 0;

        while (i < text.length()) {
            boolean matched = false;

            // 1. Try matching Consonants (Check 3-char, then 2, then 1)
            for (int len = 3; len >= 1; len--) {
                if (i + len <= text.length()) {
                    String sub = text.substring(i, i + len);
                    if (CONSONANTS.containsKey(sub)) {
                        output.append(CONSONANTS.get(sub));
                        i += len;
                        
                        // Look ahead for a vowel
                        boolean vowelFound = false;
                        for (int vLen = 2; vLen >= 1; vLen--) {
                            if (i + vLen <= text.length()) {
                                String vSub = text.substring(i, i + vLen);
                                if (VOWELS.containsKey(vSub)) {
                                    output.append(VOWELS.get(vSub));
                                    i += vLen;
                                    vowelFound = true;
                                    break;
                                }
                            }
                        }
                        // If no vowel follows a consonant, add a Halant (Virama)
                        // Note: At the end of a name, we usually skip the Halant in Gujarati.
                        if (!vowelFound && i < text.length()) {
                            output.append(VIRAMA);
                        }
                        matched = true;
                        break;
                    }
                }
            }

            // 2. Handle standalone vowels (at the start of a name)
            if (!matched) {
                // Simplified mapping for independent vowels like 'A' in 'Amit'
                if (text.startsWith("a", i)) { output.append("અ"); i++; }
                else if (text.startsWith("i", i)) { output.append("ઇ"); i++; }
                else { i++; } // Skip unknown
            }
        }
        return output.toString();
    }

}