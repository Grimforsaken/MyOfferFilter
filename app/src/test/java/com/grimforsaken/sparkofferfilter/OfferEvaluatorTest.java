package com.grimforsaken.sparkofferfilter;

public final class OfferEvaluatorTest {
    public static void main(String[] args) {
        shouldAllowOtherCities();
        shouldHardRejectTulsa();
        shouldHardRejectGlenpool();
        shouldHardRejectJenks();
        shouldAllowTulsaWhenChecked();
        shouldAllowGlenpoolWhenChecked();
        shouldAllowJenksWhenChecked();
        shouldStillApplyOtherRejectRulesToAllowedCity();
        shouldHardRejectBeforePayAndMilesLoad();
        shouldRejectBelowMinimumOrderAmount();
        shouldKeepAtMinimumOrderAmount();
        shouldRejectBelowMinimumBeforeMileageLoads();
        shouldRejectMissingShoppingWhenEnabled();
        shouldRejectLowRate();
        shouldAutoAcceptWhenAllEnabledRulesPass();
        shouldNotAutoAcceptBelowMinimumPay();
        shouldNotAutoAcceptBelowMinimumRate();
        shouldNotAutoAcceptAboveMaxMiles();
        shouldNotAutoAcceptWithNoAcceptanceRules();
        shouldRequireShoppingForAutoAccept();
        shouldRequireNoShippingForAutoAccept();
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
        CityPolicy.configure(false, false, false);
        return OfferEvaluator.evaluate(text,
                rejectShopping,
                rejectRate,
                rejectRateMin,
                rejectMinPayOn,
                rejectMinPay,
                autoAccept,
                minPayOn,
                minPay,
                minRateOn,
                minRate,
                maxMilesOn,
                maxMiles,
                false,
                false);
    }

    private static OfferEvaluator.Result evalAllowedCity(String city,
                                                         boolean allowTulsa,
                                                         boolean allowGlenpool,
                                                         boolean allowJenks,
                                                         boolean rejectMinPayOn,
                                                         double rejectMinPay) {
        CityPolicy.configure(allowTulsa, allowGlenpool, allowJenks);
        OfferEvaluator.Result result = OfferEvaluator.evaluate(
                "$30.00\n8 miles\n" + city + "\nShopping\nAccept\nReject",
                false, false, 1.25,
                rejectMinPayOn, rejectMinPay,
                false, false, 20, false, 1.25, false, 10,
                false, false);
        CityPolicy.configure(false, false, false);
        return result;
    }

    private static void shouldAllowOtherCities() {
        OfferEvaluator.Result r = eval("$20.00\n8 miles\nBIXBY\nShopping\nAccept\nReject",
                false, false, 1.25,
                false, 15.00,
                false, false, 20, false, 1.25, false, 10);
        require(r.ready && !r.shouldReject && !r.shouldAccept,
                "non-city-filter cities should not be rejected by city");
        require(!r.reason.contains("SAND SPRINGS") && !r.reason.contains("SAPULPA"),
                "old allowed-city rule should be gone");
    }

    private static void shouldHardRejectTulsa() { assertHardRejectCity("TULSA"); }
    private static void shouldHardRejectGlenpool() { assertHardRejectCity("GLENPOOL"); }
    private static void shouldHardRejectJenks() { assertHardRejectCity("JENKS"); }

    private static void assertHardRejectCity(String city) {
        OfferEvaluator.Result r = eval("$50.00\n5 miles\n" + city + "\nShopping\nAccept\nReject",
                false, false, 1.25,
                true, 15.00,
                true, true, 20, true, 1.25, true, 10);
        require(r.ready && r.shouldReject && !r.shouldAccept, city + " must reject by default");
        require(r.reason.contains(city), city + " rejection reason missing");
    }

    private static void shouldAllowTulsaWhenChecked() {
        OfferEvaluator.Result r = evalAllowedCity("TULSA", true, false, false, false, 15.00);
        require(r.ready && !r.shouldReject, "TULSA should be allowed when its checkbox is checked");
    }

    private static void shouldAllowGlenpoolWhenChecked() {
        OfferEvaluator.Result r = evalAllowedCity("GLENPOOL", false, true, false, false, 15.00);
        require(r.ready && !r.shouldReject, "GLENPOOL should be allowed when its checkbox is checked");
    }

    private static void shouldAllowJenksWhenChecked() {
        OfferEvaluator.Result r = evalAllowedCity("JENKS", false, false, true, false, 15.00);
        require(r.ready && !r.shouldReject, "JENKS should be allowed when its checkbox is checked");
    }

    private static void shouldStillApplyOtherRejectRulesToAllowedCity() {
        CityPolicy.configure(true, false, false);
        OfferEvaluator.Result r = OfferEvaluator.evaluate(
                "$10.00\n8 miles\nTULSA\nShopping\nAccept\nReject",
                false, false, 1.25,
                true, 15.00,
                false, false, 20, false, 1.25, false, 10,
                false, false);
        CityPolicy.configure(false, false, false);
        require(r.ready && r.shouldReject,
                "allowing a city must not bypass the minimum-order reject rule");
        require(r.reason.contains("below reject minimum"),
                "other reject rule should explain an allowed-city rejection");
    }

    private static void shouldHardRejectBeforePayAndMilesLoad() {
        OfferEvaluator.Result r = eval("JENKS\nShopping\nReject", false, true, 1.25,
                true, 15.00,
                true, true, 20, true, 1.25, true, 10);
        require(r.ready && r.shouldReject && !r.shouldAccept,
                "blocked city should not wait for pay or mileage");
    }

    private static void shouldRejectBelowMinimumOrderAmount() {
        OfferEvaluator.Result r = eval("$14.99\n7 miles\nBROKEN ARROW\nShopping\nReject",
                false, false, 1.25,
                true, 15.00,
                false, false, 20, false, 1.25, false, 10);
        require(r.ready && r.shouldReject && !r.shouldAccept,
                "offer below minimum order amount should reject");
    }

    private static void shouldKeepAtMinimumOrderAmount() {
        OfferEvaluator.Result r = eval("$15.00\n7 miles\nBROKEN ARROW\nShopping\nReject",
                false, false, 1.25,
                true, 15.00,
                false, false, 20, false, 1.25, false, 10);
        require(r.ready && !r.shouldReject,
                "offer exactly at minimum order amount should not reject");
    }

    private static void shouldRejectBelowMinimumBeforeMileageLoads() {
        OfferEvaluator.Result r = eval("Estimated earnings $12.50\nBROKEN ARROW\nReject",
                false, true, 1.25,
                true, 15.00,
                false, false, 20, false, 1.25, false, 10);
        require(r.ready && r.shouldReject,
                "low-dollar rejection should not wait for mileage");
    }

    private static void shouldRejectMissingShoppingWhenEnabled() {
        OfferEvaluator.Result r = eval("$20.00\n8 miles\nBIXBY\nReject",
                true, false, 1.25,
                false, 15.00,
                true, true, 10, false, 1.25, false, 10);
        require(r.shouldReject && !r.shouldAccept, "expected shopping rejection");
    }

    private static void shouldRejectLowRate() {
        OfferEvaluator.Result r = eval("$10.00\n10 miles\nBIXBY\nShopping\nReject",
                false, true, 1.25,
                false, 15.00,
                true, true, 5, false, 1.0, false, 20);
        require(r.shouldReject && !r.shouldAccept && Math.abs(r.dollarsPerMile - 1.0) < 0.0001,
                "expected low-rate rejection");
    }

    private static void shouldAutoAcceptWhenAllEnabledRulesPass() {
        OfferEvaluator.Result r = eval("Estimated earnings $30.00\n12 miles\nBIXBY\nShopping\nAccept\nReject",
                false, true, 1.25,
                true, 15.00,
                true, true, 25, true, 2.00, true, 15);
        require(!r.shouldReject && r.shouldAccept, "expected auto-accept");
    }

    private static void shouldNotAutoAcceptBelowMinimumPay() {
        OfferEvaluator.Result r = eval("$19.99\n8 miles\nBIXBY\nAccept\nReject",
                false, false, 1.25,
                false, 15.00,
                true, true, 20, false, 1.25, false, 10);
        require(!r.shouldReject && !r.shouldAccept, "minimum pay should block auto-accept");
    }

    private static void shouldNotAutoAcceptBelowMinimumRate() {
        OfferEvaluator.Result r = eval("$20.00\n20 miles\nBIXBY\nAccept\nReject",
                false, false, 1.25,
                false, 15.00,
                true, false, 20, true, 1.25, false, 10);
        require(!r.shouldReject && !r.shouldAccept, "minimum accept rate should block auto-accept");
    }

    private static void shouldNotAutoAcceptAboveMaxMiles() {
        OfferEvaluator.Result r = eval("$50.00\n10.1 miles\nBIXBY\nAccept\nReject",
                false, false, 1.25,
                false, 15.00,
                true, false, 20, false, 1.25, true, 10.0);
        require(!r.shouldReject && !r.shouldAccept, "max miles should block auto-accept");
    }

    private static void shouldNotAutoAcceptWithNoAcceptanceRules() {
        OfferEvaluator.Result r = eval("$50.00\n5 miles\nBIXBY\nAccept\nReject",
                false, false, 1.25,
                false, 15.00,
                true, false, 20, false, 1.25, false, 10);
        require(!r.shouldReject && !r.shouldAccept, "no acceptance criteria must mean no auto-accept");
    }

    private static void shouldRequireShoppingForAutoAccept() {
        CityPolicy.configure(false, false, false);
        String withoutShopping = "$30.00\n8 miles\nBIXBY\nAccept\nReject";
        OfferEvaluator.Result blocked = OfferEvaluator.evaluate(withoutShopping,
                false, false, 1.25, false, 15.00,
                true, false, 20, false, 1.25, false, 10, true, false);
        require(!blocked.shouldReject && !blocked.shouldAccept,
                "Shopping requirement should block auto-accept");

        String withShopping = "$30.00\n8 miles\nBIXBY\nShopping\nAccept\nReject";
        OfferEvaluator.Result allowed = OfferEvaluator.evaluate(withShopping,
                false, false, 1.25, false, 15.00,
                true, false, 20, false, 1.25, false, 10, true, false);
        require(!allowed.shouldReject && allowed.shouldAccept,
                "Shopping requirement should allow Shopping offer");
    }

    private static void shouldRequireNoShippingForAutoAccept() {
        CityPolicy.configure(false, false, false);
        String withShipping = "$30.00\n8 miles\nBIXBY\nShipping\nAccept\nReject";
        OfferEvaluator.Result blocked = OfferEvaluator.evaluate(withShipping,
                false, false, 1.25, false, 15.00,
                true, false, 20, false, 1.25, false, 10, false, true);
        require(!blocked.shouldReject && !blocked.shouldAccept,
                "Shipping presence should block no-Shipping auto-accept rule");

        String withoutShipping = "$30.00\n8 miles\nBIXBY\nCurbside pickup\nAccept\nReject";
        OfferEvaluator.Result allowed = OfferEvaluator.evaluate(withoutShipping,
                false, false, 1.25, false, 15.00,
                true, false, 20, false, 1.25, false, 10, false, true);
        require(!allowed.shouldReject && allowed.shouldAccept,
                "no-Shipping rule should allow offer without Shipping");
    }

    private static void shouldPreferEstimatedEarningsOverTip() {
        Double pay = OfferEvaluator.parseBestPay("Tip $5.00\nEstimated earnings $22.50\n15 miles\nBIXBY");
        require(pay != null && Math.abs(pay - 22.50) < 0.001, "pay context selection failed");
    }

    private static void shouldUseLargestMileage() {
        Double miles = OfferEvaluator.parseMiles("$25.00\n2.2 miles\n14.8 miles\nBIXBY");
        require(miles != null && Math.abs(miles - 14.8) < 0.001, "mileage selection failed");
    }

    private static void shouldWaitWhenMileageMissing() {
        OfferEvaluator.Result r = eval("$25.00\nBIXBY\nShopping\nAccept\nReject",
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
