package com.grimforsaken.sparkofferfilter;

import java.util.HashSet;
import java.util.Set;

public final class StoreSelectionPolicyTest {
    public static void main(String[] args) {
        Set<String> accepted = new HashSet<>();
        accepted.add("WALMART#838");
        Set<String> auto = new HashSet<>();
        auto.add("WALMART#838");
        StoreSelectionPolicy.configure(accepted, auto);

        require(StoreSelectionPolicy.isAccepted("WALMART#838"), "checked store must be accepted");
        require(!StoreSelectionPolicy.isAccepted("WALMART#5093"), "new unchecked store must reject");
        require(StoreSelectionPolicy.isAutoAcceptAllowed("WALMART#838"), "separate auto-accept choice must pass");
        require(!StoreSelectionPolicy.isAutoAcceptAllowed("WALMART#5093"), "unchecked store must not auto-accept");

        StoreSelectionPolicy.configure(new HashSet<>(), new HashSet<>());
        require(!StoreSelectionPolicy.isAccepted("WALMART#838"),
                "fresh dynamic store list must begin with no accepted stores");
        System.out.println("Store selection policy tests passed.");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
