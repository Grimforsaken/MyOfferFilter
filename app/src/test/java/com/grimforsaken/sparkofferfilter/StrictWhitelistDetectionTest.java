package com.grimforsaken.sparkofferfilter;

public final class StrictWhitelistDetectionTest {
    public static void main(String[] args) {
        shouldRejectUnlistedCityFromNormalAddressLine();
        shouldRejectUnlistedCityFromFullStreetAddress();
        shouldRejectUnlistedCityWhenStateIsOnNextLine();
        shouldRejectExplicitLabeledCity();
        shouldAllowCheckedSandSpringsAcrossAddressFormats();
        shouldKeepBareMapLabelUnknown();
        System.out.println("Strict whitelist detection tests passed.");
    }

    private static void shouldRejectUnlistedCityFromNormalAddressLine() {
        CityPolicy.configure(false, false, false, false, true, true);
        OfferLocationPolicy.Decision d = OfferLocationPolicy.evaluate(
                "Bixby, OK 74008\n$30.00\n8 miles\nReject\nAccept");
        require(d.identified && !d.allowed && "Bixby".equals(d.location),
                "an identified city outside the checked whitelist must reject");
    }

    private static void shouldRejectUnlistedCityFromFullStreetAddress() {
        CityPolicy.configure(false, false, false, false, true, true);
        OfferLocationPolicy.Decision d = OfferLocationPolicy.evaluate(
                "Pickup\n123 Main Street, Broken Arrow, OK 74012\n$30.00\n8 miles");
        require(d.identified && !d.allowed && "Broken Arrow".equals(d.location),
                "a full street address for an unchecked city must reject");
    }

    private static void shouldRejectUnlistedCityWhenStateIsOnNextLine() {
        CityPolicy.configure(false, false, false, false, true, true);
        OfferLocationPolicy.Decision d = OfferLocationPolicy.evaluate(
                "Pickup location\nOwasso\nOK 74055\n$30.00\n8 miles");
        require(d.identified && !d.allowed && "Owasso".equals(d.location),
                "a split city/state address for an unchecked city must reject");
    }

    private static void shouldRejectExplicitLabeledCity() {
        CityPolicy.configure(false, false, false, false, true, true);
        OfferLocationPolicy.Decision d = OfferLocationPolicy.evaluate(
                "Store City: Claremore\n$30.00\n8 miles");
        require(d.identified && !d.allowed && "Claremore".equals(d.location),
                "an explicit reliable city label outside the whitelist must reject");
    }

    private static void shouldAllowCheckedSandSpringsAcrossAddressFormats() {
        CityPolicy.configure(false, false, false, false, true, true);
        OfferLocationPolicy.Decision d = OfferLocationPolicy.evaluate(
                "Pickup\n250 S Highway 97, Sand Springs, OK 74063\n$30.00\n8 miles");
        require(d.identified && d.allowed && "Sand Springs".equals(d.location),
                "Sand Springs must remain allowed when checked by default");
    }

    private static void shouldKeepBareMapLabelUnknown() {
        CityPolicy.configure(false, false, false, false, true, true);
        OfferLocationPolicy.Decision d = OfferLocationPolicy.evaluate(
                "Tulsa\nBroken Arrow\nOwasso\nMap\n$30.00\n8 miles");
        require(!d.identified,
                "bare map labels must remain unknown rather than causing a false location rejection");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
