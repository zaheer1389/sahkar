package com.badargadh.sahkar.util;


import java.util.*;

public class FullBarakhadiEngine {
    private static final Map<String, String> CONSONANTS = new LinkedHashMap<>();
    private static final Map<String, List<String>> MATRAS = new LinkedHashMap<>();
    private static final Map<String, List<String>> IND_VOWELS = new LinkedHashMap<>();

    static {
        // 1. Consonants
    	// Adding 'dd' specifically for names like Salauddin
        CONSONANTS.put("dd", "દ્દ");
        CONSONANTS.put("kh", "ખ"); CONSONANTS.put("gh", "ઘ"); CONSONANTS.put("chh", "છ");
        CONSONANTS.put("ch", "ચ"); CONSONANTS.put("jh", "ઝ"); CONSONANTS.put("th", "થ");
        CONSONANTS.put("dh", "ધ"); CONSONANTS.put("ph", "ફ"); CONSONANTS.put("bh", "ભ");
        CONSONANTS.put("sh", "શ"); CONSONANTS.put("k", "ક");  CONSONANTS.put("g", "ગ");
        CONSONANTS.put("j", "જ");  CONSONANTS.put("t", "ત");  CONSONANTS.put("d", "દ");
        CONSONANTS.put("n", "ન");  CONSONANTS.put("p", "પ");  CONSONANTS.put("b", "બ");
        CONSONANTS.put("m", "મ");  CONSONANTS.put("y", "ય");  CONSONANTS.put("r", "ર");
        CONSONANTS.put("l", "લ");  CONSONANTS.put("v", "વ");  CONSONANTS.put("s", "સ");
        CONSONANTS.put("h", "હ");  CONSONANTS.put("z", "ઝ");

        // 2. Matras - Using Arrays.asList to allow multiple suggestions
        addMatra("aa", "ા");
        addMatra("a", "ા"); 
        addMatra("a", ""); // Inherent 'a'
        addMatra("i", "િ"); addMatra("i", "ી");
        addMatra("ee", "િ"); addMatra("ee", "ી");
        addMatra("u", "ુ"); addMatra("u", "ૂ");
        addMatra("oo", "ુ"); addMatra("oo", "ૂ");
        addMatra("e", "ે");  addMatra("ai", "ૈ");  addMatra("o", "ો");
        addMatra("au", "ૌ"); addMatra("an", "ં");

        // 3. Independent Vowels
        addIndVowel("a", "અ"); addIndVowel("a", "આ");
        addIndVowel("aa", "આ");
        addIndVowel("i", "ઇ"); addIndVowel("i", "ઈ");
        addIndVowel("ee", "ઈ");
        addIndVowel("u", "ઉ"); addIndVowel("u", "ઊ");
        addIndVowel("e", "એ");  addIndVowel("o", "ઓ");
    }

    private static void addMatra(String key, String val) {
        MATRAS.computeIfAbsent(key, k -> new ArrayList<>()).add(val);
    }

    private static void addIndVowel(String key, String val) {
        IND_VOWELS.computeIfAbsent(key, k -> new ArrayList<>()).add(val);
    }

    public static Set<String> getSuggestions(String input) {
        Set<String> results = new TreeSet<>(); 
        solve(input.toLowerCase(), "", results);
        return results;
    }

    private static void solve(String rem, String curr, Set<String> res) {
        if (rem.isEmpty()) {
            res.add(curr);
            return;
        }

        // --- Logic A: Consonants + Matras ---
        for (String cKey : CONSONANTS.keySet()) {
            if (rem.startsWith(cKey)) {
                String baseChar = CONSONANTS.get(cKey);
                String afterC = rem.substring(cKey.length());
                
                boolean matraMatched = false;
                for (String mKey : MATRAS.keySet()) {
                    if (afterC.startsWith(mKey)) {
                        // For every possible matra mapped to this key (like 'a' -> "" and "ા")
                        for (String matra : MATRAS.get(mKey)) {
                            solve(afterC.substring(mKey.length()), curr + baseChar + matra, res);
                        }
                        matraMatched = true;
                    }
                }
                
                // If no vowel follows, treat as half-letter or implicit 'a'
                if (!matraMatched) {
                	solve(afterC, curr + baseChar + "્", res);
                    solve(afterC, curr + baseChar, res); // Normal consonant
                    //solve(afterC, curr + baseChar + "્", res); // Halant version
                }
            }
        }

        // --- Logic B: Independent Vowels ---
        for (String vKey : IND_VOWELS.keySet()) {
            if (rem.startsWith(vKey)) {
                for (String indV : IND_VOWELS.get(vKey)) {
                    solve(rem.substring(vKey.length()), curr + indV, res);
                }
            }
        }
    }

}