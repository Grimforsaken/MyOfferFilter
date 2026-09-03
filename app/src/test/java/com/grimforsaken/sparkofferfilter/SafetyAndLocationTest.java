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
        shouldAllowSandSpringsByDefault();
        shouldAllowSapulpaByDefault();
        shouldRejectBixbyWhenNotOnWhitelist();
        shouldRejectTulsaWhenUnchecked();
        shouldAllowTulsaWhenChecked();
        shouldRejectSamsClubWhenUnchecked();
        shouldAllowSamsClubWhenChecked();
        shouldIgnoreBareTulsaZoneLabel();
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
        guard.noteAcceptIntent("21.54:6.10:true:Sand Springs", now);
        require(guard.isRejectProtected("21.54:6.10:true:Sand Springs", now),
                "same offer must be protected as soon as an accept decision is known");
        require(guard.isRejectProtected("21.54:6.10:true:Sand Springs", now + 29_999L),
                "accept intent protection must remain active for 30 seconds");
        require(!guard.isRejectProtected("18.74:9.20:true:Sapulpa", now + 1_000L),
                "a different offer must not inherit the accept protection");
    }

    private static void shouldProtectAcceptedOfferForSixtySeconds() {
        OfferDecisionGuard guard = new OfferDecisionGuard();
        long now = 5_000L;
        guard.noteAccepted("21.54:6.10:true:Sand Springs", now);
        require(guard.isRejectProtected("21.54:6.10:true:Sand Springs", now + 59_999L),
                "accepted offer must remain protected through its review window");
        require(!guard.isRejectProtected("21.54:6.10:true:Sand Springs", now + 60_000L),
                "accepted-offer identity protection should expire after 60 seconds");
    }

    private static void shouldRequireStableRejectObservation() {
        OfferDecisionGuard guard = new OfferDecisionGuard();
        String key = "22.54:13.90:true:Bixby";
        require(!guard.isRejectStable(key, "location not checked", 10_000L),
                "first reject observation must not click immediately");
        require(!guard.isRejectStable(key, "location not checked", 10_649L),
                "reject must remain pending before 650 ms");
        require(guard.isRejectStable(key, "location not checked", 10_650L),
                "same reject decision should become actionable after 650 ms");
        require(!guard.isRejectStable(key, "low rate", 10_700L),
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

    private static void shouldAllowSandSpringsByDefault() {
        CityPolicy.configure(false, false, false, false, true, true);
        OfferLocationPolicy.Decision d = OfferLocationPolicy.evaluate("Sand Springs, OK 74063\n$25.00\n8 miles");
        require(d.identified && d.allowed && "Sand Springs".equals(d.location),
                "Sand Springs should be checked by default");
    }

    private static void shouldAllowSapulpaByDefault() {
        CityPolicy.configure(false, false, false, false, true, true);
        OfferLocationPolicy.Decision d = OfferLocationPolicy.evaluate("Sapulpa, OK 74066\n$25.00\n8 miles");
        require(d.identified && d.allowed && "Sapulpa".equals(d.location),
                "Sapulpa should be checked by default");
    }

    private static void shouldRejectBixbyWhenNotOnWhitelist() {
        CityPolicy.configure(false, false, false, false, true, true);
        OfferLocationPolicy.Decision d = OfferLocationPolicy.evaluate("Bixby, OK 74008\n$25.00\n8 miles");
        require(d.identified && !d.allowed && "Bixby".equals(d.location),
                "reliably identified locations outside the whitelist must reject");
    }

    private static void shouldRejectTulsaWhenUnchecked() {
        CityPolicy.configure(false, false, false, false, true, true);
        OfferLocationPolicy.Decision d = OfferLocationPolicy.evaluate("Tulsa, OK 74103\n$30.00\n8 miles");
        require(d.identified && !d.allowed, "Tulsa should reject while unchecked");
    }

    private static void shouldAllowTulsaWhenChecked() {
        CityPolicy.configure(true, false, false, false, true, true);
        OfferLocationPolicy.Decision d = OfferLocationPolicy.evaluate("Tulsa, OK 74103\n$30.00\n8 miles");
        require(d.identified && d.allowed, "Tulsa should pass when checked");
    }

    private static void shouldRejectSamsClubWhenUnchecked() {
        CityPolicy.configure(false, false, false, false, true, true);
        OfferLocationPolicy.Decision d = OfferLocationPolicy.evaluate("Sam's Club #6342\n$30.00\n8 miles");
        require(d.identified && !d.allowed && "Sam's Club".equals(d.location),
                "Sam's Club should reject while unchecked");
    }

    private static void shouldAllowSamsClubWhenChecked() {
        CityPolicy.configure(false, false, false, true, true, true);
        OfferLocationPolicy.Decision d = OfferLocationPolicy.evaluate("Sam's Club #6342\n$30.00\n8 miles");
        require(d.identified && d.allowed, "Sam's Club should pass when checked");
    }

    private static void shouldIgnoreBareTulsaZoneLabel() {
        CityPolicy.configure(false, false, false, false, true, true);
        OfferLocationPolicy.Decision d = OfferLocationPolicy.evaluate("Tulsa\nSpark Zone\n$30.00\n8 miles");
        require(!d.identified,
                "bare Tulsa map/zone text must not be used as the order location");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
