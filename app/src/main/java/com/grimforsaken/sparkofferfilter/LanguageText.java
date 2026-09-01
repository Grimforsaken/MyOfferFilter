package com.grimforsaken.sparkofferfilter;

import android.content.SharedPreferences;

import java.util.Locale;

final class LanguageText {
    static final String ENGLISH = "en";
    static final String SPANISH = "es";

    private LanguageText() {}

    static void ensureDefault(SharedPreferences prefs) {
        if (prefs.contains(Prefs.APP_LANGUAGE)) return;
        String language = Locale.getDefault().getLanguage();
        prefs.edit().putString(Prefs.APP_LANGUAGE,
                "es".equalsIgnoreCase(language) ? SPANISH : ENGLISH).apply();
    }

    static boolean isSpanish(SharedPreferences prefs) {
        return SPANISH.equals(prefs.getString(Prefs.APP_LANGUAGE, ENGLISH));
    }

    static void setSpanish(SharedPreferences prefs, boolean spanish) {
        prefs.edit().putString(Prefs.APP_LANGUAGE, spanish ? SPANISH : ENGLISH).apply();
    }
}
