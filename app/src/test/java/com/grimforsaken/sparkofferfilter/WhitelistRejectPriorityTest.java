package com.grimforsaken.sparkofferfilter;

public final class WhitelistRejectPriorityTest {
    public static void main(String[] args) {
        shouldKeepSandSpringsAndSapulpaAllowedByDefault();
        shouldRejectLowPayInSandSprings();
        shouldRejectTooManyMilesInSapulpa();
        shouldRejectLowRateInSandSprings();
        shouldKeepEstimatedTotalSafetyException();
        System.out.println("Whitelist reject-priority tests passed.");
    }

    private static void shouldKeepSandSpringsAndSapulpaAllowedByDefault() {
        CityPolicy.configure(false, false, false, false, true, true);
        OfferLocationPolicy.Decision sandSprings = OfferLocationPolicy.evaluate(
                "Sand Springs, OK 74063\n$25.00\n8 miles");
        OfferLocationPolicy.Decision sapulpa = OfferLocationPolicy.evaluate(
                "Sapulpa, OK 74066\n$25.00\n8 miles");
        require(sandSprings.identified && sandSprings.allowed,
                "Sand Springs must be checked on the Accepted Locations whitelist by default");
        require(sapulpa.identified && sapulpa.allowed,
                "Sapulpa must be checked on the Accepted Locations whitelist by default");
    }

    private static void shouldRejectLowPayInSandSprings() {
        OfferEvaluator.Result r = OfferEvaluator.evaluate(
                "Sand Springs, OK 74063\n$14.99\n8 miles\nShopping\nReject\nAccept",
                false, false, 1.25,
                true, 15.00,
                false, 20.0,
                true,
                true, 10.00,
                false, 1.25,
                false, 20.0,
                false, false);
        require(r.ready && r.shouldReject && !r.shouldAccept,
                "Sand Springs must still reject when the minimum-dollar rule fails");
    }

    private static void shouldRejectTooManyMilesInSapulpa() {
        OfferEvaluator.Result r = OfferEvaluator.evaluate(
                "Sapulpa, OK 74066\n$40.00\n15.1 miles\nShopping\nReject\nAccept",
                false, false, 1.25,
                false, 15.00,
                true, 15.0,
                true,
                true, 10.00,
                false, 1.25,
                false, 30.0,
                false, false);
        require(r.ready && r.shouldReject && !r.shouldAccept,
                "Sapulpa must still reject when the maximum-mile rule fails");
    }

    private static void shouldRejectLowRateInSandSprings() {
        OfferEvaluator.Result r = OfferEvaluator.evaluate(
                "Sand Springs, OK 74063\n$20.00\n20 miles\nShopping\nReject\nAccept",
                false, true, 1.25,
                false, 15.00,
                false, 20.0,
                true,
                true, 10.00,
                false, 1.00,
                false, 30.0,
                false, false);
        require(r.ready && r.shouldReject && !r.shouldAccept,
                "Sand Springs must still reject when the dollars-per-mile rule fails");
    }

    private static void shouldKeepEstimatedTotalSafetyException() {
        OfferEvaluator.Result r = OfferEvaluator.evaluate(
                "Sand Springs, OK 74063\nEstimated total\n$10.00\n20 miles\nShopping\nReject\nAccept",
                false, true, 1.25,
                true, 15.00,
                true, 15.0,
                false,
                false, 20.00,
                false, 1.25,
                false, 20.0,
                false, false);
        require(r.ready && !r.shouldReject,
                "Estimated total screen must remain protected from auto-reject");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
