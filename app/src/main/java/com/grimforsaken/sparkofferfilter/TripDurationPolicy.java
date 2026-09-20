package com.grimforsaken.sparkofferfilter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class TripDurationPolicy {
    private static final Pattern TRIP_SUMMARY_DURATION = Pattern.compile(
            "(?i)\\b[0-9]+\\s+stops?\\b\\s*(?:[•·|]\\s*|\\s+)"
          + "[0-9]+(?:\\.[0-9]+)?\\s*(?:mi(?:le)?s?\\.?)\\b\\s*(?:[•·|]\\s*|\\s+)"
          + "(?:(\\d+)\\s*(?:hours?|hrs?|hr|h)\\b(?:\\s*(\\d+)\\s*(?:minutes?|mins?|min|m)\\b)?"
          + "|(\\d+)\\s*(?:minutes?|mins?|min|m)\\b)");

    private static volatile boolean enabled = false;
    private static volatile int maximumMinutes = 60;

    private TripDurationPolicy() {}

    static void configure(boolean rejectEnabled, int hours, int minutes) {
        enabled = rejectEnabled;
        int safeHours = Math.max(0, hours);
        int safeMinutes = Math.max(0, Math.min(59, minutes));
        maximumMinutes = safeHours * 60 + safeMinutes;
    }

    static int parseTripMinutes(String text) {
        if (text == null || text.isEmpty()) return -1;
        Matcher matcher = TRIP_SUMMARY_DURATION.matcher(text);
        if (!matcher.find()) return -1;

        try {
            if (matcher.group(3) != null) {
                return Integer.parseInt(matcher.group(3));
            }
            int hours = matcher.group(1) == null ? 0 : Integer.parseInt(matcher.group(1));
            int minutes = matcher.group(2) == null ? 0 : Integer.parseInt(matcher.group(2));
            return hours * 60 + minutes;
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    static boolean shouldReject(String text) {
        int tripMinutes = parseTripMinutes(text);
        return enabled && tripMinutes >= 0 && tripMinutes > maximumMinutes;
    }

    static String rejectionReason(String text) {
        int tripMinutes = parseTripMinutes(text);
        return formatMinutes(tripMinutes) + " trip time exceeds enabled maximum "
                + formatMinutes(maximumMinutes);
    }

    private static String formatMinutes(int totalMinutes) {
        if (totalMinutes < 0) return "Unknown";
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        if (hours == 0) return minutes + " min";
        if (minutes == 0) return hours + (hours == 1 ? " hr" : " hrs");
        return hours + (hours == 1 ? " hr " : " hrs ") + minutes + " min";
    }
}
