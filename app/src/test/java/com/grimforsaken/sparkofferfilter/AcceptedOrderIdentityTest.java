package com.grimforsaken.sparkofferfilter;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;

public final class AcceptedOrderIdentityTest {
    public static void main(String[] args) {
        long day1 = LocalDate.of(2026, 9, 20).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long day2 = LocalDate.of(2026, 9, 21).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();

        OrderRecord first = new OrderRecord(day1 + 60000L, 25.00, 5.0, 30, 3.00,
                "Sand Springs", "Walmart SAND SPRINGS #838", "8466");

        require(AcceptedOrderIdentity.isDuplicate(Arrays.asList(first), "8466", "838", day1 + 120000L),
                "same trip, store, and calendar day must be duplicate");
        require(!AcceptedOrderIdentity.isDuplicate(Arrays.asList(first), "8466", "838", day2 + 120000L),
                "same trip number on a later day must be allowed");
        require(!AcceptedOrderIdentity.isDuplicate(Arrays.asList(first), "8466", "992", day1 + 120000L),
                "same trip number at a different known store must be allowed");
        require(!AcceptedOrderIdentity.isDuplicate(Arrays.asList(first), "9001", "838", day1 + 120000L),
                "different trip must be allowed");

        System.out.println("Accepted order identity tests passed.");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
