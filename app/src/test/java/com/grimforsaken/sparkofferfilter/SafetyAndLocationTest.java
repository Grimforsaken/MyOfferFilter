package com.grimforsaken.sparkofferfilter;

public final class SafetyAndLocationTest {
    public static void main(String[] args) {
        shouldLockRejectsForTenSecondsAfterAccept();
        shouldProtectOfferWhileAcceptIsPending();
        shouldProtectAcceptedOfferForSixtySeconds();
        shouldRequireStableRejectObservation();
        shouldDetectCityFromOklahomaAddressLine();
        shouldNotGuessCityFromZoneText();
        shouldReturnUnknownForMultipleAddressCities();
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

    private static void shouldProtectOfferWhileAcceptIsPending() {
        OfferDecisionGuard guard = new OfferDecisionGuard();
        long now = 2_000L;
        guard.noteAcceptIntent("21.54:6.10:true", now);
        require(guard.isRejectProtected("21.54:6.10:true", now),
                "same offer must be protected as soon as an accept decision is known");
        require(guard.isRejectProtected("21.54:6.10:true", now + 29_999L),
                "accept intent protection must remain active for 30 seconds");
        require(!guard.isRejectProtected("18.74:9.20:true", now + 1_000L),
                "a different offer must not inherit the accept protection");
    }

    private static void shouldProtectAcceptedOfferForSixtySeconds() {
        OfferDecisionGuard guard = new OfferDecisionGuard();
        long now = 5_000L;
        guard.noteAccepted("21.54:6.10:true", now);
        require(guard.isRejectProtected("21.54:6.10:true", now + 59_999L),
                "accepted offer must remain protected through its review window");
        require(!guard.isRejectProtected("21.54:6.10:true", now + 60_000L),
                "accepted-offer identity protection should expire after 60 seconds");
    }

    private static void shouldRequireStableRejectObservation() {
        OfferDecisionGuard guard = new OfferDecisionGuard();
        String key = "22.54:13.90:true";
        require(!guard.isRejectStable(key, "low rate", 10_000L),
                "first reject observation must not click immediately");
        require(!guard.isRejectStable(key, "low rate", 10_649L),
                "reject must remain pending before 650 ms");
        require(guard.isRejectStable(key, "low rate", 10_650L),
                "same reject decision should become actionable after 650 ms");
        require(!guard.isRejectStable(key, "Tulsa", 10_700L),
                "a changed reject reason must restart safety verification");
    }

    private static void shouldDetectCityFromOklahomaAddressLine() {
        String text = "Store #123\nSand Springs, OK 74063\nShopping\n$21.54\n6.1 miles";
        require("Sand Springs".equals(OfferCityDetector.detect(text)),
                "accepted log should detect Sand Springs from an address-style city/state line");
    }

    private static void shouldNotGuessCityFromZoneText() {
        String text = "Tulsa\nSpark Zone\n$21.54\n6.1 miles\nShopping";
        require("Unknown".equals(OfferCityDetector.detect(text)),
                "a bare Tulsa zone/header must not be logged as the order city");
    }

    private static void shouldReturnUnknownForMultipleAddressCities() {
        String text = "Sand Springs, OK 74063\nTulsa, OK 74103\n$21.54\n6.1 miles";
        require("Unknown".equals(OfferCityDetector.detect(text)),
                "multiple address cities should be Unknown rather than guessed");
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
