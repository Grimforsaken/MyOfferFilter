package com.grimforsaken.sparkofferfilter;

import android.app.Application;
import android.content.SharedPreferences;

public class MyOfferFilterApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences prefs = getSharedPreferences(Prefs.NAME, MODE_PRIVATE);
        CityPolicy.configure(
                prefs.getBoolean(Prefs.ALLOW_TULSA, false),
                prefs.getBoolean(Prefs.ALLOW_GLENPOOL, false),
                prefs.getBoolean(Prefs.ALLOW_JENKS, false));
    }
}
