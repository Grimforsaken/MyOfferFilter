package com.grimforsaken.sparkofferfilter;

import java.util.Locale;

final class AutoAcceptCityPolicy {
    private static volatile boolean allowTulsa = false;
    private static volatile boolean allowGlenpool = false;
    private static volatile boolean allowJenks = false;
    private static volatile boolean allowSamsClub = false;
    private static volatile boolean allowSapulpa = true;
    private static volatile boolean allowSandSprings = true;

    private AutoAcceptCityPolicy() {}

    static void configure(boolean tulsa, boolean glenpool, boolean jenks, boolean samsClub,
                          boolean sapulpa, boolean sandSprings) {
        allowTulsa = tulsa;
        allowGlenpool = glenpool;
        allowJenks = jenks;
        allowSamsClub = samsClub;
        allowSapulpa = sapulpa;
        allowSandSprings = sandSprings;
    }

    static boolean isAllowed(String location) {
        String normalized = normalize(location);
        switch (normalized) {
            case "TULSA": return allowTulsa;
            case "GLENPOOL": return allowGlenpool;
            case "JENKS": return allowJenks;
            case "SAM'S CLUB":
            case "SAMS CLUB": return allowSamsClub;
            case "SAPULPA": return allowSapulpa;
            case "SAND SPRINGS": return allowSandSprings;
            default: return false;
        }
    }

    private static String normalize(String location) {
        if (location == null) return "";
        return location.toUpperCase(Locale.US)
                .replace('’', '\'')
                .replaceAll("\\s+", " ")
                .trim();
    }
}
