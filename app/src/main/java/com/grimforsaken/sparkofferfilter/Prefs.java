package com.grimforsaken.sparkofferfilter;

final class Prefs {
    static final String NAME = "offer_filter";
    static final String MASTER_ENABLED = "master_enabled";
    static final String DRY_RUN = "dry_run";

    static final String REJECT_NO_SHOPPING = "reject_no_shopping";
    static final String REJECT_LOW_RATE = "reject_low_rate";
    static final String THRESHOLD = "threshold";

    static final String AUTO_ACCEPT_ENABLED = "auto_accept_enabled";
    static final String ACCEPT_MIN_PAY_ENABLED = "accept_min_pay_enabled";
    static final String ACCEPT_MIN_PAY = "accept_min_pay";
    static final String ACCEPT_MIN_RATE_ENABLED = "accept_min_rate_enabled";
    static final String ACCEPT_MIN_RATE = "accept_min_rate";
    static final String ACCEPT_MAX_MILES_ENABLED = "accept_max_miles_enabled";
    static final String ACCEPT_MAX_MILES = "accept_max_miles";
    static final String ACCEPT_SHOPPING_ENABLED = "accept_shopping_enabled";
    static final String ACCEPT_NO_SHIPPING_ENABLED = "accept_no_shipping_enabled";

    static final String DECISION_CHIMES = "decision_chimes";
    static final String LAST_DECISION = "last_decision";
    static final String LAST_SPARK_EVENT = "last_spark_event";
    static final String LAST_SCAN_STATUS = "last_scan_status";
    static final String LAST_CAPTURE = "last_capture";

    private Prefs() {}
}
