package com.grimforsaken.sparkofferfilter;

import java.util.HashSet;
import java.util.Set;

public final class UnknownAndAutoAcceptLocationTest {
    public static void main(String[] args) {
        shouldWaitTwoSecondsBeforeManualReview();
        shouldKeepTimedOutOfferManualAfterLateLocation();
        shouldNotLockDifferentOfferToManualReview();
        shouldUseSeparateAutoAcceptStoreWhitelist();
        System.out.println("Unknown-location and Auto-Accept store tests passed.");
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

    private static void shouldUseSeparateAutoAcceptStoreWhitelist() {
        Set<String> accepted = new HashSet<>();
        accepted.add("WALMART#838");
        accepted.add("WALMART#1234");
        Set<String> auto = new HashSet<>();
        auto.add("WALMART#838");
        StoreSelectionPolicy.configure(accepted, auto);

        require(StoreSelectionPolicy.isAccepted("WALMART#1234"),
                "a store may be accepted without being auto-accepted");
        require(!StoreSelectionPolicy.isAutoAcceptAllowed("WALMART#1234"),
                "accepted-only store must not auto-accept");
        require(StoreSelectionPolicy.isAutoAcceptAllowed("WALMART#838"),
                "separately checked Auto-Accept store must allow automatic acceptance");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
