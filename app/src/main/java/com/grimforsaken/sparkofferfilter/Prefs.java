package com.grimforsaken.sparkofferfilter;

final class Prefs {
    static final String NAME = "offer_filter";
    static final String MASTER_ENABLED = "master_enabled";
    static final String DRY_RUN = "dry_run";

    static final String INSTALLER_CLEANUP_COMPLETED = "installer_cleanup_completed";
    static final String INSTALLER_CLEANUP_VERSION = "installer_cleanup_version";
    static final String INSTALLER_URI = "installer_uri";
    static final String APP_LANGUAGE = "app_language";

    static final String REJECT_NO_SHOPPING = "reject_no_shopping";
    static final String REJECT_LOW_RATE = "reject_low_rate";
    static final String THRESHOLD = "threshold";
    static final String REJECT_MIN_PAY_ENABLED = "reject_min_pay_enabled";
    static final String REJECT_MIN_PAY = "reject_min_pay";
    static final String REJECT_MAX_MILES_ENABLED = "reject_max_miles_enabled";
    static final String REJECT_MAX_MILES = "reject_max_miles";

    static final String ALLOW_TULSA = "allow_tulsa";
    static final String ALLOW_GLENPOOL = "allow_glenpool";
    static final String ALLOW_JENKS = "allow_jenks";
    static final String ALLOW_SAMS_CLUB = "allow_sams_club";
    static final String ALLOW_SAPULPA = "allow_sapulpa";
    static final String ALLOW_SAND_SPRINGS = "allow_sand_springs";

    static final String AUTO_ACCEPT_ENABLED = "auto_accept_enabled";
    static final String ACCEPT_MIN_PAY_ENABLED = "accept_min_pay_enabled";
    static final String ACCEPT_MIN_PAY = "accept_min_pay";
    static final String ACCEPT_MIN_RATE_ENABLED = "accept_min_rate_enabled";
    static final String ACCEPT_MIN_RATE = "accept_min_rate";
    static final String ACCEPT_MAX_MILES_ENABLED = "accept_max_miles_enabled";
    static final String ACCEPT_MAX_MILES = "accept_max_miles";
    static final String ACCEPT_SHOPPING_ENABLED = "accept_shopping_enabled";
    static final String ACCEPT_NO_SHOPPING_ENABLED = "accept_no_shopping_enabled";

    static final String ACCEPT_LOCATION_TULSA = "accept_location_tulsa";
    static final String ACCEPT_LOCATION_GLENPOOL = "accept_location_glenpool";
    static final String ACCEPT_LOCATION_JENKS = "accept_location_jenks";
    static final String ACCEPT_LOCATION_SAMS_CLUB = "accept_location_sams_club";
    static final String ACCEPT_LOCATION_SAPULPA = "accept_location_sapulpa";
    static final String ACCEPT_LOCATION_SAND_SPRINGS = "accept_location_sand_springs";

    static final String LEGACY_ACCEPT_NO_SHIPPING_ENABLED = "accept_no_shipping_enabled";
    static final String ACCEPT_NO_SHIPPING_ENABLED = ACCEPT_NO_SHOPPING_ENABLED;

    static final String DECISION_CHIMES = "decision_chimes";
    static final String LAST_DECISION = "last_decision";
    static final String LAST_SPARK_EVENT = "last_spark_event";
    static final String LAST_SCAN_STATUS = "last_scan_status";
    static final String LAST_CAPTURE = "last_capture";

    static final String HISTORY_REJECTED = "history_rejected";
    static final String HISTORY_ACCEPTED = "history_accepted";

    private Prefs() {}
}
