package com.grimforsaken.sparkofferfilter;

import java.util.HashSet;
import java.util.Set;

public final class StrictWhitelistDetectionTest {
    public static void main(String[] args) {
        shouldRejectNewUncheckedWalmartStore();
        shouldAllowSelectedWalmartStore();
        shouldTreatDifferentStoreNumberAsDifferentLocation();
        shouldReadWrappedWalmartStoreLabel();
        shouldRejectUncheckedSamsClubStore();
        shouldKeepAddressOnlyLocationUnknown();
        shouldKeepBareMapLabelUnknown();
        System.out.println("Strict whitelist detection tests passed.");
    }

    private static void shouldRejectNewUncheckedWalmartStore() {
        StoreSelectionPolicy.configure(new HashSet<>(), new HashSet<>());
        OfferLocationPolicy.Decision d = OfferLocationPolicy.evaluate(
                "$32.14\n3 stops • 3.9 miles • 54 mins\nShopping\nWalmart TULSA #5093\nREJECT\nACCEPT");
        require(d.identified && !d.allowed && "WALMART#5093".equals(d.storeKey),
                "a newly discovered Walmart store must reject until selected");
    }

    private static void shouldAllowSelectedWalmartStore() {
        Set<String> accepted = new HashSet<>();
        accepted.add("WALMART#838");
        StoreSelectionPolicy.configure(accepted, new HashSet<>());
        OfferLocationPolicy.Decision d = OfferLocationPolicy.evaluate(
                "$25.00\nShopping\nWalmart SAND SPRINGS #838\nREJECT\nACCEPT");
        require(d.identified && d.allowed && "Walmart Sand Springs #838".equals(d.location),
                "selected Walmart store must pass the store whitelist");
    }

    private static void shouldTreatDifferentStoreNumberAsDifferentLocation() {
        Set<String> accepted = new HashSet<>();
        accepted.add("WALMART#838");
        StoreSelectionPolicy.configure(accepted, new HashSet<>());
        OfferLocationPolicy.Decision d = OfferLocationPolicy.evaluate(
                "$25.00\nShopping\nWalmart SAND SPRINGS #999\nREJECT\nACCEPT");
        require(d.identified && !d.allowed,
                "a different store number in the same city must remain unchecked");
    }

    private static void shouldReadWrappedWalmartStoreLabel() {
        Set<String> accepted = new HashSet<>();
        accepted.add("WALMART#838");
        StoreSelectionPolicy.configure(accepted, new HashSet<>());
        OfferLocationPolicy.Decision d = OfferLocationPolicy.evaluate(
                "Walmart SAND SPRINGS\n#838\nShopping\nREJECT\nACCEPT");
        require(d.identified && d.allowed,
                "store labels split across lines must match the same store key");
    }

    private static void shouldRejectUncheckedSamsClubStore() {
        StoreSelectionPolicy.configure(new HashSet<>(), new HashSet<>());
        OfferLocationPolicy.Decision d = OfferLocationPolicy.evaluate(
                "Sam's Club TULSA #6342\nShopping\nREJECT\nACCEPT");
        require(d.identified && !d.allowed && "SAMS_CLUB#6342".equals(d.storeKey),
                "Sam's Club must be store-specific too");
    }

    private static void shouldKeepAddressOnlyLocationUnknown() {
        StoreSelectionPolicy.configure(new HashSet<>(), new HashSet<>());
        OfferLocationPolicy.Decision d = OfferLocationPolicy.evaluate(
                "Pickup\n250 S Highway 97, Sand Springs, OK 74063\n$30.00\n8 miles");
        require(!d.identified,
                "address-only text must stay unknown because the new list is store-specific");
    }

    private static void shouldKeepBareMapLabelUnknown() {
        StoreSelectionPolicy.configure(new HashSet<>(), new HashSet<>());
        OfferLocationPolicy.Decision d = OfferLocationPolicy.evaluate(
                "Tulsa\nBroken Arrow\nOwasso\nMap\n$30.00\n8 miles");
        require(!d.identified,
                "bare map labels must not create discovered stores");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
