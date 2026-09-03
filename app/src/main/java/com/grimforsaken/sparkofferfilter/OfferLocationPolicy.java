package com.grimforsaken.sparkofferfilter;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class OfferLocationPolicy {
    private static final Pattern CITY_STATE_PATTERN = Pattern.compile(
            "(?im)^\\s*([A-Za-z][A-Za-z .'-]{1,40}?)\\s*,?\\s+(?:OK|Oklahoma)(?:\\s+\\d{5}(?:-\\d{4})?)?\\s*$");

    private OfferLocationPolicy() {}

    static Decision evaluate(String currentTreeText) {
        if (currentTreeText == null || currentTreeText.trim().isEmpty()) return Decision.unknown();

        Matcher addressMatcher = CITY_STATE_PATTERN.matcher(currentTreeText);
        Set<String> addressCities = new LinkedHashSet<>();
        while (addressMatcher.find()) {
            String city = CityPolicy.canonical(addressMatcher.group(1));
            if (!city.equals("Unknown")) addressCities.add(city);
        }

        if (addressCities.size() == 1) {
            String city = addressCities.iterator().next();
            return Decision.identified(city, CityPolicy.isAllowed(city));
        }
        if (addressCities.size() > 1) {
            return Decision.ambiguous("Multiple address cities are visible");
        }

        Set<String> namedMatches = new LinkedHashSet<>();
        String[] lines = currentTreeText.split("\\R");
        for (String line : lines) {
            String normalized = normalize(line);
            if (normalized.equals("TULSA")) namedMatches.add("Tulsa");
            else if (normalized.equals("GLENPOOL")) namedMatches.add("Glenpool");
            else if (normalized.equals("JENKS")) namedMatches.add("Jenks");
            else if (normalized.equals("SAPULPA")) namedMatches.add("Sapulpa");
            else if (normalized.equals("SAND SPRINGS")) namedMatches.add("Sand Springs");
            else if (normalized.equals("SAM'S CLUB") || normalized.equals("SAMS CLUB")
                    || normalized.startsWith("SAM'S CLUB ") || normalized.startsWith("SAMS CLUB ")) {
                namedMatches.add("Sam's Club");
            }
        }

        if (namedMatches.size() == 1) {
            String location = namedMatches.iterator().next();
            return Decision.identified(location, CityPolicy.isAllowed(location));
        }
        if (namedMatches.size() > 1) {
            return Decision.ambiguous("Multiple possible locations are visible");
        }

        return Decision.unknown();
    }

    private static String normalize(String text) {
        if (text == null) return "";
        return text.toUpperCase(Locale.US)
                .replace('’', '\'')
                .replaceAll("\\s+", " ")
                .trim();
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
