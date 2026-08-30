package com.grimforsaken.sparkofferfilter;

final class CityPolicy {
    private static volatile boolean allowTulsa = false;
    private static volatile boolean allowGlenpool = false;
    private static volatile boolean allowJenks = false;

    private CityPolicy() {}

    static void configure(boolean tulsa, boolean glenpool, boolean jenks) {
        allowTulsa = tulsa;
        allowGlenpool = glenpool;
        allowJenks = jenks;
    }

    static boolean isAllowed(String city) {
        if (city == null) return false;
        switch (city.toUpperCase(java.util.Locale.US)) {
            case "TULSA": return allowTulsa;
            case "GLENPOOL": return allowGlenpool;
            case "JENKS": return allowJenks;
            default: return false;
        }
    }
}
