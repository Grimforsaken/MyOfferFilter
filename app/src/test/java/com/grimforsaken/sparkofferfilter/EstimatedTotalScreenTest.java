package com.grimforsaken.sparkofferfilter;

public final class EstimatedTotalScreenTest {
    public static void main(String[] args) {
        shouldExposeEstimatedTotalSafetyHeading();
        shouldRejectLowPayOnEstimatedTotalScreen();
        shouldRejectTooManyMilesOnEstimatedTotalScreen();
        shouldRejectLowRateOnEstimatedTotalScreen();
        shouldStillAllowAutoAcceptWhenRejectRulesPass();
        System.out.println("Estimated total offer tests passed.");
    }

    private static void shouldExposeEstimatedTotalSafetyHeading() {
        String screen = "3 stops • 13.7 miles • 54 mins\nEstimated total\n$30.83\nBase\n$16.10\nBoost\n$8.00\nTips (estimated)\n$6.73\nREJECT\nACCEPT";
        require(OfferEvaluator.normalize(screen).contains("ESTIMATED TOTAL"),
                "the Accessibility service must be able to recognize the Estimated total screen and suppress Reject");
    }

    private static void shouldRejectLowPayOnEstimatedTotalScreen() {
        String screen = "2 stops\n8.0 miles\nEstimated total\n$14.00\nSand Springs, OK 74063\nREJECT\nACCEPT";
        OfferEvaluator.Result r = OfferEvaluator.evaluate(
                screen,
                false, false, 1.25,
                true, 15.00,
                false, 20.0,
                false,
                false, 20.00,
                false, 1.25,
                false, 20.0,
                false, false);
        require(r.ready && r.shouldReject && !r.shouldAccept,
                "the evaluator may detect a reject rule, while the service-level Estimated total safety blocks the click");
    }

    private static void shouldRejectTooManyMilesOnEstimatedTotalScreen() {
        String screen = "2 stops\n16.4 miles\nEstimated total\n$30.00\nSapulpa, OK 74066\nREJECT\nACCEPT";
        OfferEvaluator.Result r = OfferEvaluator.evaluate(
                screen,
                false, false, 1.25,
                false, 15.00,
                true, 15.0,
                false,
                false, 20.00,
                false, 1.25,
                false, 20.0,
                false, false);
        require(r.ready && r.shouldReject,
                "the evaluator may flag maximum miles before the service-level Estimated total safety suppresses Reject");
    }

    private static void shouldRejectLowRateOnEstimatedTotalScreen() {
        String screen = "2 stops\n20.0 miles\nEstimated total\n$20.00\nSand Springs, OK 74063\nREJECT\nACCEPT";
        OfferEvaluator.Result r = OfferEvaluator.evaluate(
                screen,
                false, true, 1.25,
                false, 15.00,
                false, 25.0,
                false,
                false, 20.00,
                false, 1.25,
                false, 20.0,
                false, false);
        require(r.ready && r.shouldReject && r.dollarsPerMile != null
                        && r.dollarsPerMile < 1.25,
                "the evaluator may flag low dollars-per-mile before the service-level Estimated total safety suppresses Reject");
    }

    private static void shouldStillAllowAutoAcceptWhenRejectRulesPass() {
        String screen = "2 stops\n8.0 miles\nEstimated total\n$30.00\nSand Springs, OK 74063\nShopping\nREJECT\nACCEPT";
        OfferEvaluator.Result r = OfferEvaluator.evaluate(
                screen,
                false, true, 1.25,
                true, 15.00,
                true, 20.0,
                true,
                true, 20.00,
                true, 1.25,
                true, 10.0,
                true, false);
        require(!r.shouldReject && r.shouldAccept,
                "Auto-Accept should remain available when every reject and accept rule passes");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
