package com.badargadh.sahkar.util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class TransliterationService {

    public static String getGoogleTransliteration(String text) {
        try {
            if (text == null || text.trim().isEmpty()) return "";

            // 1. Encode the text for URL (Important for spaces)
            String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);
            String url = "https://inputtools.google.com/request?text=" + encodedText + "&itc=gu-t-i0-und&num=1";

            // 2. Create the HTTP Client and Request
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            // 3. Send Request and get Response
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseGujaratiFromJson(response.body());
            }

        } catch (Exception e) {
            System.err.println("Transliteration failed: " + e.getMessage());
        }
        return text; // Return original English text if conversion fails
    }

    /**
     * Google Response format is nested: ["SUCCESS",[["English",["Gujarati"]]]]
     * We use a simple regex/string extraction to avoid a heavy JSON library
     */
    private static String parseGujaratiFromJson(String json) {
        try {
            // Finding the first Gujarati result between quotes
            // Looking for the pattern: ... [["english",["GUJARATI_HERE"]]]
            int start = json.lastIndexOf("[\"") + 2;
            int end = json.lastIndexOf("\"]");
            
            if (start > 1 && end > start) {
                return json.substring(start, end);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Conversion Error";
    }
}