package com.grimforsaken.sparkofferfilter;

import android.app.Application;
import android.content.SharedPreferences;

public class MyOfferFilterApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences prefs = getSharedPreferences(Prefs.NAME, MODE_PRIVATE);

        // The installer-cleanup gate was removed. Keep the legacy flag complete so
        // upgraded installations can never be redirected to the old setup window.
        if (!prefs.getBoolean(Prefs.INSTALLER_CLEANUP_COMPLETED, false)) {
            prefs.edit().putBoolean(Prefs.INSTALLER_CLEANUP_COMPLETED, true).apply();
        }

        // Dynamic store locations replace the old fixed city checklists. The first
        // version of this system intentionally starts empty; stores are discovered
        // from Spark screens and remain unchecked until the user chooses them.
        StoreLocationRegistry.ensureFreshDynamicList(prefs);
        StoreLocationRegistry.configurePolicy(prefs);

        DropoffPolicy.configure(prefs.getBoolean(Prefs.REJECT_3_PLUS_DROPOFFS, false));
        TripDurationPolicy.configure(
                prefs.getBoolean(Prefs.REJECT_MAX_DURATION_ENABLED, false),
                prefs.getInt(Prefs.REJECT_MAX_DURATION_HOURS, 1),
                prefs.getInt(Prefs.REJECT_MAX_DURATION_MINUTES, 0));
        HourlyRatePolicy.configure(
                prefs.getBoolean(Prefs.REJECT_MIN_HOURLY_ENABLED, false),
                prefs.getFloat(Prefs.REJECT_MIN_HOURLY, 20.00f));
    }
}
