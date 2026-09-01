package com.grimforsaken.sparkofferfilter;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class OfferCityDetector {
    private static final Pattern CITY_STATE_PATTERN = Pattern.compile(
            "(?im)^\\s*([A-Za-z][A-Za-z .'-]{1,40}?)\\s*,?\\s+(?:OK|Oklahoma)(?:\\s+\\d{5}(?:-\\d{4})?)?\\s*$");

    private OfferCityDetector() {}

    static String detect(String currentTreeText) {
        if (currentTreeText == null || currentTreeText.trim().isEmpty()) return "Unknown";

        Set<String> cities = new LinkedHashSet<>();
        Matcher matcher = CITY_STATE_PATTERN.matcher(currentTreeText);
        while (matcher.find()) {
            String city = cleanCity(matcher.group(1));
            if (!city.isEmpty()) cities.add(city);
        }

        if (cities.size() == 1) return cities.iterator().next();
        return "Unknown";
    }

    private static String cleanCity(String raw) {
        if (raw == null) return "";
        String normalized = raw.trim().replaceAll("\\s+", " ").toLowerCase(Locale.US);
        if (normalized.isEmpty()) return "";
        StringBuilder out = new StringBuilder(normalized.length());
        boolean capitalize = true;
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (capitalize && Character.isLetter(c)) {
                out.append(Character.toUpperCase(c));
                capitalize = false;
            } else {
                out.append(c);
            }
            if (c == ' ' || c == '-' || c == '\'') capitalize = true;
        }
        return out.toString();
    }
}
