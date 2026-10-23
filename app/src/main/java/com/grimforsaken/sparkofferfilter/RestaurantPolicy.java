package com.grimforsaken.sparkofferfilter;

import java.util.regex.Pattern;

/**
 * Restaurant is an explicit offer tag, not a substring of a business name.
 * Do not treat "Joe's Restaurant" or an address containing Restaurant as tags.
 */
final class RestaurantPolicy {
    private static final Pattern TAG = Pattern.compile(
            "(?im)^[ \\t]*(?:[•·]\\s*)?restaurant(?:[ \\t]+(?:order|delivery))?[ \\t]*$");

    enum Action { NONE, ACCEPT, REJECT }

    private RestaurantPolicy() {}

    static boolean hasTag(String visibleText) {
        return visibleText != null && TAG.matcher(visibleText).find();
    }

    static Action action(String visibleText, boolean autoRejectRestaurant,
                         boolean autoAcceptRestaurant, boolean autoAcceptEnabled) {
        if (!hasTag(visibleText)) return Action.NONE;
        // Reject wins for corrupted or migrated settings where both may be ON.
        if (autoRejectRestaurant) return Action.REJECT;
        if (autoAcceptRestaurant && autoAcceptEnabled) return Action.ACCEPT;
        return Action.NONE;
    }
}
