package com.grimforsaken.sparkofferfilter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class DropoffPolicy {
    private static final Pattern DROPOFF_PATTERN = Pattern.compile(
            "(?i)\\b([0-9]{1,2})\\s*drop(?:-|\\s)?offs?\\b");

    private static volatile boolean rejectThreePlus = false;

    private DropoffPolicy() {}

    static void configure(boolean enabled) {
        rejectThreePlus = enabled;
    }

    static int parseDropoffs(String text) {
        if (text == null || text.isEmpty()) return -1;
        Matcher matcher = DROPOFF_PATTERN.matcher(text);
        int highest = -1;
        while (matcher.find()) {
            try {
                int value = Integer.parseInt(matcher.group(1));
                if (value > highest) highest = value;
            } catch (NumberFormatException ignored) {}
        }
        return highest;
    }

    static boolean shouldReject(String text) {
        return rejectThreePlus && parseDropoffs(text) >= 3;
    }

    static String rejectionReason(String text) {
        int count = parseDropoffs(text);
        return count >= 0
                ? count + " drop-offs meets the enabled 3+ drop-off reject rule"
                : "3+ drop-off reject rule";
    }
}
