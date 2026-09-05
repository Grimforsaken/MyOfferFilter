package com.grimforsaken.sparkofferfilter;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class OfferCityDetector {
    private static final Pattern CITY_STATE_LINE_PATTERN = Pattern.compile(
            "(?im)^\\s*([A-Za-z][A-Za-z .'-]{1,40}?)\\s*,?\\s+(?:OK|Oklahoma)\\b(?:\\s*,?\\s*\\d{5}(?:-\\d{4})?)?\\s*$");
    private static final Pattern SPLIT_CITY_STATE_PATTERN = Pattern.compile(
            "(?im)^\\s*([A-Za-z][A-Za-z .'-]{1,40}?)\\s*,?\\s*$\\R\\s*(?:OK|Oklahoma)\\b(?:\\s*,?\\s*\\d{5}(?:-\\d{4})?)?\\s*$");
    private static final Pattern INLINE_ADDRESS_PATTERN = Pattern.compile(
            "(?im)^.*?,\\s*([A-Za-z][A-Za-z .'-]{1,40}?)\\s*,\\s*(?:OK|Oklahoma)\\b(?:\\s*,?\\s*\\d{5}(?:-\\d{4})?)?\\s*$");
    private static final Pattern LABELED_CITY_PATTERN = Pattern.compile(
            "(?im)^\\s*(?:city|pickup city|store city|location city)\\s*[:\\-]\\s*([A-Za-z][A-Za-z .'-]{1,40}?)\\s*$");

    private OfferCityDetector() {}

    static String detect(String currentTreeText) {
        if (currentTreeText == null || currentTreeText.trim().isEmpty()) return "Unknown";

        Set<String> cities = new LinkedHashSet<>();
        addCities(cities, CITY_STATE_LINE_PATTERN, currentTreeText);
        addCities(cities, SPLIT_CITY_STATE_PATTERN, currentTreeText);
        addCities(cities, INLINE_ADDRESS_PATTERN, currentTreeText);
        addCities(cities, LABELED_CITY_PATTERN, currentTreeText);

        if (cities.size() == 1) return cities.iterator().next();
        return "Unknown";
    }

    private static void addCities(Set<String> cities, Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String city = cleanCity(matcher.group(1));
            if (!city.isEmpty()) cities.add(city);
        }
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
