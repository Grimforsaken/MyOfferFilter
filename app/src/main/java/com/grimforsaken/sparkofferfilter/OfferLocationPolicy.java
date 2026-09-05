package com.grimforsaken.sparkofferfilter;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class OfferLocationPolicy {
    private static final Pattern CITY_STATE_LINE_PATTERN = Pattern.compile(
            "(?im)^\\s*([A-Za-z][A-Za-z .'-]{1,40}?)\\s*,?\\s+(?:OK|Oklahoma)\\b(?:\\s*,?\\s*\\d{5}(?:-\\d{4})?)?\\s*$");
    private static final Pattern SPLIT_CITY_STATE_PATTERN = Pattern.compile(
            "(?im)^\\s*([A-Za-z][A-Za-z .'-]{1,40}?)\\s*,?\\s*$\\R\\s*(?:OK|Oklahoma)\\b(?:\\s*,?\\s*\\d{5}(?:-\\d{4})?)?\\s*$");
    private static final Pattern INLINE_ADDRESS_PATTERN = Pattern.compile(
            "(?im)^.*?,\\s*([A-Za-z][A-Za-z .'-]{1,40}?)\\s*,\\s*(?:OK|Oklahoma)\\b(?:\\s*,?\\s*\\d{5}(?:-\\d{4})?)?\\s*$");
    private static final Pattern LABELED_CITY_PATTERN = Pattern.compile(
            "(?im)^\\s*(?:city|pickup city|store city|location city)\\s*[:\\-]\\s*([A-Za-z][A-Za-z .'-]{1,40}?)\\s*$");
    private static final Pattern SAMS_CLUB_PATTERN = Pattern.compile("(?i)\\bSAM(?:'|’)?S\\s+CLUB\\b");

    private OfferLocationPolicy() {}

    static Decision evaluate(String currentTreeText) {
        if (currentTreeText == null || currentTreeText.trim().isEmpty()) return Decision.unknown();

        Set<String> addressCities = new LinkedHashSet<>();
        addCities(addressCities, CITY_STATE_LINE_PATTERN, currentTreeText);
        addCities(addressCities, SPLIT_CITY_STATE_PATTERN, currentTreeText);
        addCities(addressCities, INLINE_ADDRESS_PATTERN, currentTreeText);
        addCities(addressCities, LABELED_CITY_PATTERN, currentTreeText);

        if (addressCities.size() == 1) {
            String city = addressCities.iterator().next();
            return Decision.identified(city, CityPolicy.isAllowed(city));
        }
        if (addressCities.size() > 1) {
            return Decision.ambiguous("Multiple reliable address cities are visible");
        }

        // Sam's Club is a store/location type rather than a city. Its store name is
        // reliable enough to apply the user's explicit Sam's Club whitelist option.
        if (SAMS_CLUB_PATTERN.matcher(currentTreeText).find()) {
            return Decision.identified("Sam's Club", CityPolicy.isAllowed("Sam's Club"));
        }

        // Deliberately do not use bare map labels or zone headers such as "Tulsa".
        // They are not reliable enough to identify the actual order location.
        return Decision.unknown();
    }

    private static void addCities(Set<String> cities, Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String city = CityPolicy.canonical(matcher.group(1));
            if (!city.equals("Unknown")) cities.add(city);
        }
    }

    static final class Decision {
        final boolean identified;
        final boolean allowed;
        final String location;
        final String reason;

        private Decision(boolean identified, boolean allowed, String location, String reason) {
            this.identified = identified;
            this.allowed = allowed;
            this.location = location;
            this.reason = reason;
        }

        static Decision identified(String location, boolean allowed) {
            return new Decision(true, allowed, location,
                    allowed
                            ? location + " is checked in Accepted Locations"
                            : location + " is not checked in Accepted Locations");
        }

        static Decision unknown() {
            return new Decision(false, false, "Unknown", "Waiting for a reliable order location.");
        }

        static Decision ambiguous(String reason) {
            return new Decision(false, false, "Unknown", reason + "; waiting for a reliable order location.");
        }
    }
}
