package com.grimforsaken.sparkofferfilter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class StoreLocationDetector {
    private static final Pattern STORE_PATTERN = Pattern.compile(
            "(?i)\\b(WALMART|SAM(?:'|’)?S\\s+CLUB)"
          + "(?:\\s+([A-Z][A-Z .'-]{0,45}?))?"
          + "\\s*#\\s*(\\d{2,6})\\b");

    private StoreLocationDetector() {}

    static List<Store> findStores(String text) {
        List<Store> out = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) return out;

        String flattened = text.replace('\u00A0', ' ')
                .replace('’', '\'')
                .replaceAll("\\s+", " ")
                .trim();

        Map<String, Store> unique = new LinkedHashMap<>();
        Matcher matcher = STORE_PATTERN.matcher(flattened);
        while (matcher.find()) {
            String retailer = canonicalRetailer(matcher.group(1));
            String city = canonicalCity(matcher.group(2));
            String number = matcher.group(3);
            String key = storeKey(retailer, number);
            String label = retailer
                    + (city.isEmpty() ? "" : " " + city)
                    + " #" + number;
            Store store = new Store(key, label, retailer, city, number, matcher.start());
            Store old = unique.get(key);
            if (old == null || old.city.isEmpty() && !city.isEmpty()) unique.put(key, store);
        }
        out.addAll(unique.values());
        return out;
    }

    static Store firstStore(String text) {
        List<Store> stores = findStores(text);
        if (stores.isEmpty()) return null;
        Store best = stores.get(0);
        for (Store store : stores) {
            if (store.start < best.start) best = store;
        }
        return best;
    }

    static String storeKey(String retailer, String number) {
        String r = retailer == null ? "" : retailer.toUpperCase(Locale.US)
                .replace("'", "")
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        String n = number == null ? "" : number.replaceAll("[^0-9]", "");
        return r + "#" + n;
    }

    private static String canonicalRetailer(String raw) {
        if (raw == null) return "Store";
        String n = raw.toUpperCase(Locale.US).replace('’', '\'').replaceAll("\\s+", " ").trim();
        if (n.startsWith("SAM")) return "Sam's Club";
        return "Walmart";
    }

    private static String canonicalCity(String raw) {
        if (raw == null) return "";
        String n = raw.trim().replaceAll("\\s+", " ").toLowerCase(Locale.US);
        if (n.isEmpty()) return "";
        StringBuilder out = new StringBuilder();
        boolean cap = true;
        for (int i = 0; i < n.length(); i++) {
            char ch = n.charAt(i);
            if (cap && Character.isLetter(ch)) {
                out.append(Character.toUpperCase(ch));
                cap = false;
            } else {
                out.append(ch);
            }
            if (ch == ' ' || ch == '-' || ch == '\'') cap = true;
        }
        return out.toString();
    }

    static final class Store {
        final String key;
        final String label;
        final String retailer;
        final String city;
        final String storeNumber;
        final int start;

        Store(String key, String label, String retailer, String city, String storeNumber, int start) {
            this.key = key;
            this.label = label;
            this.retailer = retailer;
            this.city = city;
            this.storeNumber = storeNumber;
            this.start = start;
        }
    }
}
