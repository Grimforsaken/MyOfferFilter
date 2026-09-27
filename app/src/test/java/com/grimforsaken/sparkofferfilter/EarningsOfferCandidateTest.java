package com.grimforsaken.sparkofferfilter;

public final class EarningsOfferCandidateTest {
    public static void main(String[] args) {
        String single = "$32.14\n3 stops • 3.9 miles • 54 mins\n"
                + "Shopping\nWalmart TULSA #5093\nREJECT\nACCEPT";
        require(EarningsOfferCandidate.isCompleteShoppingOfferCard(single),
                "a complete individual Shopping offer card must be trackable");

        String active = "Stop #1 for Trip 8466\n2:38 PM shopping\n"
                + "Walmart SAND SPRINGS #838\nCONFIRM ARRIVAL";
        require(!EarningsOfferCandidate.isCompleteShoppingOfferCard(active),
                "active accepted-trip screen is confirmation, not an offer candidate");

        String incomplete = "Shopping\nWalmart TULSA #5093\nREJECT\nACCEPT";
        require(!EarningsOfferCandidate.isCompleteShoppingOfferCard(incomplete),
                "candidate must have pay, miles, and trip time");

        System.out.println("Earnings offer candidate tests passed.");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
