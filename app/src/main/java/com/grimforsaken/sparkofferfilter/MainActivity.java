package com.grimforsaken.sparkofferfilter;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private SharedPreferences prefs;
    private TextView serviceStatus;
    private TextView latestDecision;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prefs = getSharedPreferences(Prefs.NAME, MODE_PRIVATE);

        CheckBox masterEnabled = findViewById(R.id.masterEnabled);
        CheckBox dryRun = findViewById(R.id.dryRun);
        CheckBox decisionChimes = findViewById(R.id.decisionChimes);
        CheckBox rejectNoShopping = findViewById(R.id.rejectNoShopping);
        CheckBox rejectLowRate = findViewById(R.id.rejectLowRate);
        EditText rejectThreshold = findViewById(R.id.rejectThreshold);

        CheckBox autoAcceptEnabled = findViewById(R.id.autoAcceptEnabled);
        CheckBox acceptMinPayEnabled = findViewById(R.id.acceptMinPayEnabled);
        EditText acceptMinPay = findViewById(R.id.acceptMinPay);
        CheckBox acceptMinRateEnabled = findViewById(R.id.acceptMinRateEnabled);
        EditText acceptMinRate = findViewById(R.id.acceptMinRate);
        CheckBox acceptMaxMilesEnabled = findViewById(R.id.acceptMaxMilesEnabled);
        EditText acceptMaxMiles = findViewById(R.id.acceptMaxMiles);

        serviceStatus = findViewById(R.id.serviceStatus);
        latestDecision = findViewById(R.id.latestDecision);
        Button openAccessibility = findViewById(R.id.openAccessibility);

        masterEnabled.setChecked(prefs.getBoolean(Prefs.MASTER_ENABLED, false));
        dryRun.setChecked(prefs.getBoolean(Prefs.DRY_RUN, true));
        decisionChimes.setChecked(prefs.getBoolean(Prefs.DECISION_CHIMES, true));
        rejectNoShopping.setChecked(prefs.getBoolean(Prefs.REJECT_NO_SHOPPING, false));
        rejectLowRate.setChecked(prefs.getBoolean(Prefs.REJECT_LOW_RATE, true));
        rejectThreshold.setText(format(prefs.getFloat(Prefs.THRESHOLD, 1.25f), 2));

        autoAcceptEnabled.setChecked(prefs.getBoolean(Prefs.AUTO_ACCEPT_ENABLED, false));
        acceptMinPayEnabled.setChecked(prefs.getBoolean(Prefs.ACCEPT_MIN_PAY_ENABLED, false));
        acceptMinPay.setText(format(prefs.getFloat(Prefs.ACCEPT_MIN_PAY, 20.00f), 2));
        acceptMinRateEnabled.setChecked(prefs.getBoolean(Prefs.ACCEPT_MIN_RATE_ENABLED, false));
        acceptMinRate.setText(format(prefs.getFloat(Prefs.ACCEPT_MIN_RATE, 1.25f), 2));
        acceptMaxMilesEnabled.setChecked(prefs.getBoolean(Prefs.ACCEPT_MAX_MILES_ENABLED, false));
        acceptMaxMiles.setText(format(prefs.getFloat(Prefs.ACCEPT_MAX_MILES, 10.0f), 1));

        bindCheck(masterEnabled, Prefs.MASTER_ENABLED);
        bindCheck(dryRun, Prefs.DRY_RUN);
        bindCheck(decisionChimes, Prefs.DECISION_CHIMES);
        bindCheck(rejectNoShopping, Prefs.REJECT_NO_SHOPPING);
        bindCheck(rejectLowRate, Prefs.REJECT_LOW_RATE);
        bindNumber(rejectThreshold, Prefs.THRESHOLD, 0.10f, 20.0f);

        bindCheck(autoAcceptEnabled, Prefs.AUTO_ACCEPT_ENABLED);
        bindCheck(acceptMinPayEnabled, Prefs.ACCEPT_MIN_PAY_ENABLED);
        bindNumber(acceptMinPay, Prefs.ACCEPT_MIN_PAY, 0.01f, 10000.0f);
        bindCheck(acceptMinRateEnabled, Prefs.ACCEPT_MIN_RATE_ENABLED);
        bindNumber(acceptMinRate, Prefs.ACCEPT_MIN_RATE, 0.01f, 100.0f);
        bindCheck(acceptMaxMilesEnabled, Prefs.ACCEPT_MAX_MILES_ENABLED);
        bindNumber(acceptMaxMiles, Prefs.ACCEPT_MAX_MILES, 0.1f, 1000.0f);

        openAccessibility.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
    }

    private void bindCheck(CheckBox checkBox, String key) {
        checkBox.setOnCheckedChangeListener((button, checked) ->
                prefs.edit().putBoolean(key, checked).apply());
    }

    private void bindNumber(EditText editText, String key, float min, float max) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                try {
                    float value = Float.parseFloat(s.toString());
                    if (value >= min && value <= max) {
                        prefs.edit().putFloat(key, value).apply();
                    }
                } catch (NumberFormatException ignored) {}
            }
        });
    }

    private String format(float value, int decimals) {
        return String.format(Locale.US, decimals == 1 ? "%.1f" : "%.2f", (double) value);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private void refreshStatus() {
        boolean enabled = isAccessibilityServiceEnabled();
        serviceStatus.setText(enabled
                ? "Accessibility service status: ON"
                : "Accessibility service status: OFF — open settings and enable My Offer Filter service");

        latestDecision.setText(prefs.getString(Prefs.LAST_DECISION, "No offer evaluated yet."));
    }

    private boolean isAccessibilityServiceEnabled() {
        AccessibilityManager manager = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (manager == null) return false;
        List<AccessibilityServiceInfo> enabled = manager.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        String expected = getPackageName() + "/" + SparkOfferAccessibilityService.class.getName();
        for (AccessibilityServiceInfo info : enabled) {
            if (info.getId() != null && info.getId().equalsIgnoreCase(expected)) return true;
        }
        return false;
    }
}
