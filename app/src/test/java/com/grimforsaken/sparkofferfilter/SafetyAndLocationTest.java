package com.grimforsaken.sparkofferfilter;

import java.util.HashSet;
import java.util.Set;

public final class SafetyAndLocationTest {
    public static void main(String[] args) {
        shouldLockRejectsForTenSecondsAfterAccept();
        shouldProtectOfferWhileAcceptIsPending();
        shouldProtectAcceptedOfferForSixtySeconds();
        shouldRequireStableRejectObservation();
        shouldDetectCityFromOklahomaAddressLine();
        shouldNotGuessCityFromZoneText();
        shouldReturnUnknownForMultipleAddressCities();
        shouldStartStoreWhitelistEmpty();
        shouldAllowOnlyCheckedStore();
        shouldKeepTwoStoresInSameCityIndependent();
        shouldRejectUncheckedSamsClubStore();
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
        guard.noteAcceptIntent("21.54:6.10:true:Walmart Sand Springs #838", now);
        require(guard.isRejectProtected("21.54:6.10:true:Walmart Sand Springs #838", now),
                "same offer must be protected as soon as an accept decision is known");
        require(guard.isRejectProtected("21.54:6.10:true:Walmart Sand Springs #838", now + 29_999L),
                "accept intent protection must remain active for 30 seconds");
        require(!guard.isRejectProtected("18.74:9.20:true:Walmart Sapulpa #1234", now + 1_000L),
                "a different offer must not inherit the accept protection");
    }

    private static void shouldProtectAcceptedOfferForSixtySeconds() {
        OfferDecisionGuard guard = new OfferDecisionGuard();
        long now = 5_000L;
        guard.noteAccepted("21.54:6.10:true:Walmart Sand Springs #838", now);
        require(guard.isRejectProtected("21.54:6.10:true:Walmart Sand Springs #838", now + 59_999L),
                "accepted offer must remain protected through its review window");
        require(!guard.isRejectProtected("21.54:6.10:true:Walmart Sand Springs #838", now + 60_000L),
                "accepted-offer identity protection should expire after 60 seconds");
    }

    private static void shouldRequireStableRejectObservation() {
        OfferDecisionGuard guard = new OfferDecisionGuard();
        String key = "22.54:13.90:true:Walmart Bixby #123";
        require(!guard.isRejectStable(key, "low rate", 10_000L),
                "first ordinary reject observation must not click immediately");
        require(!guard.isRejectStable(key, "low rate", 10_649L),
                "ordinary reject must remain pending before 650 ms");
        require(guard.isRejectStable(key, "low rate", 10_650L),
                "same ordinary reject decision should become actionable after 650 ms");
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

    private static void shouldStartStoreWhitelistEmpty() {
        StoreSelectionPolicy.configure(new HashSet<>(), new HashSet<>());
        OfferLocationPolicy.Decision d = OfferLocationPolicy.evaluate(
                "Walmart SAND SPRINGS #838\n$25.00\n8 miles\nREJECT\nACCEPT");
        require(d.identified && !d.allowed,
                "new dynamic store list must start with discovered stores unchecked");
    }

    private static void shouldAllowOnlyCheckedStore() {
        Set<String> accepted = new HashSet<>();
        accepted.add("WALMART#838");
        StoreSelectionPolicy.configure(accepted, new HashSet<>());
        OfferLocationPolicy.Decision d = OfferLocationPolicy.evaluate(
                "Walmart SAND SPRINGS #838\n$25.00\n8 miles\nREJECT\nACCEPT");
        require(d.identified && d.allowed && "WALMART#838".equals(d.storeKey),
                "checking a store must allow that exact store");
    }

    private static void shouldKeepTwoStoresInSameCityIndependent() {
        Set<String> accepted = new HashSet<>();
        accepted.add("WALMART#838");
        StoreSelectionPolicy.configure(accepted, new HashSet<>());
        OfferLocationPolicy.Decision other = OfferLocationPolicy.evaluate(
                "Walmart SAND SPRINGS #999\n$25.00\n8 miles\nREJECT\nACCEPT");
        require(other.identified && !other.allowed,
                "checking one store must not automatically allow another store in the same city");
    }

    private static void shouldRejectUncheckedSamsClubStore() {
        StoreSelectionPolicy.configure(new HashSet<>(), new HashSet<>());
        OfferLocationPolicy.Decision d = OfferLocationPolicy.evaluate(
                "Sam's Club TULSA #6342\n$30.00\n8 miles\nREJECT\nACCEPT");
        require(d.identified && !d.allowed && "SAMS_CLUB#6342".equals(d.storeKey),
                "an unchecked Sam's Club store must reject");
    }

    private static void shouldIgnoreBareTulsaZoneLabel() {
        StoreSelectionPolicy.configure(new HashSet<>(), new HashSet<>());
        OfferLocationPolicy.Decision d = OfferLocationPolicy.evaluate(
                "Tulsa\nSpark Zone\n$30.00\n8 miles");
        require(!d.identified,
                "bare Tulsa map/zone text must not create a store location");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
