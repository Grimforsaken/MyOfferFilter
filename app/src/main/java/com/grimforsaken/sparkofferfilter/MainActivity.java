package com.grimforsaken.sparkofferfilter;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
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
    private TextView diagnostics;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final Runnable refreshRunnable = new Runnable() {
        @Override public void run() {
            refreshStatus();
            uiHandler.postDelayed(this, 750L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prefs = getSharedPreferences(Prefs.NAME, MODE_PRIVATE);

        CheckBox masterEnabled = findViewById(R.id.masterEnabled);
        CheckBox dryRun = findViewById(R.id.dryRun);
        CheckBox decisionChimes = findViewById(R.id.decisionChimes);
        CheckBox rejectNoShopping = findViewById(R.id.rejectNoShopping);
        CheckBox rejectMinPayEnabled = findViewById(R.id.rejectMinPayEnabled);
        EditText rejectMinPay = findViewById(R.id.rejectMinPay);
        CheckBox rejectLowRate = findViewById(R.id.rejectLowRate);
        EditText rejectThreshold = findViewById(R.id.rejectThreshold);
        CheckBox allowTulsa = findViewById(R.id.allowTulsa);
        CheckBox allowGlenpool = findViewById(R.id.allowGlenpool);
        CheckBox allowJenks = findViewById(R.id.allowJenks);

        CheckBox autoAcceptEnabled = findViewById(R.id.autoAcceptEnabled);
        CheckBox acceptMinPayEnabled = findViewById(R.id.acceptMinPayEnabled);
        EditText acceptMinPay = findViewById(R.id.acceptMinPay);
        CheckBox acceptMinRateEnabled = findViewById(R.id.acceptMinRateEnabled);
        EditText acceptMinRate = findViewById(R.id.acceptMinRate);
        CheckBox acceptMaxMilesEnabled = findViewById(R.id.acceptMaxMilesEnabled);
        EditText acceptMaxMiles = findViewById(R.id.acceptMaxMiles);
        CheckBox acceptShoppingEnabled = findViewById(R.id.acceptShoppingEnabled);
        CheckBox acceptNoShippingEnabled = findViewById(R.id.acceptNoShippingEnabled);

        serviceStatus = findViewById(R.id.serviceStatus);
        latestDecision = findViewById(R.id.latestDecision);
        diagnostics = findViewById(R.id.diagnostics);
        Button openHistory = findViewById(R.id.openHistory);
        Button openAccessibility = findViewById(R.id.openAccessibility);

        masterEnabled.setChecked(prefs.getBoolean(Prefs.MASTER_ENABLED, false));
        dryRun.setChecked(prefs.getBoolean(Prefs.DRY_RUN, true));
        decisionChimes.setChecked(prefs.getBoolean(Prefs.DECISION_CHIMES, true));
        rejectNoShopping.setChecked(prefs.getBoolean(Prefs.REJECT_NO_SHOPPING, false));
        rejectMinPayEnabled.setChecked(prefs.getBoolean(Prefs.REJECT_MIN_PAY_ENABLED, true));
        rejectMinPay.setText(format(prefs.getFloat(Prefs.REJECT_MIN_PAY, 15.00f), 2));
        rejectLowRate.setChecked(prefs.getBoolean(Prefs.REJECT_LOW_RATE, true));
        rejectThreshold.setText(format(prefs.getFloat(Prefs.THRESHOLD, 1.25f), 2));
        allowTulsa.setChecked(prefs.getBoolean(Prefs.ALLOW_TULSA, false));
        allowGlenpool.setChecked(prefs.getBoolean(Prefs.ALLOW_GLENPOOL, false));
        allowJenks.setChecked(prefs.getBoolean(Prefs.ALLOW_JENKS, false));

        autoAcceptEnabled.setChecked(prefs.getBoolean(Prefs.AUTO_ACCEPT_ENABLED, false));
        acceptMinPayEnabled.setChecked(prefs.getBoolean(Prefs.ACCEPT_MIN_PAY_ENABLED, false));
        acceptMinPay.setText(format(prefs.getFloat(Prefs.ACCEPT_MIN_PAY, 20.00f), 2));
        acceptMinRateEnabled.setChecked(prefs.getBoolean(Prefs.ACCEPT_MIN_RATE_ENABLED, false));
        acceptMinRate.setText(format(prefs.getFloat(Prefs.ACCEPT_MIN_RATE, 1.25f), 2));
        acceptMaxMilesEnabled.setChecked(prefs.getBoolean(Prefs.ACCEPT_MAX_MILES_ENABLED, false));
        acceptMaxMiles.setText(format(prefs.getFloat(Prefs.ACCEPT_MAX_MILES, 10.0f), 1));
        acceptShoppingEnabled.setChecked(prefs.getBoolean(Prefs.ACCEPT_SHOPPING_ENABLED, false));
        acceptNoShippingEnabled.setChecked(prefs.getBoolean(Prefs.ACCEPT_NO_SHIPPING_ENABLED, false));

        refreshCityPolicy();

        bindCheck(masterEnabled, Prefs.MASTER_ENABLED);
        bindCheck(dryRun, Prefs.DRY_RUN);
        bindCheck(decisionChimes, Prefs.DECISION_CHIMES);
        bindCheck(rejectNoShopping, Prefs.REJECT_NO_SHOPPING);
        bindCheck(rejectMinPayEnabled, Prefs.REJECT_MIN_PAY_ENABLED);
        bindNumber(rejectMinPay, Prefs.REJECT_MIN_PAY, 0.01f, 10000.0f);
        bindCheck(rejectLowRate, Prefs.REJECT_LOW_RATE);
        bindNumber(rejectThreshold, Prefs.THRESHOLD, 0.10f, 20.0f);
        bindCheck(allowTulsa, Prefs.ALLOW_TULSA);
        bindCheck(allowGlenpool, Prefs.ALLOW_GLENPOOL);
        bindCheck(allowJenks, Prefs.ALLOW_JENKS);

        bindCheck(autoAcceptEnabled, Prefs.AUTO_ACCEPT_ENABLED);
        bindCheck(acceptMinPayEnabled, Prefs.ACCEPT_MIN_PAY_ENABLED);
        bindNumber(acceptMinPay, Prefs.ACCEPT_MIN_PAY, 0.01f, 10000.0f);
        bindCheck(acceptMinRateEnabled, Prefs.ACCEPT_MIN_RATE_ENABLED);
        bindNumber(acceptMinRate, Prefs.ACCEPT_MIN_RATE, 0.01f, 100.0f);
        bindCheck(acceptMaxMilesEnabled, Prefs.ACCEPT_MAX_MILES_ENABLED);
        bindNumber(acceptMaxMiles, Prefs.ACCEPT_MAX_MILES, 0.1f, 1000.0f);
        bindCheck(acceptShoppingEnabled, Prefs.ACCEPT_SHOPPING_ENABLED);
        bindCheck(acceptNoShippingEnabled, Prefs.ACCEPT_NO_SHIPPING_ENABLED);

        openHistory.setOnClickListener(v ->
                startActivity(new Intent(this, HistoryActivity.class)));
        openAccessibility.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
    }

    private void bindCheck(CheckBox checkBox, String key) {
        checkBox.setOnCheckedChangeListener((button, checked) -> {
            prefs.edit().putBoolean(key, checked).apply();
            refreshCityPolicy();
        });
    }

    private void refreshCityPolicy() {
        CityPolicy.configure(
                prefs.getBoolean(Prefs.ALLOW_TULSA, false),
                prefs.getBoolean(Prefs.ALLOW_GLENPOOL, false),
                prefs.getBoolean(Prefs.ALLOW_JENKS, false));
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
        refreshCityPolicy();
        uiHandler.removeCallbacks(refreshRunnable);
        refreshRunnable.run();
    }

    @Override
    protected void onPause() {
        uiHandler.removeCallbacks(refreshRunnable);
        super.onPause();
    }

    private void refreshStatus() {
        boolean enabled = isAccessibilityServiceEnabled();
        boolean testMode = prefs.getBoolean(Prefs.DRY_RUN, true);
        String modeText = testMode
                ? "\nTEST MODE is ON — decisions are evaluated, but Accept/Reject is NOT pressed."
                : "\nLIVE MODE is ON — matching offers may be accepted/rejected automatically.";
        serviceStatus.setText((enabled
                ? "Accessibility service status: ON"
                : "Accessibility service status: OFF — open settings and enable My Offer Filter service")
                + modeText);

        latestDecision.setText(prefs.getString(Prefs.LAST_DECISION, "No offer evaluated yet."));
        String event = prefs.getString(Prefs.LAST_SPARK_EVENT, "No Spark Accessibility event received yet.");
        String scan = prefs.getString(Prefs.LAST_SCAN_STATUS, "No Spark screen scan yet.");
        String capture = prefs.getString(Prefs.LAST_CAPTURE, "No readable Spark text captured yet.");
        diagnostics.setText(event + "\n\n" + scan + "\n\nVISIBLE SPARK TEXT:\n" + capture);
    }

    private boolean isAccessibilityServiceEnabled() {
        ComponentName expected = new ComponentName(this, SparkOfferAccessibilityService.class);

        AccessibilityManager manager = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (manager != null) {
            List<AccessibilityServiceInfo> enabled = manager.getEnabledAccessibilityServiceList(
                    AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
            for (AccessibilityServiceInfo info : enabled) {
                if (info == null) continue;

                if (info.getResolveInfo() != null && info.getResolveInfo().serviceInfo != null) {
                    ComponentName actual = new ComponentName(
                            info.getResolveInfo().serviceInfo.packageName,
                            info.getResolveInfo().serviceInfo.name);
                    if (expected.equals(actual)) return true;
                }

                String id = info.getId();
                if (id != null) {
                    ComponentName actual = ComponentName.unflattenFromString(id);
                    if (expected.equals(actual)) return true;
                }
            }
        }

        String rawEnabled = Settings.Secure.getString(
                getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (!TextUtils.isEmpty(rawEnabled)) {
            TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
            splitter.setString(rawEnabled);
            while (splitter.hasNext()) {
                ComponentName actual = ComponentName.unflattenFromString(splitter.next());
                if (expected.equals(actual)) return true;
            }
        }

        return false;
    }
}
