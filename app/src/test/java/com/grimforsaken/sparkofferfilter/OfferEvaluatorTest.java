package com.grimforsaken.sparkofferfilter;

public final class OfferEvaluatorTest {
    public static void main(String[] args) {
        shouldKeepAllowedCityAtRejectThreshold();
        shouldRejectWrongCityBeforeAccept();
        shouldRejectMissingShoppingWhenEnabled();
        shouldRejectLowRate();
        shouldAutoAcceptWhenAllEnabledRulesPass();
        shouldNotAutoAcceptBelowMinimumPay();
        shouldNotAutoAcceptBelowMinimumRate();
        shouldNotAutoAcceptAboveMaxMiles();
        shouldNotAutoAcceptWithNoAcceptanceRules();
        shouldPreferEstimatedEarningsOverTip();
        shouldUseLargestMileage();
        shouldWaitWhenMileageMissing();
        System.out.println("OfferEvaluator tests passed.");
    }

    private static OfferEvaluator.Result eval(String text,
                                               boolean rejectShopping, boolean rejectRate, double rejectMin,
                                               boolean autoAccept,
                                               boolean minPayOn, double minPay,
                                               boolean minRateOn, double minRate,
                                               boolean maxMilesOn, double maxMiles) {
        return OfferEvaluator.evaluate(text, rejectShopping, rejectRate, rejectMin,
                autoAccept, minPayOn, minPay, minRateOn, minRate, maxMilesOn, maxMiles);
    }

    private static void shouldKeepAllowedCityAtRejectThreshold() {
        String text = "Estimated earnings $12.50\n10 miles\nSAND SPRINGS\nShopping\nReject";
        OfferEvaluator.Result r = eval(text, true, true, 1.25,
                false, false, 20, false, 1.25, false, 10);
        require(r.ready && !r.shouldReject && !r.shouldAccept, "expected keep at reject threshold");
    }

    private static void shouldRejectWrongCityBeforeAccept() {
        String text = "$30.00\n8 mi\nTulsa\nShopping\nAccept\nReject";
        OfferEvaluator.Result r = eval(text, false, false, 1.25,
                true, true, 20, true, 1.25, true, 10);
        require(r.ready && r.shouldReject && !r.shouldAccept, "wrong city must reject before accept");
    }

    private static void shouldRejectMissingShoppingWhenEnabled() {
        String text = "$20.00\n8 miles\nSAPULPA\nReject";
        OfferEvaluator.Result r = eval(text, true, false, 1.25,
                true, true, 10, false, 1.25, false, 10);
        require(r.shouldReject && !r.shouldAccept, "expected shopping rejection");
    }

    private static void shouldRejectLowRate() {
        String text = "$10.00\n10 miles\nSAPULPA\nShopping\nReject";
        OfferEvaluator.Result r = eval(text, false, true, 1.25,
                true, true, 5, false, 1.0, false, 20);
        require(r.shouldReject && !r.shouldAccept && Math.abs(r.dollarsPerMile - 1.0) < 0.0001,
                "expected low-rate rejection");
    }

    private static void shouldAutoAcceptWhenAllEnabledRulesPass() {
        String text = "Estimated earnings $30.00\n12 miles\nSAPULPA\nShopping\nAccept\nReject";
        OfferEvaluator.Result r = eval(text, false, true, 1.25,
                true, true, 25, true, 2.00, true, 15);
        require(!r.shouldReject && r.shouldAccept, "expected auto-accept");
    }

    private static void shouldNotAutoAcceptBelowMinimumPay() {
        String text = "$19.99\n8 miles\nSAPULPA\nAccept\nReject";
        OfferEvaluator.Result r = eval(text, false, false, 1.25,
                true, true, 20, false, 1.25, false, 10);
        require(!r.shouldReject && !r.shouldAccept, "minimum pay should block auto-accept");
    }

    private static void shouldNotAutoAcceptBelowMinimumRate() {
        String text = "$20.00\n20 miles\nSAPULPA\nAccept\nReject";
        OfferEvaluator.Result r = eval(text, false, false, 1.25,
                true, false, 20, true, 1.25, false, 10);
        require(!r.shouldReject && !r.shouldAccept, "minimum accept rate should block auto-accept");
    }

    private static void shouldNotAutoAcceptAboveMaxMiles() {
        String text = "$50.00\n10.1 miles\nSAND SPRINGS\nAccept\nReject";
        OfferEvaluator.Result r = eval(text, false, false, 1.25,
                true, false, 20, false, 1.25, true, 10.0);
        require(!r.shouldReject && !r.shouldAccept, "max miles should block auto-accept");
    }

    private static void shouldNotAutoAcceptWithNoAcceptanceRules() {
        String text = "$50.00\n5 miles\nSAPULPA\nAccept\nReject";
        OfferEvaluator.Result r = eval(text, false, false, 1.25,
                true, false, 20, false, 1.25, false, 10);
        require(!r.shouldReject && !r.shouldAccept, "no acceptance criteria must mean no auto-accept");
    }

    private static void shouldPreferEstimatedEarningsOverTip() {
        String text = "Tip $5.00\nEstimated earnings $22.50\n15 miles\nSAPULPA";
        Double pay = OfferEvaluator.parseBestPay(text);
        require(pay != null && Math.abs(pay - 22.50) < 0.001, "pay context selection failed");
    }

    private static void shouldUseLargestMileage() {
        String text = "$25.00\n2.2 miles\n14.8 miles\nSAND SPRINGS";
        Double miles = OfferEvaluator.parseMiles(text);
        require(miles != null && Math.abs(miles - 14.8) < 0.001, "mileage selection failed");
    }

    private static void shouldWaitWhenMileageMissing() {
        String text = "$25.00\nSAPULPA\nShopping\nAccept\nReject";
        OfferEvaluator.Result r = eval(text, true, true, 1.25,
                true, true, 20, true, 1.25, true, 10);
        require(!r.ready && !r.shouldReject && !r.shouldAccept, "should not act on incomplete offer");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
