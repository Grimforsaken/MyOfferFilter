package com.grimforsaken.sparkofferfilter;

public final class OfferEvaluatorTest {
    public static void main(String[] args) {
        shouldRejectBelowMinimumOrderAmount();
        shouldRejectLowRate();
        shouldRejectMissingShoppingWhenEnabled();
        shouldRejectOverSelectedMaximumMiles();
        shouldKeepOrderAtSelectedMaximumMiles();
        shouldAutoAcceptWhenAllEnabledRulesPass();
        shouldNotAutoAcceptAboveAcceptMaxMiles();
        shouldRequireShoppingForAutoAccept();
        shouldRequireNoShoppingForAutoAccept();
        shouldPreferEstimatedEarningsOverTip();
        shouldUseLargestMileage();
        shouldWaitWhenMileageMissing();
        System.out.println("OfferEvaluator tests passed.");
    }

    private static OfferEvaluator.Result eval(String text,
                                               boolean rejectShopping,
                                               boolean rejectRate,
                                               double rejectRateMin,
                                               boolean rejectMinPayOn,
                                               double rejectMinPay,
                                               boolean autoAccept,
                                               boolean minPayOn,
                                               double minPay,
                                               boolean minRateOn,
                                               double minRate,
                                               boolean maxMilesOn,
                                               double maxMiles) {
        return OfferEvaluator.evaluate(text,
                rejectShopping, rejectRate, rejectRateMin,
                rejectMinPayOn, rejectMinPay,
                autoAccept, minPayOn, minPay,
                minRateOn, minRate,
                maxMilesOn, maxMiles,
                false, false);
    }

    private static void shouldRejectBelowMinimumOrderAmount() {
        OfferEvaluator.Result r = eval("$14.99\n7 miles\nShopping\nReject",
                false, false, 1.25,
                true, 15.00,
                false, false, 20, false, 1.25, false, 10);
        require(r.ready && r.shouldReject && !r.shouldAccept,
                "offer below minimum order amount should reject");
    }

    private static void shouldRejectLowRate() {
        OfferEvaluator.Result r = eval("$10.00\n10 miles\nShopping\nReject",
                false, true, 1.25,
                false, 15.00,
                true, true, 5, false, 1.0, false, 20);
        require(r.shouldReject && !r.shouldAccept && Math.abs(r.dollarsPerMile - 1.0) < 0.0001,
                "expected low-rate rejection");
    }

    private static void shouldRejectMissingShoppingWhenEnabled() {
        OfferEvaluator.Result r = eval("$20.00\n8 miles\nReject",
                true, false, 1.25,
                false, 15.00,
                true, true, 10, false, 1.25, false, 10);
        require(r.shouldReject && !r.shouldAccept, "expected shopping rejection");
    }

    private static void shouldRejectOverSelectedMaximumMiles() {
        OfferEvaluator.Result r = OfferEvaluator.evaluate(
                "$30.00\n15.1 miles\nShopping\nReject",
                false, false, 1.25,
                false, 15.00,
                true, 15.0,
                false, false, 20.00,
                false, 1.25,
                false, 20.0,
                false, false);
        require(r.ready && r.shouldReject && r.reason.contains("exceeds reject maximum 15.0 mi"),
                "orders over the selected reject mileage must reject");
    }

    private static void shouldKeepOrderAtSelectedMaximumMiles() {
        OfferEvaluator.Result r = OfferEvaluator.evaluate(
                "$30.00\n15.0 miles\nShopping\nReject",
                false, false, 1.25,
                false, 15.00,
                true, 15.0,
                false, false, 20.00,
                false, 1.25,
                false, 20.0,
                false, false);
        require(r.ready && !r.shouldReject,
                "order exactly at the selected reject mileage should not reject for mileage");
    }

    private static void shouldAutoAcceptWhenAllEnabledRulesPass() {
        OfferEvaluator.Result r = eval("Estimated earnings $30.00\n12 miles\nShopping\nAccept\nReject",
                false, true, 1.25,
                true, 15.00,
                true, true, 25, true, 2.00, true, 15);
        require(!r.shouldReject && r.shouldAccept, "expected auto-accept");
    }

    private static void shouldNotAutoAcceptAboveAcceptMaxMiles() {
        OfferEvaluator.Result r = eval("$50.00\n10.1 miles\nAccept\nReject",
                false, false, 1.25,
                false, 15.00,
                true, false, 20, false, 1.25, true, 10.0);
        require(!r.shouldReject && !r.shouldAccept, "accept max miles should block auto-accept");
    }

    private static void shouldRequireShoppingForAutoAccept() {
        OfferEvaluator.Result blocked = OfferEvaluator.evaluate(
                "$30.00\n8 miles\nAccept\nReject",
                false, false, 1.25, false, 15.00,
                true, false, 20, false, 1.25, false, 10, true, false);
        require(!blocked.shouldReject && !blocked.shouldAccept,
                "Shopping-only choice should block an order without Shopping");

        OfferEvaluator.Result allowed = OfferEvaluator.evaluate(
                "$30.00\n8 miles\nShopping\nAccept\nReject",
                false, false, 1.25, false, 15.00,
                true, false, 20, false, 1.25, false, 10, true, false);
        require(!allowed.shouldReject && allowed.shouldAccept,
                "Shopping-only choice should allow a Shopping order");
    }

    private static void shouldRequireNoShoppingForAutoAccept() {
        OfferEvaluator.Result blocked = OfferEvaluator.evaluate(
                "$30.00\n8 miles\nShopping\nAccept\nReject",
                false, false, 1.25, false, 15.00,
                true, false, 20, false, 1.25, false, 10, false, true);
        require(!blocked.shouldReject && !blocked.shouldAccept,
                "No-Shopping choice should block a Shopping order");

        OfferEvaluator.Result allowed = OfferEvaluator.evaluate(
                "$30.00\n8 miles\nCurbside pickup\nAccept\nReject",
                false, false, 1.25, false, 15.00,
                true, false, 20, false, 1.25, false, 10, false, true);
        require(!allowed.shouldReject && allowed.shouldAccept,
                "No-Shopping choice should allow an order without Shopping");
    }

    private static void shouldPreferEstimatedEarningsOverTip() {
        Double pay = OfferEvaluator.parseBestPay("Tip $5.00\nEstimated earnings $22.50\n15 miles");
        require(pay != null && Math.abs(pay - 22.50) < 0.001, "pay context selection failed");
    }

    private static void shouldUseLargestMileage() {
        Double miles = OfferEvaluator.parseMiles("$25.00\n2.2 miles\n14.8 miles");
        require(miles != null && Math.abs(miles - 14.8) < 0.001, "mileage selection failed");
    }

    private static void shouldWaitWhenMileageMissing() {
        OfferEvaluator.Result r = eval("$25.00\nShopping\nAccept\nReject",
                true, true, 1.25,
                true, 15.00,
                true, true, 20, true, 1.25, true, 10);
        require(!r.ready && !r.shouldReject && !r.shouldAccept,
                "complete non-reject offers should still wait for mileage");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
