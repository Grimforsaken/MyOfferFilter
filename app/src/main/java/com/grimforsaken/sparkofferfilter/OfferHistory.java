package com.grimforsaken.sparkofferfilter;

import android.content.SharedPreferences;

final class OfferHistory {
    private static final String SEPARATOR = "\n\n────────────────────────\n\n";
    private static final int MAX_CHARS = 60000;

    private OfferHistory() {}

    static void addRejected(SharedPreferences prefs, String entry) {
        append(prefs, Prefs.HISTORY_REJECTED, entry);
    }

    static void addAccepted(SharedPreferences prefs, String entry) {
        append(prefs, Prefs.HISTORY_ACCEPTED, entry);
    }

    static String rejected(SharedPreferences prefs) {
        String value = prefs.getString(Prefs.HISTORY_REJECTED, "");
        return value == null || value.trim().isEmpty()
                ? "No rejected orders recorded yet."
                : value;
    }

    static String accepted(SharedPreferences prefs) {
        String value = prefs.getString(Prefs.HISTORY_ACCEPTED, "");
        return value == null || value.trim().isEmpty()
                ? "No accepted orders recorded yet."
                : value;
    }

    private static void append(SharedPreferences prefs, String key, String entry) {
        if (prefs == null || entry == null || entry.trim().isEmpty()) return;
        String old = prefs.getString(key, "");
        String combined = entry.trim();
        if (old != null && !old.trim().isEmpty()) {
            combined += SEPARATOR + old;
        }
        if (combined.length() > MAX_CHARS) {
            combined = combined.substring(0, MAX_CHARS);
        }
        prefs.edit().putString(key, combined).apply();
    }
}
