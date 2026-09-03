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

        CityPolicy.configure(
                prefs.getBoolean(Prefs.ALLOW_TULSA, false),
                prefs.getBoolean(Prefs.ALLOW_GLENPOOL, false),
                prefs.getBoolean(Prefs.ALLOW_JENKS, false),
                prefs.getBoolean(Prefs.ALLOW_SAMS_CLUB, false),
                prefs.getBoolean(Prefs.ALLOW_SAPULPA, true),
                prefs.getBoolean(Prefs.ALLOW_SAND_SPRINGS, true));
    }
}
