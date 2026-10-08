package com.grimforsaken.sparkofferfilter;

final class OfferLocationPolicy {
    private OfferLocationPolicy() {}

    static Decision evaluate(String currentTreeText) {
        if (currentTreeText == null || currentTreeText.trim().isEmpty()) {
            return Decision.unknown();
        }

        StoreLocationDetector.Store store = StoreLocationDetector.firstStore(currentTreeText);
        if (store == null) {
            // Deliberately do not fall back to a bare city, map label, zone header, or
            // unrelated delivery address. The new whitelist is store-specific and is
            // populated only from reliable Walmart / Sam's Club store labels.
            return Decision.unknown();
        }

        return Decision.identified(
                store.key,
                store.label,
                StoreSelectionPolicy.isAccepted(store.key));
    }

    static final class Decision {
        final boolean identified;
        final boolean allowed;
        final String location;
        final String storeKey;
        final String reason;

        private Decision(boolean identified, boolean allowed, String location,
                         String storeKey, String reason) {
            this.identified = identified;
            this.allowed = allowed;
            this.location = location;
            this.storeKey = storeKey == null ? "" : storeKey;
            this.reason = reason;
        }

        static Decision identified(String storeKey, String location, boolean allowed) {
            return new Decision(true, allowed, location, storeKey,
                    allowed
                            ? location + " is checked in Accepted Store Locations"
                            : location + " is not checked in Accepted Store Locations");
        }

        // Kept for source compatibility with older tests/helpers.
        static Decision identified(String location, boolean allowed) {
            return identified("", location, allowed);
        }

        static Decision unknown() {
            return new Decision(false, false, "Unknown", "",
                    "Waiting for a reliable Walmart or Sam's Club store location.");
        }

        static Decision ambiguous(String reason) {
            return new Decision(false, false, "Unknown", "",
                    reason + "; waiting for a reliable store location.");
        }
    }
}
