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

        // Sand Springs and Sapulpa are the default checked locations in both lists.
        // Only seed these values when the user has never made a choice, so an
        // intentional later uncheck is preserved across upgrades.
        SharedPreferences.Editor defaults = null;
        if (!prefs.contains(Prefs.ALLOW_SAND_SPRINGS)) {
            if (defaults == null) defaults = prefs.edit();
            defaults.putBoolean(Prefs.ALLOW_SAND_SPRINGS, true);
        }
        if (!prefs.contains(Prefs.ALLOW_SAPULPA)) {
            if (defaults == null) defaults = prefs.edit();
            defaults.putBoolean(Prefs.ALLOW_SAPULPA, true);
        }
        if (!prefs.contains(Prefs.ACCEPT_LOCATION_SAND_SPRINGS)) {
            if (defaults == null) defaults = prefs.edit();
            defaults.putBoolean(Prefs.ACCEPT_LOCATION_SAND_SPRINGS, true);
        }
        if (!prefs.contains(Prefs.ACCEPT_LOCATION_SAPULPA)) {
            if (defaults == null) defaults = prefs.edit();
            defaults.putBoolean(Prefs.ACCEPT_LOCATION_SAPULPA, true);
        }
        if (defaults != null) defaults.apply();

        CityPolicy.configure(
                prefs.getBoolean(Prefs.ALLOW_TULSA, false),
                prefs.getBoolean(Prefs.ALLOW_GLENPOOL, false),
                prefs.getBoolean(Prefs.ALLOW_JENKS, false),
                prefs.getBoolean(Prefs.ALLOW_SAMS_CLUB, false),
                prefs.getBoolean(Prefs.ALLOW_SAPULPA, true),
                prefs.getBoolean(Prefs.ALLOW_SAND_SPRINGS, true));

        AutoAcceptCityPolicy.configure(
                prefs.getBoolean(Prefs.ACCEPT_LOCATION_TULSA, false),
                prefs.getBoolean(Prefs.ACCEPT_LOCATION_GLENPOOL, false),
                prefs.getBoolean(Prefs.ACCEPT_LOCATION_JENKS, false),
                prefs.getBoolean(Prefs.ACCEPT_LOCATION_SAMS_CLUB, false),
                prefs.getBoolean(Prefs.ACCEPT_LOCATION_SAPULPA, true),
                prefs.getBoolean(Prefs.ACCEPT_LOCATION_SAND_SPRINGS, true));

        DropoffPolicy.configure(prefs.getBoolean(Prefs.REJECT_3_PLUS_DROPOFFS, false));
    }
}
