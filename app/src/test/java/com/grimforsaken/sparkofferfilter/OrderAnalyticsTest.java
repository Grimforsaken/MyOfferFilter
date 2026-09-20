package com.grimforsaken.sparkofferfilter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class OrderAnalyticsTest {
    public static void main(String[] args) {
        OrderRecord a = new OrderRecord(1L, 20.00, 10.0, 30, 3.50, "Sapulpa", "Walmart SAPULPA #1", "1");
        OrderRecord b = new OrderRecord(2L, 30.00, 20.0, 90, 3.50, "Sand Springs", "Walmart SAND SPRINGS #2", "2");
        OrderAnalytics.Summary s = OrderAnalytics.summarize(Arrays.asList(a, b));
        require(s.count == 2, "count");
        require(close(s.totalPay, 50.0), "pay");
        require(close(s.totalMiles, 30.0), "miles");
        require(s.totalMinutes == 120, "minutes");
        require(close(s.dollarsPerMile, 50.0 / 30.0), "weighted dollars per mile");
        require(close(s.dollarsPerHour, 25.0), "weighted dollars per hour");
        require(close(s.fuelCost, 3.0), "fuel cost at 35 MPG");
        require(close(s.afterFuel, 47.0), "after fuel");
        require(close(s.averageGasPrice, 3.50), "average gas price");

        Set<String> delete = new HashSet<>();
        delete.add(a.stableKey());
        List<OrderRecord> kept = OrderAnalytics.withoutKeys(Arrays.asList(a, b), delete);
        require(kept.size() == 1, "selected order must be removed");
        require(kept.get(0).stableKey().equals(b.stableKey()), "unselected order must remain");
        OrderAnalytics.Summary afterDelete = OrderAnalytics.summarize(kept);
        require(afterDelete.count == 1, "summary must recalculate after deletion");
        require(close(afterDelete.totalPay, 30.0), "remaining pay after deletion");

        System.out.println("Order analytics tests passed.");
    }

    private static boolean close(double a, double b) { return Math.abs(a - b) < 0.0001; }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
