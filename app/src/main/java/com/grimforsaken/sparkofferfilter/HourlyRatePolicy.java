package com.grimforsaken.sparkofferfilter;

import java.util.Locale;

final class HourlyRatePolicy {
    private static volatile boolean enabled = false;
    private static volatile double minimumDollarsPerHour = 20.00;

    private HourlyRatePolicy() {}

    static void configure(boolean rejectEnabled, double minimumRate) {
        enabled = rejectEnabled;
        minimumDollarsPerHour = Math.max(0.01, minimumRate);
    }

    static Double calculateDollarsPerHour(Double pay, String text) {
        if (pay == null || pay <= 0.0) return null;
        int tripMinutes = TripDurationPolicy.parseTripMinutes(text);
        if (tripMinutes <= 0) return null;
        return pay * 60.0 / tripMinutes;
    }

    static boolean shouldReject(Double pay, String text) {
        Double hourly = calculateDollarsPerHour(pay, text);
        return enabled && hourly != null && hourly + 1e-9 < minimumDollarsPerHour;
    }

    static String rejectionReason(Double pay, String text) {
        Double hourly = calculateDollarsPerHour(pay, text);
        if (hourly == null) return "Minimum dollars-per-hour reject rule";
        return String.format(Locale.US,
                "$%.2f/hr is below reject minimum $%.2f/hr",
                hourly, minimumDollarsPerHour);
    }
}
