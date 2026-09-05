package com.grimforsaken.sparkofferfilter;

public final class ImmediateLocationRejectTest {
    public static void main(String[] args) {
        shouldRejectUncheckedLocationWithoutStabilityDelay();
        shouldKeepStabilityDelayForNonLocationRejects();
        shouldUseFirstReliablePickupAddress();
        shouldPrioritizeSamsClubCheckboxOverCity();
        shouldKeepCheckedDefaultsAllowed();
        shouldIgnoreBareZoneLabel();
        System.out.println("Immediate location reject tests passed.");
    }

    private static void shouldRejectUncheckedLocationWithoutStabilityDelay() {
        CityPolicy.configure(false, false, false, false, true, true);
        OfferLocationPolicy.Decision location = OfferLocationPolicy.evaluate(
                "Store #123\nBixby, OK 74008\nReject\nAccept");
        require(location.identified && !location.allowed && "Bixby".equals(location.location),
                "Bixby must be identified as an unchecked location");

        OfferDecisionGuard guard = new OfferDecisionGuard();
        String reason = location.location + " is not checked in Accepted Locations";
        require(guard.isRejectStable("?:?:false:Bixby", reason, 1_000L),
                "unchecked location must bypass the 650 ms stability delay on the first observation");
    }

    private static void shouldKeepStabilityDelayForNonLocationRejects() {
        OfferDecisionGuard guard = new OfferDecisionGuard();
        require(!guard.isRejectStable("20.00:20.00:false:Sand Springs",
                        "$1.00/mi is below reject minimum $1.25/mi", 2_000L),
                "ordinary reject rules should retain the first-observation safety delay");
        require(!guard.isRejectStable("20.00:20.00:false:Sand Springs",
                        "$1.00/mi is below reject minimum $1.25/mi", 2_649L),
                "ordinary reject rule must wait the full 650 ms");
        require(guard.isRejectStable("20.00:20.00:false:Sand Springs",
                        "$1.00/mi is below reject minimum $1.25/mi", 2_650L),
                "ordinary reject rule becomes stable after 650 ms");
    }

    private static void shouldUseFirstReliablePickupAddress() {
        CityPolicy.configure(false, false, false, false, true, true);
        String screen = "Pickup\nBixby, OK 74008\nDrop off\nTulsa, OK 74103\n$30.00\n8 miles";
        OfferLocationPolicy.Decision location = OfferLocationPolicy.evaluate(screen);
        require(location.identified && "Bixby".equals(location.location) && !location.allowed,
                "multiple visible addresses must use the first reliable pickup/store address instead of becoming Unknown");
    }

    private static void shouldPrioritizeSamsClubCheckboxOverCity() {
        CityPolicy.configure(false, false, false, false, true, true);
        OfferLocationPolicy.Decision blocked = OfferLocationPolicy.evaluate(
                "Sam's Club #6342\nSand Springs, OK 74063\n$30.00\n8 miles");
        require(blocked.identified && "Sam's Club".equals(blocked.location) && !blocked.allowed,
                "unchecked Sam's Club must reject even if the city itself is checked");

        CityPolicy.configure(false, false, false, true, true, true);
        OfferLocationPolicy.Decision allowed = OfferLocationPolicy.evaluate(
                "Sam's Club #6342\nSand Springs, OK 74063\n$30.00\n8 miles");
        require(allowed.identified && allowed.allowed,
                "checked Sam's Club must pass the location whitelist");
    }

    private static void shouldKeepCheckedDefaultsAllowed() {
        CityPolicy.configure(false, false, false, false, true, true);
        require(OfferLocationPolicy.evaluate("Sand Springs, OK 74063\n$25.00\n8 miles").allowed,
                "Sand Springs remains checked by default");
        require(OfferLocationPolicy.evaluate("Sapulpa, OK 74066\n$25.00\n8 miles").allowed,
                "Sapulpa remains checked by default");
    }

    private static void shouldIgnoreBareZoneLabel() {
        CityPolicy.configure(false, false, false, false, true, true);
        OfferLocationPolicy.Decision location = OfferLocationPolicy.evaluate(
                "Tulsa\nSpark Zone\n$25.00\n8 miles");
        require(!location.identified,
                "bare Tulsa zone/header text must still be Unknown rather than a false location reject");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
