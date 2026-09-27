package com.grimforsaken.sparkofferfilter;

import java.util.Locale;

final class EarningsOfferCandidate {
    private EarningsOfferCandidate() {}

    static boolean isCompleteShoppingOfferCard(String text) {
        if (text == null || text.trim().isEmpty()) return false;
        String n = OfferEvaluator.normalize(text);
        boolean hasStore = n.contains("WALMART") || n.contains("SAM'S CLUB") || n.contains("SAMS CLUB");
        boolean hasShopping = n.contains("SHOPPING")
                || n.contains("SHOP & DELIVER")
                || n.contains("SHOP AND DELIVER");
        boolean hasDecision = n.contains("ACCEPT")
                && (n.contains("REJECT") || n.contains("DECLINE"));
        return hasStore
                && hasShopping
                && hasDecision
                && OfferEvaluator.parseBestPay(text) != null
                && OfferEvaluator.parseMiles(text) != null
                && TripDurationPolicy.parseTripMinutes(text) > 0;
    }
}
