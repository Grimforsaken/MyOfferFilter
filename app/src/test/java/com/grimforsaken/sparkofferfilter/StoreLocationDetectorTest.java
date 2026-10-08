package com.grimforsaken.sparkofferfilter;

import java.util.List;

public final class StoreLocationDetectorTest {
    public static void main(String[] args) {
        shouldReadWalmartStore();
        shouldReadWrappedStoreLabel();
        shouldReadSamsClubStore();
        shouldFindSeveralStoresOnOneScreen();
        shouldIgnoreBareCityText();
        System.out.println("Store location detector tests passed.");
    }

    private static void shouldReadWalmartStore() {
        StoreLocationDetector.Store s = StoreLocationDetector.firstStore(
                "$32.14\nShopping\nWalmart TULSA #5093\nREJECT\nACCEPT");
        require(s != null, "Walmart store must be detected");
        require("WALMART#5093".equals(s.key), "Walmart store key");
        require("Walmart Tulsa #5093".equals(s.label), "Walmart display label");
    }

    private static void shouldReadWrappedStoreLabel() {
        StoreLocationDetector.Store s = StoreLocationDetector.firstStore(
                "Walmart SAND SPRINGS\n#838\nShopping\nREJECT\nACCEPT");
        require(s != null && "WALMART#838".equals(s.key), "wrapped store number must be detected");
        require("Walmart Sand Springs #838".equals(s.label), "wrapped label should be canonical");
    }

    private static void shouldReadSamsClubStore() {
        StoreLocationDetector.Store s = StoreLocationDetector.firstStore(
                "Sam's Club TULSA #6342\nShopping\nREJECT\nACCEPT");
        require(s != null && "SAMS_CLUB#6342".equals(s.key), "Sam's Club key");
        require("Sam's Club Tulsa #6342".equals(s.label), "Sam's Club label");
    }

    private static void shouldFindSeveralStoresOnOneScreen() {
        List<StoreLocationDetector.Store> stores = StoreLocationDetector.findStores(
                "Walmart TULSA #5093\n$20\nWalmart SAPULPA #1234\n$25");
        require(stores.size() == 2, "each visible store must be discovered separately");
    }

    private static void shouldIgnoreBareCityText() {
        require(StoreLocationDetector.firstStore("Tulsa\nSpark Zone\n$20") == null,
                "bare city/map text must not create a store");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
