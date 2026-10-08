package com.grimforsaken.sparkofferfilter;

import java.util.HashSet;
import java.util.Set;

public final class WhitelistRejectPriorityTest {
    public static void main(String[] args) {
        shouldAllowSelectedStoreThroughLocationFilter();
        shouldRejectLowPayAtSelectedStore();
        shouldRejectTooManyMilesAtSelectedStore();
        shouldRejectLowRateAtSelectedStore();
        shouldApplyRejectRulesOnEstimatedTotalOffers();
        System.out.println("Whitelist reject-priority tests passed.");
    }

    private static void shouldAllowSelectedStoreThroughLocationFilter() {
        Set<String> accepted = new HashSet<>();
        accepted.add("WALMART#838");
        StoreSelectionPolicy.configure(accepted, new HashSet<>());
        OfferLocationPolicy.Decision d = OfferLocationPolicy.evaluate(
                "Walmart SAND SPRINGS #838\n$25.00\n8 miles\nREJECT\nACCEPT");
        require(d.identified && d.allowed,
                "selected store must pass the location whitelist");
    }

    private static void shouldRejectLowPayAtSelectedStore() {
        OfferEvaluator.Result r = OfferEvaluator.evaluate(
                "Walmart SAND SPRINGS #838\n$14.99\n8 miles\nShopping\nReject\nAccept",
                false, false, 1.25,
                true, 15.00,
                false, 20.0,
                true,
                true, 10.00,
                false, 1.25,
                false, 20.0,
                false, false);
        require(r.ready && r.shouldReject && !r.shouldAccept,
                "selected store must still reject when the minimum-dollar rule fails");
    }

    private static void shouldRejectTooManyMilesAtSelectedStore() {
        OfferEvaluator.Result r = OfferEvaluator.evaluate(
                "Walmart SAPULPA #1234\n$40.00\n15.1 miles\nShopping\nReject\nAccept",
                false, false, 1.25,
                false, 15.00,
                true, 15.0,
                true,
                true, 10.00,
                false, 1.25,
                false, 30.0,
                false, false);
        require(r.ready && r.shouldReject && !r.shouldAccept,
                "selected store must still reject when the maximum-mile rule fails");
    }

    private static void shouldRejectLowRateAtSelectedStore() {
        OfferEvaluator.Result r = OfferEvaluator.evaluate(
                "Walmart SAND SPRINGS #838\n$20.00\n20 miles\nShopping\nReject\nAccept",
                false, true, 1.25,
                false, 15.00,
                false, 20.0,
                true,
                true, 10.00,
                false, 1.00,
                false, 30.0,
                false, false);
        require(r.ready && r.shouldReject && !r.shouldAccept,
                "selected store must still reject when the dollars-per-mile rule fails");
    }

    private static void shouldApplyRejectRulesOnEstimatedTotalOffers() {
        OfferEvaluator.Result r = OfferEvaluator.evaluate(
                "Walmart SAND SPRINGS #838\nEstimated total\n$10.00\n20 miles\nShopping\nReject\nAccept",
                false, true, 1.25,
                true, 15.00,
                true, 15.0,
                false,
                false, 20.00,
                false, 1.25,
                false, 20.0,
                false, false);
        require(r.ready && r.shouldReject,
                "offer evaluation should still identify reject rules before the service applies Estimated-total safety");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
