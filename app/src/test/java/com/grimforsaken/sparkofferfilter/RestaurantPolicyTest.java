package com.grimforsaken.sparkofferfilter;

public final class RestaurantPolicyTest {
    public static void main(String[] args) {
        assertAction("Pickup\nRestaurant\nEstimated earnings $25.00", true, false, false,
                RestaurantPolicy.Action.REJECT);
        assertAction("Pickup\nRestaurant\nEstimated earnings $25.00", false, true, true,
                RestaurantPolicy.Action.ACCEPT);
        assertAction("Pickup\nRestaurant\nEstimated earnings $25.00", false, true, false,
                RestaurantPolicy.Action.NONE);
        assertAction("Pickup\nRestaurant\nEstimated earnings $25.00", true, true, true,
                RestaurantPolicy.Action.REJECT);
        assertAction("Pickup\nRestaurant\nEstimated earnings $25.00", false, false, true,
                RestaurantPolicy.Action.NONE);
        assertAction("Pickup\nRestaurant Mart\nEstimated earnings $25.00", true, false, true,
                RestaurantPolicy.Action.NONE);
        assertAction("123 Restaurant Road\nEstimated earnings $25.00", true, false, true,
                RestaurantPolicy.Action.NONE);
        assertAction("Pickup\nRESTAURANT DELIVERY\nEstimated earnings $25.00", true, false, true,
                RestaurantPolicy.Action.REJECT);
        System.out.println("RestaurantPolicyTest passed");
    }

    private static void assertAction(String input, boolean reject, boolean accept,
                                      boolean autoAcceptEnabled, RestaurantPolicy.Action expected) {
        RestaurantPolicy.Action actual = RestaurantPolicy.action(input, reject, accept, autoAcceptEnabled);
        if (actual != expected) throw new AssertionError("Expected " + expected + " but got " + actual);
    }
}
