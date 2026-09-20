package com.grimforsaken.sparkofferfilter;

public final class AcceptedShoppingScreenDetectorTest {
    public static void main(String[] args) {
        String screen = "Stop #1 for Trip 8466\n2:38 PM shopping\n"
                + "Walmart SAND SPRINGS #838\n220 S HIGHWAY 97\n"
                + "SAND SPRINGS, OK 74063-65\nCONTACT\nNAVIGATE\nCONFIRM ARRIVAL";
        require(AcceptedShoppingScreenDetector.isConfirmedShoppingTripScreen(screen),
                "active shopping stop screen must confirm an accepted shopping order");
        require("8466".equals(AcceptedShoppingScreenDetector.tripId(screen)), "trip id must parse");
        require("838".equals(AcceptedShoppingScreenDetector.storeNumber(screen)), "store number must parse");
        require(!AcceptedShoppingScreenDetector.isConfirmedShoppingTripScreen(
                "$32.14\n3 stops • 3.9 miles • 54 mins\nShopping\nWalmart TULSA #5093\nREJECT\nACCEPT"),
                "ordinary offer card must not count as accepted");
        require(!AcceptedShoppingScreenDetector.isConfirmedShoppingTripScreen(
                "Stop #1 for Trip 8466\nWalmart SAND SPRINGS #838\nCONFIRM ARRIVAL"),
                "non-shopping active stop must not count as shopping confirmation");
        System.out.println("Accepted shopping screen detector tests passed.");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
