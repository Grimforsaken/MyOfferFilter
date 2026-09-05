package com.grimforsaken.sparkofferfilter;

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
            "(?im)^\\s*(?:city|pickup city|pickup location city|store city|store location city|location city)\\s*[:\\-]\\s*([A-Za-z][A-Za-z .'-]{1,40}?)\\s*$");
    private static final Pattern SAMS_CLUB_PATTERN = Pattern.compile("(?i)\\bSAM(?:'|’)?S\\s+CLUB\\b");

    private OfferLocationPolicy() {}

    static Decision evaluate(String currentTreeText) {
        if (currentTreeText == null || currentTreeText.trim().isEmpty()) return Decision.unknown();

        // Sam's Club has its own explicit checkbox. If Spark identifies the store as
        // Sam's Club, that checkbox takes priority over the city address beneath it.
        // This prevents a checked city from accidentally allowing an unchecked Sam's Club.
        if (SAMS_CLUB_PATTERN.matcher(currentTreeText).find()) {
            return Decision.identified("Sam's Club", CityPolicy.isAllowed("Sam's Club"));
        }

        // Explicit pickup/store-city labels are the most reliable city signal.
        String labeled = firstMatchCity(LABELED_CITY_PATTERN, currentTreeText);
        if (labeled != null) {
            return Decision.identified(labeled, CityPolicy.isAllowed(labeled));
        }

        // Spark can expose both pickup and delivery addresses in one Accessibility tree.
        // The pickup/store address is presented first. Use the earliest reliable Oklahoma
        // address rather than treating every multi-address offer as Unknown and waiting.
        String firstAddressCity = earliestAddressCity(currentTreeText);
        if (firstAddressCity != null) {
            return Decision.identified(firstAddressCity, CityPolicy.isAllowed(firstAddressCity));
        }

        // Deliberately do not use bare map labels or zone headers such as "Tulsa".
        // They are not reliable enough to identify the actual order location.
        return Decision.unknown();
    }

    private static String firstMatchCity(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) return null;
        String city = CityPolicy.canonical(matcher.group(1));
        return "Unknown".equals(city) ? null : city;
    }

    private static String earliestAddressCity(String text) {
        Match best = null;
        best = earlier(best, firstMatch(CITY_STATE_LINE_PATTERN, text));
        best = earlier(best, firstMatch(SPLIT_CITY_STATE_PATTERN, text));
        best = earlier(best, firstMatch(INLINE_ADDRESS_PATTERN, text));
        return best == null ? null : best.city;
    }

    private static Match firstMatch(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) return null;
        String city = CityPolicy.canonical(matcher.group(1));
        if ("Unknown".equals(city)) return null;
        return new Match(matcher.start(), city);
    }

    private static Match earlier(Match a, Match b) {
        if (a == null) return b;
        if (b == null) return a;
        return b.start < a.start ? b : a;
    }

    private static final class Match {
        final int start;
        final String city;

        Match(int start, String city) {
            this.start = start;
            this.city = city;
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
