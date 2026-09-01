package com.grimforsaken.sparkofferfilter;

public final class SafetyAndLocationTest {
    public static void main(String[] args) {
        shouldLockRejectsForTenSecondsAfterAccept();
        shouldRejectSamsClubByDefault();
        shouldAllowSamsClubWhenChecked();
        System.out.println("Safety and location tests passed.");
    }

    private static void shouldLockRejectsForTenSecondsAfterAccept() {
        ActionSafetyGuard guard = new ActionSafetyGuard();
        long acceptedAt = 1_000L;
        guard.onAccepted(acceptedAt);
        require(guard.isRejectLocked(acceptedAt), "lockout should begin immediately");
        require(guard.isRejectLocked(acceptedAt + 9_999L), "lockout must last the full 10 seconds");
        require(!guard.isRejectLocked(acceptedAt + 10_000L), "lockout should end after 10 seconds");
    }

    private static void shouldRejectSamsClubByDefault() {
        CityPolicy.configure(false, false, false, false);
        OfferEvaluator.Result r = OfferEvaluator.evaluate(
                "$30.00\n8 miles\nSam's Club\nShopping\nAccept\nReject",
                false, false, 1.25, false, 15.00,
                true, true, 20, false, 1.25, false, 10,
                false, false);
        require(r.ready && r.shouldReject && !r.shouldAccept, "Sam's Club must reject by default");
    }

    private static void shouldAllowSamsClubWhenChecked() {
        CityPolicy.configure(false, false, false, true);
        OfferEvaluator.Result r = OfferEvaluator.evaluate(
                "$30.00\n8 miles\nSam's Club\nShopping\nAccept\nReject",
                false, false, 1.25, false, 15.00,
                false, false, 20, false, 1.25, false, 10,
                false, false);
        CityPolicy.configure(false, false, false, false);
        require(r.ready && !r.shouldReject, "Sam's Club should pass location filter when allowed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
