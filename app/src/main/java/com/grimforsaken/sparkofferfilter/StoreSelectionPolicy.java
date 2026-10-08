package com.grimforsaken.sparkofferfilter;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

final class StoreSelectionPolicy {
    private static volatile Set<String> accepted = Collections.emptySet();
    private static volatile Set<String> autoAccept = Collections.emptySet();

    private StoreSelectionPolicy() {}

    static void configure(Set<String> acceptedKeys, Set<String> autoAcceptKeys) {
        accepted = immutableCopy(acceptedKeys);
        autoAccept = immutableCopy(autoAcceptKeys);
    }

    static boolean isAccepted(String storeKey) {
        return storeKey != null && accepted.contains(storeKey);
    }

    static boolean isAutoAcceptAllowed(String storeKey) {
        return storeKey != null && autoAccept.contains(storeKey);
    }

    private static Set<String> immutableCopy(Set<String> source) {
        if (source == null || source.isEmpty()) return Collections.emptySet();
        return Collections.unmodifiableSet(new HashSet<>(source));
    }
}
