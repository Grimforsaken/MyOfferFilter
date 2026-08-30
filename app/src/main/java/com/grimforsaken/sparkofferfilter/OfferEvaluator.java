package com.grimforsaken.sparkofferfilter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OfferEvaluator {
    private static final Pattern DOLLAR_PATTERN = Pattern.compile("\\$\\s*([0-9]{1,5}(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?)");
    private static final Pattern MILES_PATTERN = Pattern.compile("(?i)([0-9]+(?:\\.[0-9]+)?)\\s*(?:mi(?:le)?s?\\.?)\\b");
    private static final Pattern SHOP_DELIVER_PATTERN = Pattern.compile("(?i)\\bshop\\s*(?:&|and)\\s*deliver(?:y)?\\b");

    private OfferEvaluator() {}

    public static Result evaluate(
            String visibleText,
            boolean rejectNoShopping,
            boolean rejectLowRate,
            double rejectMinimumDollarsPerMile,
            boolean autoAcceptEnabled,
            boolean acceptMinPayEnabled,
            double acceptMinPay,
            boolean acceptMinRateEnabled,
            double acceptMinRate,
            boolean acceptMaxMilesEnabled,
            double acceptMaxMiles) {

        String text = visibleText == null ? "" : visibleText;
        String normalized = normalize(text);
        Double pay = parseBestPay(text);
        Double miles = parseMiles(text);
        boolean hasAllowedCity = normalized.contains("SAND SPRINGS") || normalized.contains("SAPULPA");
        boolean hasShopping = normalized.contains("SHOPPING") || SHOP_DELIVER_PATTERN.matcher(text).find();

        if (pay == null || miles == null || miles <= 0.0) {
            return Result.notReady(hasAllowedCity, hasShopping, pay, miles,
                    "Waiting for readable pay and mileage.");
        }

        double rate = pay / miles;
        List<String> rejectionReasons = new ArrayList<>();

        if (!hasAllowedCity) {
            rejectionReasons.add("neither SAND SPRINGS nor SAPULPA is shown");
        }
        if (rejectNoShopping && !hasShopping) {
            rejectionReasons.add("Shopping is not shown");
        }
        if (rejectLowRate && rate + 1e-9 < rejectMinimumDollarsPerMile) {
            rejectionReasons.add(String.format(Locale.US,
                    "$%.2f/mi is below reject minimum $%.2f/mi", rate, rejectMinimumDollarsPerMile));
        }

        if (!rejectionReasons.isEmpty()) {
            return Result.ready(true, false, hasAllowedCity, hasShopping, pay, miles, rate,
                    String.join("; ", rejectionReasons));
        }

        boolean anyAcceptRule = acceptMinPayEnabled || acceptMinRateEnabled || acceptMaxMilesEnabled;
        if (!autoAcceptEnabled) {
            return Result.ready(false, false, hasAllowedCity, hasShopping, pay, miles, rate,
                    String.format(Locale.US, "Offer passes reject rules at $%.2f/mi; Auto-Accept is off.", rate));
        }
        if (!anyAcceptRule) {
            return Result.ready(false, false, hasAllowedCity, hasShopping, pay, miles, rate,
                    "Offer passes reject rules, but no Auto-Accept criteria are enabled.");
        }

        List<String> acceptFailures = new ArrayList<>();
        if (acceptMinPayEnabled && pay + 1e-9 < acceptMinPay) {
            acceptFailures.add(String.format(Locale.US, "$%.2f is below minimum $%.2f", pay, acceptMinPay));
        }
        if (acceptMinRateEnabled && rate + 1e-9 < acceptMinRate) {
            acceptFailures.add(String.format(Locale.US, "$%.2f/mi is below accept minimum $%.2f/mi", rate, acceptMinRate));
        }
        if (acceptMaxMilesEnabled && miles - 1e-9 > acceptMaxMiles) {
            acceptFailures.add(String.format(Locale.US, "%.1f mi exceeds maximum %.1f mi", miles, acceptMaxMiles));
        }

        if (acceptFailures.isEmpty()) {
            return Result.ready(false, true, hasAllowedCity, hasShopping, pay, miles, rate,
                    "Offer passes every enabled Auto-Accept criterion.");
        }

        return Result.ready(false, false, hasAllowedCity, hasShopping, pay, miles, rate,
                "Not auto-accepted: " + String.join("; ", acceptFailures));
    }

    static String normalize(String input) {
        return input.toUpperCase(Locale.US)
                .replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    static Double parseBestPay(String text) {
        String[] lines = text.split("\\R");
        Double bestValue = null;
        int bestScore = Integer.MIN_VALUE;
        int order = 0;

        for (String line : lines) {
            Matcher matcher = DOLLAR_PATTERN.matcher(line);
            while (matcher.find()) {
                Double value = parseNumber(matcher.group(1));
                if (value == null || value <= 0) continue;

                String upper = line.toUpperCase(Locale.US);
                int score = 0;
                if (upper.contains("ESTIMATED EARNINGS")) score += 100;
                else if (upper.contains("EARNINGS")) score += 80;
                if (upper.contains("OFFER")) score += 60;
                if (upper.contains("TOTAL")) score += 40;
                if (upper.contains("PAY")) score += 30;
                if (upper.contains("TIP")) score -= 50;
                if (upper.contains("INCENTIVE")) score -= 50;
                score -= Math.min(order, 20);

                if (bestValue == null || score > bestScore) {
                    bestValue = value;
                    bestScore = score;
                }
                order++;
            }
        }

        if (bestValue != null) return bestValue;

        Matcher matcher = DOLLAR_PATTERN.matcher(text);
        if (matcher.find()) return parseNumber(matcher.group(1));
        return null;
    }

    static Double parseMiles(String text) {
        Matcher matcher = MILES_PATTERN.matcher(text);
        Double largest = null;
        while (matcher.find()) {
            Double value = parseNumber(matcher.group(1));
            if (value != null && value > 0 && (largest == null || value > largest)) {
                largest = value;
            }
        }
        return largest;
    }

    private static Double parseNumber(String raw) {
        try {
            return Double.parseDouble(raw.replace(",", ""));
        } catch (Exception ignored) {
            return null;
        }
    }

    public static final class Result {
        public final boolean ready;
        public final boolean shouldReject;
        public final boolean shouldAccept;
        public final boolean hasAllowedCity;
        public final boolean hasShopping;
        public final Double pay;
        public final Double miles;
        public final Double dollarsPerMile;
        public final String reason;

        private Result(boolean ready, boolean shouldReject, boolean shouldAccept,
                       boolean hasAllowedCity, boolean hasShopping, Double pay,
                       Double miles, Double dollarsPerMile, String reason) {
            this.ready = ready;
            this.shouldReject = shouldReject;
            this.shouldAccept = shouldAccept;
            this.hasAllowedCity = hasAllowedCity;
            this.hasShopping = hasShopping;
            this.pay = pay;
            this.miles = miles;
            this.dollarsPerMile = dollarsPerMile;
            this.reason = reason;
        }

        static Result notReady(boolean hasAllowedCity, boolean hasShopping, Double pay,
                               Double miles, String reason) {
            return new Result(false, false, false, hasAllowedCity, hasShopping,
                    pay, miles, null, reason);
        }

        static Result ready(boolean shouldReject, boolean shouldAccept,
                            boolean hasAllowedCity, boolean hasShopping,
                            Double pay, Double miles, Double dollarsPerMile, String reason) {
            return new Result(true, shouldReject, shouldAccept, hasAllowedCity, hasShopping,
                    pay, miles, dollarsPerMile, reason);
        }
    }
}
