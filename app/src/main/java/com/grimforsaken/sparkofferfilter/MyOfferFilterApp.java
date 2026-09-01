package com.grimforsaken.sparkofferfilter;

import android.app.Application;
import android.content.SharedPreferences;

public class MyOfferFilterApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences prefs = getSharedPreferences(Prefs.NAME, MODE_PRIVATE);

        // The installer-cleanup gate was removed in Safe Driver v1.8.2.
        // Keep the legacy flag complete so older MainActivity code can never redirect to it.
        if (!prefs.getBoolean(Prefs.INSTALLER_CLEANUP_COMPLETED, false)) {
            prefs.edit().putBoolean(Prefs.INSTALLER_CLEANUP_COMPLETED, true).apply();
        }

        CityPolicy.configure(
                prefs.getBoolean(Prefs.ALLOW_TULSA, false),
                prefs.getBoolean(Prefs.ALLOW_GLENPOOL, false),
                prefs.getBoolean(Prefs.ALLOW_JENKS, false));
    }
}
