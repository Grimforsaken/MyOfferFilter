package com.grimforsaken.sparkofferfilter;

import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class StoreLocationRegistry {
    private static final int STORE_LIST_VERSION = 1;

    private StoreLocationRegistry() {}

    static void ensureFreshDynamicList(SharedPreferences prefs) {
        if (prefs == null) return;
        int version = prefs.getInt(Prefs.STORE_LOCATION_LIST_VERSION, 0);
        if (version >= STORE_LIST_VERSION) return;

        prefs.edit()
                .remove(Prefs.DISCOVERED_STORE_RECORDS)
                .remove(Prefs.ACCEPTED_STORE_KEYS)
                .remove(Prefs.AUTO_ACCEPT_STORE_KEYS)
                .putInt(Prefs.STORE_LOCATION_LIST_VERSION, STORE_LIST_VERSION)
                .apply();
    }

    static boolean discoverFromText(SharedPreferences prefs, String text) {
        if (prefs == null || text == null || text.trim().isEmpty()) return false;
        List<StoreLocationDetector.Store> found = StoreLocationDetector.findStores(text);
        if (found.isEmpty()) return false;

        Map<String, String> current = recordMap(prefs);
        boolean changed = false;
        for (StoreLocationDetector.Store store : found) {
            String previous = current.get(store.key);
            if (previous == null || !previous.equals(store.label)) {
                current.put(store.key, store.label);
                changed = true;
            }
        }
        if (!changed) return false;

        Set<String> records = new HashSet<>();
        for (Map.Entry<String, String> entry : current.entrySet()) {
            records.add(entry.getKey() + "\t" + entry.getValue());
        }
        prefs.edit().putStringSet(Prefs.DISCOVERED_STORE_RECORDS, records).apply();
        return true;
    }

    static List<StoreEntry> stores(SharedPreferences prefs) {
        Map<String, String> map = recordMap(prefs);
        Set<String> accepted = acceptedKeys(prefs);
        Set<String> auto = autoAcceptKeys(prefs);
        List<StoreEntry> out = new ArrayList<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            out.add(new StoreEntry(entry.getKey(), entry.getValue(),
                    accepted.contains(entry.getKey()), auto.contains(entry.getKey())));
        }
        out.sort(Comparator.comparing((StoreEntry e) -> e.label, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    static Set<String> acceptedKeys(SharedPreferences prefs) {
        return copySet(prefs, Prefs.ACCEPTED_STORE_KEYS);
    }

    static Set<String> autoAcceptKeys(SharedPreferences prefs) {
        return copySet(prefs, Prefs.AUTO_ACCEPT_STORE_KEYS);
    }

    static void setAccepted(SharedPreferences prefs, String key, boolean value) {
        Set<String> accepted = acceptedKeys(prefs);
        Set<String> auto = autoAcceptKeys(prefs);
        if (value) accepted.add(key);
        else {
            accepted.remove(key);
            auto.remove(key);
        }
        prefs.edit()
                .putStringSet(Prefs.ACCEPTED_STORE_KEYS, accepted)
                .putStringSet(Prefs.AUTO_ACCEPT_STORE_KEYS, auto)
                .apply();
        configurePolicy(prefs, accepted, auto);
    }

    static void setAutoAccept(SharedPreferences prefs, String key, boolean value) {
        Set<String> accepted = acceptedKeys(prefs);
        Set<String> auto = autoAcceptKeys(prefs);
        if (value) {
            auto.add(key);
            accepted.add(key);
        } else {
            auto.remove(key);
        }
        prefs.edit()
                .putStringSet(Prefs.ACCEPTED_STORE_KEYS, accepted)
                .putStringSet(Prefs.AUTO_ACCEPT_STORE_KEYS, auto)
                .apply();
        configurePolicy(prefs, accepted, auto);
    }

    static void configurePolicy(SharedPreferences prefs) {
        configurePolicy(prefs, acceptedKeys(prefs), autoAcceptKeys(prefs));
    }

    private static void configurePolicy(SharedPreferences prefs, Set<String> accepted, Set<String> auto) {
        StoreSelectionPolicy.configure(accepted, auto);
    }

    private static Set<String> copySet(SharedPreferences prefs, String key) {
        if (prefs == null) return new HashSet<>();
        Set<String> source = prefs.getStringSet(key, Collections.emptySet());
        return source == null ? new HashSet<>() : new HashSet<>(source);
    }

    private static Map<String, String> recordMap(SharedPreferences prefs) {
        Map<String, String> out = new HashMap<>();
        if (prefs == null) return out;
        Set<String> raw = prefs.getStringSet(Prefs.DISCOVERED_STORE_RECORDS, Collections.emptySet());
        if (raw == null) return out;
        for (String record : raw) {
            if (record == null) continue;
            int split = record.indexOf('\t');
            if (split <= 0 || split >= record.length() - 1) continue;
            out.put(record.substring(0, split), record.substring(split + 1));
        }
        return out;
    }

    static final class StoreEntry {
        final String key;
        final String label;
        final boolean accepted;
        final boolean autoAccept;

        StoreEntry(String key, String label, boolean accepted, boolean autoAccept) {
            this.key = key;
            this.label = label;
            this.accepted = accepted;
            this.autoAccept = autoAccept;
        }
    }
}
