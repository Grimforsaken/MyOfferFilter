package com.grimforsaken.sparkofferfilter;

final class CityPolicy {
    private static volatile boolean allowTulsa = false;
    private static volatile boolean allowGlenpool = false;
    private static volatile boolean allowJenks = false;
    private static volatile boolean allowSamsClub = false;

    private CityPolicy() {}

    static void configure(boolean tulsa, boolean glenpool, boolean jenks) {
        configure(tulsa, glenpool, jenks, false);
    }

    static void configure(boolean tulsa, boolean glenpool, boolean jenks, boolean samsClub) {
        allowTulsa = tulsa;
        allowGlenpool = glenpool;
        allowJenks = jenks;
        allowSamsClub = samsClub;
    }

    static boolean isAllowed(String location) {
        if (location == null) return false;
        String normalized = location.toUpperCase(java.util.Locale.US)
                .replace('’', '\'')
                .replaceAll("\\s+", " ")
                .trim();
        switch (normalized) {
            case "TULSA": return allowTulsa;
            case "GLENPOOL": return allowGlenpool;
            case "JENKS": return allowJenks;
            case "SAM'S CLUB":
            case "SAMS CLUB": return allowSamsClub;
            default: return false;
        }
    }
}
