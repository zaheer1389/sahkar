package com.badargadh.sahkar.util;

import java.util.LinkedHashMap;
import java.util.Map;

public final class GujaratiTransliterator {

    private GujaratiTransliterator() {}

    /* =========================
       SPECIAL OVERRIDES
       ========================= */
    private static final Map<String, String> SPECIAL = new LinkedHashMap<>();
    static {
        SPECIAL.put("imtiyaz", "ઇમ્તીયાઝ");
        SPECIAL.put("riyaz", "રિયાઝ");

        SPECIAL.put("tiya", "તિયા");
        SPECIAL.put("diya", "દિયા");
        SPECIAL.put("riya", "રિયા");
        SPECIAL.put("ziya", "ઝિયા");
        SPECIAL.put("liya", "લિયા");
        SPECIAL.put("miya", "મિયા");
        SPECIAL.put("niya", "નિયા");
    }

    /* =========================
       INDEPENDENT VOWELS
       ========================= */
    private static final Map<String, String> VOWELS = Map.ofEntries(
        Map.entry("aa","આ"),
        Map.entry("ii","ઈ"),
        Map.entry("ee","ઈ"),
        Map.entry("uu","ઊ"),
        Map.entry("ai","ઐ"),
        Map.entry("au","ઔ"),
        Map.entry("a","અ"),
        Map.entry("i","ઇ"),
        Map.entry("e","એ"),
        Map.entry("u","ઉ"),
        Map.entry("o","ઓ")
    );

    /* =========================
       MATRAS (IMPORTANT RULE)
       - NO matra for single 'a'
       - long vowel ONLY when doubled
       ========================= */
    private static final Map<String, String> MATRAS = Map.ofEntries(
        Map.entry("aa","ા"),
        Map.entry("i","િ"),
        Map.entry("ii","ી"),
        Map.entry("ee","ી"),
        Map.entry("u","ુ"),
        Map.entry("uu","ૂ"),
        Map.entry("e","ે"),
        Map.entry("ai","ૈ"),
        Map.entry("o","ો"),
        Map.entry("au","ૌ")
    );

    /* =========================
       CONSONANTS (ORDER MATTERS)
       ========================= */
    private static final Map<String, String> CONSONANTS = new LinkedHashMap<>();
    static {
        // Conjuncts
        CONSONANTS.put("ksh","ક્ષ");
        CONSONANTS.put("gya","જ્ઞ");
        CONSONANTS.put("shr","શ્ર");
        CONSONANTS.put("tra","ત્ર");

        // Digraphs
        CONSONANTS.put("sh","શ");   // must come before s
        CONSONANTS.put("chh","છ");
        CONSONANTS.put("ch","ચ");
        CONSONANTS.put("jh","ઝ");
        CONSONANTS.put("kh","ખ");
        CONSONANTS.put("gh","ઘ");
        CONSONANTS.put("th","થ");
        CONSONANTS.put("dh","ધ");
        CONSONANTS.put("ph","ફ");
        CONSONANTS.put("bh","ભ");

        // Persian / Arabic
        CONSONANTS.put("z","ઝ");
        CONSONANTS.put("f","ફ");
        CONSONANTS.put("q","ક");

        // Doubles
        CONSONANTS.put("nn","ન્ન");
        CONSONANTS.put("ll","લ્લ");
        CONSONANTS.put("tt","ટ્ટ");
        CONSONANTS.put("dd","ડ્ડ");

        // Singles
        CONSONANTS.put("s","સ");
        CONSONANTS.put("k","ક");
        CONSONANTS.put("g","ગ");
        CONSONANTS.put("c","ક");
        CONSONANTS.put("j","જ");
        CONSONANTS.put("t","ત");
        CONSONANTS.put("d","દ");
        CONSONANTS.put("n","ન");
        CONSONANTS.put("p","પ");
        CONSONANTS.put("b","બ");
        CONSONANTS.put("m","મ");
        CONSONANTS.put("y","ય");
        CONSONANTS.put("r","ર");
        CONSONANTS.put("l","લ");
        CONSONANTS.put("v","વ");
        CONSONANTS.put("w","વ");
        CONSONANTS.put("h","હ");
    }

    /* =========================
       MAIN TRANSLITERATION
       ========================= */
    public static String toGujarati(String english) {
        if (english == null || english.isBlank()) return "";

        String text = english.toLowerCase();
        StringBuilder out = new StringBuilder();

        for (int i = 0; i < text.length();) {

            if (Character.isWhitespace(text.charAt(i))) {
                out.append(" ");
                i++;
                continue;
            }

            boolean matched = false;

            // 1️⃣ Special overrides
            for (String key : SPECIAL.keySet()) {
                if (text.startsWith(key, i)) {
                    out.append(SPECIAL.get(key));
                    i += key.length();
                    matched = true;
                    break;
                }
            }
            if (matched) continue;

            // 2️⃣ Consonant + matra
            for (String cKey : CONSONANTS.keySet()) {
                if (text.startsWith(cKey, i)) {

                    String consonant = CONSONANTS.get(cKey);
                    i += cKey.length();

                    String matra = "";
                    for (String vKey : MATRAS.keySet()) {
                        if (text.startsWith(vKey, i)) {
                            matra = MATRAS.get(vKey);
                            i += vKey.length();
                            break;
                        }
                    }

                    out.append(consonant).append(matra);
                    matched = true;
                    break;
                }
            }
            if (matched) continue;

            // 3️⃣ Standalone vowel
            for (String vKey : VOWELS.keySet()) {
                if (text.startsWith(vKey, i)) {
                    out.append(VOWELS.get(vKey));
                    i += vKey.length();
                    matched = true;
                    break;
                }
            }

            if (!matched) i++;
        }

        return out.toString();
    }
    
    
}
