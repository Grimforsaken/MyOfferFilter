package com.grimforsaken.sparkofferfilter;

public final class EstimatedTotalScreenTest {
    public static void main(String[] args) {
        shouldNeverRejectEstimatedTotalScreen();
        shouldStillAllowAutoAcceptOnEstimatedTotalScreen();
        shouldNotConfuseEstimatedEarningsWithEstimatedTotal();
        System.out.println("Estimated total screen tests passed.");
    }

    private static void shouldNeverRejectEstimatedTotalScreen() {
        String screen = "2 stops\n16.4 miles\nEstimated total\n$19.98\nBase $11.98\nBoost $8.00\nREJECT\nACCEPT";
        OfferEvaluator.Result r = OfferEvaluator.evaluate(
                screen,
                true, true, 1.75,
                true, 25.00,
                true, 10.0,
                false,
                false, 20.00,
                false, 1.25,
                false, 20.0,
                false, false);
        require(r.ready, "Estimated total screen should still be evaluable");
        require(!r.shouldReject, "Estimated total screen must never auto-reject");
        require(r.reason.contains("automatic rejection is disabled"),
                "Estimated total suppression should be visible in the reason");
    }

    private static void shouldStillAllowAutoAcceptOnEstimatedTotalScreen() {
        String screen = "2 stops\n16.4 miles\nEstimated total\n$19.98\nREJECT\nACCEPT";
        OfferEvaluator.Result r = OfferEvaluator.evaluate(
                screen,
                true, true, 1.75,
                true, 25.00,
                true, 10.0,
                true,
                true, 15.00,
                false, 1.25,
                true, 20.0,
                false, false);
        require(!r.shouldReject, "Estimated total screen must not reject even when reject rules fail");
        require(r.shouldAccept, "Auto-Accept should remain available on Estimated total screen when accept rules pass");
    }

    private static void shouldNotConfuseEstimatedEarningsWithEstimatedTotal() {
        String screen = "Estimated earnings $19.98\n16.4 miles\nREJECT\nACCEPT";
        OfferEvaluator.Result r = OfferEvaluator.evaluate(
                screen,
                false, true, 1.75,
                false, 15.00,
                false, 20.0,
                false,
                false, 20.00,
                false, 1.25,
                false, 20.0,
                false, false);
        require(r.shouldReject, "Only the exact Estimated total screen should suppress auto-reject");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
