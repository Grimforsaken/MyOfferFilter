package com.grimforsaken.sparkofferfilter;

import java.util.HashSet;
import java.util.Set;

public final class ImmediateLocationRejectTest {
    public static void main(String[] args) {
        shouldRejectUncheckedStoreWithoutStabilityDelay();
        shouldKeepStabilityDelayForNonLocationRejects();
        shouldAllowCheckedStore();
        shouldRejectUnselectedStoreInSameCity();
        shouldKeepAddressOnlyTextUnknown();
        System.out.println("Immediate location reject tests passed.");
    }

    private static void shouldRejectUncheckedStoreWithoutStabilityDelay() {
        StoreSelectionPolicy.configure(new HashSet<>(), new HashSet<>());
        OfferLocationPolicy.Decision location = OfferLocationPolicy.evaluate(
                "Walmart BIXBY #123\nShopping\nReject\nAccept");
        require(location.identified && !location.allowed && "WALMART#123".equals(location.storeKey),
                "Bixby store must be identified as an unchecked store");

        OfferDecisionGuard guard = new OfferDecisionGuard();
        String reason = location.location + " is not checked in Accepted Store Locations";
        require(guard.isRejectStable("?:?:true:Walmart Bixby #123", reason, 1_000L),
                "unchecked store must bypass the 650 ms stability delay on first observation");
    }

    private static void shouldKeepStabilityDelayForNonLocationRejects() {
        OfferDecisionGuard guard = new OfferDecisionGuard();
        require(!guard.isRejectStable("20.00:20.00:false:Walmart Sand Springs #838",
                        "$1.00/mi is below reject minimum $1.25/mi", 2_000L),
                "ordinary reject rules should retain the first-observation safety delay");
        require(!guard.isRejectStable("20.00:20.00:false:Walmart Sand Springs #838",
                        "$1.00/mi is below reject minimum $1.25/mi", 2_649L),
                "ordinary reject rule must wait the full 650 ms");
        require(guard.isRejectStable("20.00:20.00:false:Walmart Sand Springs #838",
                        "$1.00/mi is below reject minimum $1.25/mi", 2_650L),
                "ordinary reject rule becomes stable after 650 ms");
    }

    private static void shouldAllowCheckedStore() {
        Set<String> accepted = new HashSet<>();
        accepted.add("WALMART#838");
        StoreSelectionPolicy.configure(accepted, new HashSet<>());
        OfferLocationPolicy.Decision location = OfferLocationPolicy.evaluate(
                "Walmart SAND SPRINGS #838\nShopping\nReject\nAccept");
        require(location.identified && location.allowed,
                "selected store must pass the location gate");
    }

    private static void shouldRejectUnselectedStoreInSameCity() {
        Set<String> accepted = new HashSet<>();
        accepted.add("WALMART#838");
        StoreSelectionPolicy.configure(accepted, new HashSet<>());
        OfferLocationPolicy.Decision location = OfferLocationPolicy.evaluate(
                "Walmart SAND SPRINGS #839\nShopping\nReject\nAccept");
        require(location.identified && !location.allowed,
                "another store in the same city must still reject when unchecked");
    }

    private static void shouldKeepAddressOnlyTextUnknown() {
        StoreSelectionPolicy.configure(new HashSet<>(), new HashSet<>());
        OfferLocationPolicy.Decision location = OfferLocationPolicy.evaluate(
                "Tulsa, OK 74103\n$25.00\n8 miles");
        require(!location.identified,
                "city/address text without a reliable store label must remain Unknown");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
