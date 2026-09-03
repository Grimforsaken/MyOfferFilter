package com.grimforsaken.sparkofferfilter;

import java.util.Locale;

final class CityPolicy {
    private static volatile boolean allowTulsa = false;
    private static volatile boolean allowGlenpool = false;
    private static volatile boolean allowJenks = false;
    private static volatile boolean allowSamsClub = false;
    private static volatile boolean allowSapulpa = true;
    private static volatile boolean allowSandSprings = true;

    private CityPolicy() {}

    static void configure(boolean tulsa, boolean glenpool, boolean jenks) {
        configure(tulsa, glenpool, jenks, false, true, true);
    }

    static void configure(boolean tulsa, boolean glenpool, boolean jenks, boolean samsClub) {
        configure(tulsa, glenpool, jenks, samsClub, true, true);
    }

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

    static boolean isNamedWhitelistLocation(String location) {
        String normalized = normalize(location);
        return normalized.equals("TULSA")
                || normalized.equals("GLENPOOL")
                || normalized.equals("JENKS")
                || normalized.equals("SAM'S CLUB")
                || normalized.equals("SAMS CLUB")
                || normalized.equals("SAPULPA")
                || normalized.equals("SAND SPRINGS");
    }

    static String canonical(String location) {
        String normalized = normalize(location);
        if (normalized.equals("SAM'S CLUB") || normalized.equals("SAMS CLUB")) return "Sam's Club";
        if (normalized.equals("SAND SPRINGS")) return "Sand Springs";
        if (normalized.equals("SAPULPA")) return "Sapulpa";
        if (normalized.equals("TULSA")) return "Tulsa";
        if (normalized.equals("GLENPOOL")) return "Glenpool";
        if (normalized.equals("JENKS")) return "Jenks";
        return titleCase(location);
    }

    private static String normalize(String location) {
        if (location == null) return "";
        return location.toUpperCase(Locale.US)
                .replace('’', '\'')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String titleCase(String raw) {
        if (raw == null) return "Unknown";
        String normalized = raw.trim().replaceAll("\\s+", " ").toLowerCase(Locale.US);
        if (normalized.isEmpty()) return "Unknown";
        StringBuilder out = new StringBuilder(normalized.length());
        boolean capitalize = true;
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (capitalize && Character.isLetter(c)) {
                out.append(Character.toUpperCase(c));
                capitalize = false;
            } else {
                out.append(c);
            }
            if (c == ' ' || c == '-' || c == '\'') capitalize = true;
        }
        return out.toString();
    }
}
