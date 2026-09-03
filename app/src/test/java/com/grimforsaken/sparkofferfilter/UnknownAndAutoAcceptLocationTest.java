package com.grimforsaken.sparkofferfilter;

public final class UnknownAndAutoAcceptLocationTest {
    public static void main(String[] args) {
        shouldWaitTwoSecondsBeforeManualReview();
        shouldKeepTimedOutOfferManualAfterLateLocation();
        shouldNotLockDifferentOfferToManualReview();
        shouldUseSeparateAutoAcceptWhitelist();
        System.out.println("Unknown-location and Auto-Accept location tests passed.");
    }

    private static void shouldWaitTwoSecondsBeforeManualReview() {
        UnknownLocationGuard guard = new UnknownLocationGuard();
        String key = "21.54:6.10:true";
        require(!guard.shouldLeaveForManualReview(key, 1_000L),
                "first unknown scan must wait rather than force manual review");
        require(!guard.shouldLeaveForManualReview(key, 2_999L),
                "unknown location must wait the full two seconds");
        require(guard.shouldLeaveForManualReview(key, 3_000L),
                "location still unknown at two seconds must go to manual review");
    }

    private static void shouldKeepTimedOutOfferManualAfterLateLocation() {
        UnknownLocationGuard guard = new UnknownLocationGuard();
        String key = "19.98:16.40:false";
        guard.shouldLeaveForManualReview(key, 10_000L);
        require(guard.shouldLeaveForManualReview(key, 12_000L),
                "offer should time out to manual review at two seconds");
        guard.onLocationIdentified(key);
        require(guard.isManualReviewLocked(key, 12_100L),
                "a late location update must not resume automatic actions for a timed-out offer");
    }

    private static void shouldNotLockDifferentOfferToManualReview() {
        UnknownLocationGuard guard = new UnknownLocationGuard();
        guard.shouldLeaveForManualReview("19.98:16.40:false", 20_000L);
        guard.shouldLeaveForManualReview("19.98:16.40:false", 22_000L);
        require(!guard.isManualReviewLocked("25.00:8.00:true", 22_100L),
                "manual-review lock must not carry to a different offer");
    }

    private static void shouldUseSeparateAutoAcceptWhitelist() {
        AutoAcceptCityPolicy.configure(false, false, false, false, true, true);
        require(AutoAcceptCityPolicy.isAllowed("Sand Springs"),
                "Sand Springs should be enabled by default in Auto-Accept locations");
        require(AutoAcceptCityPolicy.isAllowed("Sapulpa"),
                "Sapulpa should be enabled by default in Auto-Accept locations");
        require(!AutoAcceptCityPolicy.isAllowed("Tulsa"),
                "Tulsa must not auto-accept unless its separate Auto-Accept checkbox is checked");

        AutoAcceptCityPolicy.configure(true, false, false, false, true, true);
        require(AutoAcceptCityPolicy.isAllowed("Tulsa"),
                "checking Tulsa in Auto-Accept locations must allow Tulsa auto-acceptance");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
