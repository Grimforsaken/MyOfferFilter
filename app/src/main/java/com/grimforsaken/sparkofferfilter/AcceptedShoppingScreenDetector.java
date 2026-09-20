package com.grimforsaken.sparkofferfilter;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class AcceptedShoppingScreenDetector {
    private static final Pattern TRIP_HEADER = Pattern.compile(
            "(?i)\\bSTOP\\s*#\\s*\\d+\\s+FOR\\s+TRIP\\s+(\\d+)\\b");
    private static final Pattern STORE_LINE = Pattern.compile(
            "(?i)\\b((?:WALMART|SAM(?:'|’)?S\\s+CLUB)\\s+[A-Z][A-Z .'-]*?\\s*#\\s*\\d+)\\b");
    private static final Pattern STORE_NUMBER = Pattern.compile("#\\s*(\\d+)");

    private AcceptedShoppingScreenDetector() {}

    static boolean isConfirmedShoppingTripScreen(String text) {
        if (text == null || text.trim().isEmpty()) return false;
        String normalized = text.toUpperCase(Locale.US)
                .replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
        return TRIP_HEADER.matcher(normalized).find()
                && normalized.contains("SHOPPING")
                && normalized.contains("CONFIRM ARRIVAL")
                && (normalized.contains("WALMART") || normalized.contains("SAM'S CLUB")
                    || normalized.contains("SAMS CLUB"));
    }

    static String tripId(String text) {
        if (text == null) return "";
        Matcher matcher = TRIP_HEADER.matcher(text);
        return matcher.find() ? matcher.group(1) : "";
    }

    static String storeLabel(String text) {
        if (text == null) return "";
        String flattened = text.replace('\u00A0', ' ').replaceAll("\\s+", " ");
        Matcher matcher = STORE_LINE.matcher(flattened);
        if (!matcher.find()) return "";
        return matcher.group(1).trim().replaceAll("\\s+", " ");
    }

    static String storeNumber(String text) {
        String store = storeLabel(text);
        Matcher matcher = STORE_NUMBER.matcher(store);
        return matcher.find() ? matcher.group(1) : "";
    }
}
