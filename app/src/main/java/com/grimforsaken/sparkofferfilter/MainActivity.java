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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        prefs = getSharedPreferences(Prefs.NAME, MODE_PRIVATE);
        LanguageText.ensureDefault(prefs);
        migrateNoShoppingPreference();
        setContentView(R.layout.activity_main);

        CheckBox masterEnabled = findViewById(R.id.masterEnabled);
        CheckBox dryRun = findViewById(R.id.dryRun);
        CheckBox decisionChimes = findViewById(R.id.decisionChimes);
        CheckBox rejectNoShopping = findViewById(R.id.rejectNoShopping);
        CheckBox rejectMinPayEnabled = findViewById(R.id.rejectMinPayEnabled);
        EditText rejectMinPay = findViewById(R.id.rejectMinPay);
        CheckBox rejectLowRate = findViewById(R.id.rejectLowRate);
        EditText rejectThreshold = findViewById(R.id.rejectThreshold);
        CheckBox rejectMaxMilesEnabled = findViewById(R.id.rejectMaxMilesEnabled);
        EditText rejectMaxMiles = findViewById(R.id.rejectMaxMiles);
        CheckBox rejectThreePlusDropoffs = findViewById(R.id.rejectThreePlusDropoffs);

        CheckBox allowSandSprings = findViewById(R.id.allowSandSprings);
        CheckBox allowSapulpa = findViewById(R.id.allowSapulpa);
        CheckBox allowTulsa = findViewById(R.id.allowTulsa);
        CheckBox allowGlenpool = findViewById(R.id.allowGlenpool);
        CheckBox allowJenks = findViewById(R.id.allowJenks);
        CheckBox allowSamsClub = findViewById(R.id.allowSamsClub);

        CheckBox autoAcceptEnabled = findViewById(R.id.autoAcceptEnabled);
        CheckBox acceptAllowSandSprings = findViewById(R.id.acceptAllowSandSprings);
        CheckBox acceptAllowSapulpa = findViewById(R.id.acceptAllowSapulpa);
        CheckBox acceptAllowTulsa = findViewById(R.id.acceptAllowTulsa);
        CheckBox acceptAllowGlenpool = findViewById(R.id.acceptAllowGlenpool);
        CheckBox acceptAllowJenks = findViewById(R.id.acceptAllowJenks);
        CheckBox acceptAllowSamsClub = findViewById(R.id.acceptAllowSamsClub);
        CheckBox acceptMinPayEnabled = findViewById(R.id.acceptMinPayEnabled);
        EditText acceptMinPay = findViewById(R.id.acceptMinPay);
        CheckBox acceptMinRateEnabled = findViewById(R.id.acceptMinRateEnabled);
        EditText acceptMinRate = findViewById(R.id.acceptMinRate);
        CheckBox acceptMaxMilesEnabled = findViewById(R.id.acceptMaxMilesEnabled);
        EditText acceptMaxMiles = findViewById(R.id.acceptMaxMiles);
        CheckBox acceptShoppingEnabled = findViewById(R.id.acceptShoppingEnabled);
        CheckBox acceptNoShoppingEnabled = findViewById(R.id.acceptNoShoppingEnabled);

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
        rejectMaxMilesEnabled.setChecked(prefs.getBoolean(Prefs.REJECT_MAX_MILES_ENABLED, false));
        rejectMaxMiles.setText(format(prefs.getFloat(Prefs.REJECT_MAX_MILES, 20.0f), 1));
        rejectThreePlusDropoffs.setChecked(prefs.getBoolean(Prefs.REJECT_3_PLUS_DROPOFFS, false));

        allowSandSprings.setChecked(prefs.getBoolean(Prefs.ALLOW_SAND_SPRINGS, true));
        allowSapulpa.setChecked(prefs.getBoolean(Prefs.ALLOW_SAPULPA, true));
        allowTulsa.setChecked(prefs.getBoolean(Prefs.ALLOW_TULSA, false));
        allowGlenpool.setChecked(prefs.getBoolean(Prefs.ALLOW_GLENPOOL, false));
        allowJenks.setChecked(prefs.getBoolean(Prefs.ALLOW_JENKS, false));
        allowSamsClub.setChecked(prefs.getBoolean(Prefs.ALLOW_SAMS_CLUB, false));

        autoAcceptEnabled.setChecked(prefs.getBoolean(Prefs.AUTO_ACCEPT_ENABLED, false));
        acceptAllowSandSprings.setChecked(prefs.getBoolean(Prefs.ACCEPT_LOCATION_SAND_SPRINGS, true));
        acceptAllowSapulpa.setChecked(prefs.getBoolean(Prefs.ACCEPT_LOCATION_SAPULPA, true));
        acceptAllowTulsa.setChecked(prefs.getBoolean(Prefs.ACCEPT_LOCATION_TULSA, false));
        acceptAllowGlenpool.setChecked(prefs.getBoolean(Prefs.ACCEPT_LOCATION_GLENPOOL, false));
        acceptAllowJenks.setChecked(prefs.getBoolean(Prefs.ACCEPT_LOCATION_JENKS, false));
        acceptAllowSamsClub.setChecked(prefs.getBoolean(Prefs.ACCEPT_LOCATION_SAMS_CLUB, false));
        acceptMinPayEnabled.setChecked(prefs.getBoolean(Prefs.ACCEPT_MIN_PAY_ENABLED, false));
        acceptMinPay.setText(format(prefs.getFloat(Prefs.ACCEPT_MIN_PAY, 20.00f), 2));
        acceptMinRateEnabled.setChecked(prefs.getBoolean(Prefs.ACCEPT_MIN_RATE_ENABLED, false));
        acceptMinRate.setText(format(prefs.getFloat(Prefs.ACCEPT_MIN_RATE, 1.25f), 2));
        acceptMaxMilesEnabled.setChecked(prefs.getBoolean(Prefs.ACCEPT_MAX_MILES_ENABLED, false));
        acceptMaxMiles.setText(format(prefs.getFloat(Prefs.ACCEPT_MAX_MILES, 10.0f), 1));
        acceptShoppingEnabled.setChecked(prefs.getBoolean(Prefs.ACCEPT_SHOPPING_ENABLED, false));
        acceptNoShoppingEnabled.setChecked(prefs.getBoolean(Prefs.ACCEPT_NO_SHOPPING_ENABLED, false));

        refreshLocationPolicies();

        bindCheck(masterEnabled, Prefs.MASTER_ENABLED);
        bindCheck(dryRun, Prefs.DRY_RUN);
        bindCheck(decisionChimes, Prefs.DECISION_CHIMES);
        bindCheck(rejectNoShopping, Prefs.REJECT_NO_SHOPPING);
        bindCheck(rejectMinPayEnabled, Prefs.REJECT_MIN_PAY_ENABLED);
        bindNumber(rejectMinPay, Prefs.REJECT_MIN_PAY, 0.01f, 10000.0f);
        bindCheck(rejectLowRate, Prefs.REJECT_LOW_RATE);
        bindNumber(rejectThreshold, Prefs.THRESHOLD, 0.10f, 20.0f);
        bindCheck(rejectMaxMilesEnabled, Prefs.REJECT_MAX_MILES_ENABLED);
        bindNumber(rejectMaxMiles, Prefs.REJECT_MAX_MILES, 0.1f, 1000.0f);
        bindCheck(rejectThreePlusDropoffs, Prefs.REJECT_3_PLUS_DROPOFFS);

        bindCheck(allowSandSprings, Prefs.ALLOW_SAND_SPRINGS);
        bindCheck(allowSapulpa, Prefs.ALLOW_SAPULPA);
        bindCheck(allowTulsa, Prefs.ALLOW_TULSA);
        bindCheck(allowGlenpool, Prefs.ALLOW_GLENPOOL);
        bindCheck(allowJenks, Prefs.ALLOW_JENKS);
        bindCheck(allowSamsClub, Prefs.ALLOW_SAMS_CLUB);

        bindCheck(autoAcceptEnabled, Prefs.AUTO_ACCEPT_ENABLED);
        bindCheck(acceptAllowSandSprings, Prefs.ACCEPT_LOCATION_SAND_SPRINGS);
        bindCheck(acceptAllowSapulpa, Prefs.ACCEPT_LOCATION_SAPULPA);
        bindCheck(acceptAllowTulsa, Prefs.ACCEPT_LOCATION_TULSA);
        bindCheck(acceptAllowGlenpool, Prefs.ACCEPT_LOCATION_GLENPOOL);
        bindCheck(acceptAllowJenks, Prefs.ACCEPT_LOCATION_JENKS);
        bindCheck(acceptAllowSamsClub, Prefs.ACCEPT_LOCATION_SAMS_CLUB);
        bindCheck(acceptMinPayEnabled, Prefs.ACCEPT_MIN_PAY_ENABLED);
        bindNumber(acceptMinPay, Prefs.ACCEPT_MIN_PAY, 0.01f, 10000.0f);
        bindCheck(acceptMinRateEnabled, Prefs.ACCEPT_MIN_RATE_ENABLED);
        bindNumber(acceptMinRate, Prefs.ACCEPT_MIN_RATE, 0.01f, 100.0f);
        bindCheck(acceptMaxMilesEnabled, Prefs.ACCEPT_MAX_MILES_ENABLED);
        bindNumber(acceptMaxMiles, Prefs.ACCEPT_MAX_MILES, 0.1f, 1000.0f);
        bindCheck(acceptShoppingEnabled, Prefs.ACCEPT_SHOPPING_ENABLED);
        bindCheck(acceptNoShoppingEnabled, Prefs.ACCEPT_NO_SHOPPING_ENABLED);

        findViewById(R.id.languageEnglish).setOnClickListener(v -> {
            LanguageText.setSpanish(prefs, false);
            applyLanguage();
            refreshStatus();
        });
        findViewById(R.id.languageSpanish).setOnClickListener(v -> {
            LanguageText.setSpanish(prefs, true);
            applyLanguage();
            refreshStatus();
        });

        openHistory.setOnClickListener(v -> startActivity(new Intent(this, HistoryActivity.class)));
        openAccessibility.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        applyLanguage();
    }

    private void migrateNoShoppingPreference() {
        if (!prefs.contains(Prefs.ACCEPT_NO_SHOPPING_ENABLED)
                && prefs.contains(Prefs.LEGACY_ACCEPT_NO_SHIPPING_ENABLED)) {
            boolean oldValue = prefs.getBoolean(Prefs.LEGACY_ACCEPT_NO_SHIPPING_ENABLED, false);
            prefs.edit()
                    .putBoolean(Prefs.ACCEPT_NO_SHOPPING_ENABLED, oldValue)
                    .remove(Prefs.LEGACY_ACCEPT_NO_SHIPPING_ENABLED)
                    .apply();
        }
    }

    private void applyLanguage() {
        boolean es = LanguageText.isSpanish(prefs);
        ((TextView) findViewById(R.id.appTitle)).setText("Safe Driver");
        ((TextView) findViewById(R.id.appSubtitle)).setText(es
                ? "Compañero personal no oficial para Spark Driver. Usa el Modo de prueba mientras estés estacionado hasta que Diagnóstico en vivo muestre correctamente la información de la oferta."
                : "Unofficial personal Spark Driver companion. Use Test mode while parked until Live Diagnostics shows the offer information correctly.");

        ((CheckBox) findViewById(R.id.masterEnabled)).setText(es ? "Activar filtrado automático de ofertas" : "Enable automatic offer filtering");
        ((CheckBox) findViewById(R.id.dryRun)).setText(es ? "Modo de prueba — registrar decisiones sin pulsar Aceptar ni Rechazar" : "Test mode — log decisions but do not click Accept or Reject");
        ((CheckBox) findViewById(R.id.decisionChimes)).setText(es ? "Reproducir sonidos separados para Aceptar / Rechazar" : "Play separate Accept / Reject chimes");

        ((TextView) findViewById(R.id.rejectHeading)).setText(es ? "REGLAS DE RECHAZO AUTOMÁTICO" : "AUTO-REJECT RULES");
        ((TextView) findViewById(R.id.rejectPriorityText)).setText(es
                ? "Las reglas de monto mínimo, máximo de millas, dólares por milla, 3 o más entregas y Compras siguen aplicándose incluso cuando Sand Springs o Sapulpa están permitidas. Si la ubicación es desconocida, Safe Driver espera 2 segundos y vuelve a comprobar; si sigue siendo desconocida, deja el pedido para revisión manual. Los pedidos ya aceptados permanecen protegidos por los bloqueos de seguridad."
                : "Minimum-dollar, maximum-mile, dollars-per-mile, 3+ drop-off, and Shopping reject rules still apply even when Sand Springs or Sapulpa is allowed. If the location is unknown, Safe Driver waits 2 seconds and checks again; if it is still unknown, the order is left for manual review. Already accepted offers remain protected by the safety locks.");

        ((TextView) findViewById(R.id.locationHeading)).setText(es ? "UBICACIONES ACEPTADAS" : "ACCEPTED LOCATIONS");
        ((TextView) findViewById(R.id.locationHelp)).setText(es
                ? "Esta lista solo controla el filtro de ubicación. Una ubicación marcada NO omite las reglas de monto mínimo, máximo de millas, dólares por milla ni Compras. Sand Springs y Sapulpa están marcadas de forma predeterminada."
                : "This list only controls the location filter. A checked location does NOT bypass minimum-dollar, maximum-mile, dollars-per-mile, or Shopping reject rules. Sand Springs and Sapulpa are checked by default.");
        ((CheckBox) findViewById(R.id.allowSandSprings)).setText(es ? "Aceptar pedidos de Sand Springs" : "Accept Sand Springs offers");
        ((CheckBox) findViewById(R.id.allowSapulpa)).setText(es ? "Aceptar pedidos de Sapulpa" : "Accept Sapulpa offers");
        ((CheckBox) findViewById(R.id.allowTulsa)).setText(es ? "Aceptar pedidos de Tulsa" : "Accept Tulsa offers");
        ((CheckBox) findViewById(R.id.allowGlenpool)).setText(es ? "Aceptar pedidos de Glenpool" : "Accept Glenpool offers");
        ((CheckBox) findViewById(R.id.allowJenks)).setText(es ? "Aceptar pedidos de Jenks" : "Accept Jenks offers");
        ((CheckBox) findViewById(R.id.allowSamsClub)).setText(es ? "Aceptar pedidos de Sam’s Club" : "Accept Sam’s Club offers");

        ((CheckBox) findViewById(R.id.rejectNoShopping)).setText(es ? "Rechazar pedidos que no muestran Compras" : "Reject orders that do not show Shopping");
        ((CheckBox) findViewById(R.id.rejectMinPayEnabled)).setText(es ? "Rechazar pedidos por debajo de este monto mínimo" : "Reject orders below this minimum order dollar amount");
        ((TextView) findViewById(R.id.rejectMinPayLabel)).setText(es ? "Rechazar por debajo de $:  " : "Reject orders below $:  ");
        ((CheckBox) findViewById(R.id.rejectLowRate)).setText(es ? "Rechazar pedidos por debajo de este monto de dólares por milla" : "Reject orders below this dollars-per-mile amount");
        ((TextView) findViewById(R.id.rejectRateLabel)).setText(es ? "Rechazar por debajo de $ / milla:  " : "Reject below $ / mile:  ");
        ((CheckBox) findViewById(R.id.rejectMaxMilesEnabled)).setText(es ? "Rechazar pedidos que superen este máximo de millas" : "Reject orders over this maximum number of miles");
        ((TextView) findViewById(R.id.rejectMaxMilesLabel)).setText(es ? "Rechazar por encima de millas:  " : "Reject over miles:  ");
        ((CheckBox) findViewById(R.id.rejectThreePlusDropoffs)).setText(es ? "Rechazar pedidos con 3 o más entregas" : "Reject orders with 3 or more drop-offs");

        ((TextView) findViewById(R.id.acceptHeading)).setText(es ? "REGLAS DE ACEPTACIÓN AUTOMÁTICA" : "AUTO-ACCEPT RULES");
        ((CheckBox) findViewById(R.id.autoAcceptEnabled)).setText(es ? "Activar aceptación automática" : "Enable Auto-Accept");
        ((TextView) findViewById(R.id.acceptLocationHeading)).setText(es ? "UBICACIONES DE ACEPTACIÓN AUTOMÁTICA" : "AUTO-ACCEPT LOCATIONS");
        ((TextView) findViewById(R.id.acceptLocationHelp)).setText(es
                ? "Esta es una lista separada. Safe Driver solo puede pulsar Aceptar automáticamente cuando la ubicación está marcada aquí Y se cumplen todas las demás reglas de aceptación automática. Sand Springs y Sapulpa están marcadas de forma predeterminada."
                : "This is a separate checklist. Safe Driver can only press Accept automatically when the location is checked here AND every other enabled Auto-Accept rule passes. Sand Springs and Sapulpa are checked by default.");
        ((CheckBox) findViewById(R.id.acceptAllowSandSprings)).setText(es ? "Aceptar automáticamente Sand Springs" : "Auto-Accept Sand Springs");
        ((CheckBox) findViewById(R.id.acceptAllowSapulpa)).setText(es ? "Aceptar automáticamente Sapulpa" : "Auto-Accept Sapulpa");
        ((CheckBox) findViewById(R.id.acceptAllowTulsa)).setText(es ? "Aceptar automáticamente Tulsa" : "Auto-Accept Tulsa");
        ((CheckBox) findViewById(R.id.acceptAllowGlenpool)).setText(es ? "Aceptar automáticamente Glenpool" : "Auto-Accept Glenpool");
        ((CheckBox) findViewById(R.id.acceptAllowJenks)).setText(es ? "Aceptar automáticamente Jenks" : "Auto-Accept Jenks");
        ((CheckBox) findViewById(R.id.acceptAllowSamsClub)).setText(es ? "Aceptar automáticamente Sam’s Club" : "Auto-Accept Sam’s Club");
        ((TextView) findViewById(R.id.acceptHelp)).setText(es
                ? "Cada criterio activado debe cumplirse. Para Compras: una sola casilla limita el tipo de pedido, ambas permiten cualquiera y ninguna ignora el estado de Compras."
                : "Every enabled Auto-Accept criterion must pass. Shopping choices work together: one checked limits the order type, both checked allow either type, neither checked ignores Shopping status.");
        ((CheckBox) findViewById(R.id.acceptShoppingEnabled)).setText(es ? "Aceptar pedidos de compras" : "Accept Shopping orders");
        ((CheckBox) findViewById(R.id.acceptNoShoppingEnabled)).setText(es ? "Aceptar pedidos que no incluyen compras" : "Accept orders that do not include Shopping");
        ((CheckBox) findViewById(R.id.acceptMinPayEnabled)).setText(es ? "Requerir un monto mínimo del pedido" : "Require a minimum order dollar amount");
        ((TextView) findViewById(R.id.acceptMinPayLabel)).setText(es ? "Monto mínimo $:  " : "Minimum order $:  ");
        ((CheckBox) findViewById(R.id.acceptMinRateEnabled)).setText(es ? "Requerir un mínimo de dólares por milla" : "Require a minimum dollars-per-mile amount");
        ((TextView) findViewById(R.id.acceptMinRateLabel)).setText(es ? "Mínimo $ / milla:  " : "Minimum $ / mile:  ");
        ((CheckBox) findViewById(R.id.acceptMaxMilesEnabled)).setText(es ? "Requerir un máximo de millas" : "Require a maximum number of miles");
        ((TextView) findViewById(R.id.acceptMaxMilesLabel)).setText(es ? "Máximo de millas:  " : "Maximum miles:  ");

        ((Button) findViewById(R.id.openHistory)).setText(es ? "Historial de pedidos aceptados / rechazados" : "Accepted / Rejected Order History");
        ((Button) findViewById(R.id.openAccessibility)).setText(es ? "Abrir ajustes de accesibilidad" : "Open Accessibility Settings");
        ((TextView) findViewById(R.id.latestHeading)).setText(es ? "ÚLTIMA DECISIÓN" : "LATEST DECISION");
        ((TextView) findViewById(R.id.diagnosticsHeading)).setText(es ? "DIAGNÓSTICO EN VIVO" : "LIVE DIAGNOSTICS");
        ((TextView) findViewById(R.id.safetyHelp)).setText(es
                ? "Después de una aceptación exitosa, todas las acciones y confirmaciones de rechazo se desactivan durante 10 segundos."
                : "After a successful acceptance, all reject actions and reject confirmations are disabled for 10 seconds.");
        ((TextView) findViewById(R.id.diagnosticHelp)).setText(es
                ? "Si una oferta está visible pero no ocurre ninguna decisión, vuelve aquí y revisa Diagnóstico en vivo."
                : "If an offer is visible but no decision occurs, return here and read Live Diagnostics.");
    }

    private void bindCheck(CheckBox checkBox, String key) {
        checkBox.setOnCheckedChangeListener((button, checked) -> {
            prefs.edit().putBoolean(key, checked).apply();
            refreshLocationPolicies();
        });
    }

    private void refreshLocationPolicies() {
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

    private void bindNumber(EditText editText, String key, float min, float max) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                try {
                    float value = Float.parseFloat(s.toString());
                    if (value >= min && value <= max) prefs.edit().putFloat(key, value).apply();
                } catch (NumberFormatException ignored) {}
            }
        });
    }

    private String format(float value, int decimals) {
        return String.format(Locale.US, decimals == 1 ? "%.1f" : "%.2f", (double) value);
    }

    @Override protected void onResume() {
        super.onResume();
        if (prefs != null) {
            refreshLocationPolicies();
            if (findViewById(R.id.appTitle) != null) applyLanguage();
        }
        uiHandler.removeCallbacks(refreshRunnable);
        refreshRunnable.run();
    }

    @Override protected void onPause() {
        uiHandler.removeCallbacks(refreshRunnable);
        super.onPause();
    }

    private void refreshStatus() {
        if (serviceStatus == null || prefs == null) return;
        boolean es = LanguageText.isSpanish(prefs);
        boolean enabled = isAccessibilityServiceEnabled();
        boolean testMode = prefs.getBoolean(Prefs.DRY_RUN, true);
        String modeText = es
                ? (testMode
                    ? "\nMODO DE PRUEBA ACTIVADO — se evalúan decisiones, pero no se pulsa Aceptar/Rechazar."
                    : "\nMODO EN VIVO ACTIVADO — las ofertas coincidentes pueden aceptarse/rechazarse automáticamente.")
                : (testMode
                    ? "\nTEST MODE is ON — decisions are evaluated, but Accept/Reject is NOT pressed."
                    : "\nLIVE MODE is ON — matching offers may be accepted/rejected automatically.");

        serviceStatus.setText((enabled
                ? (es ? "Estado del servicio de accesibilidad: ACTIVADO" : "Accessibility service status: ON")
                : (es ? "Estado del servicio de accesibilidad: DESACTIVADO — abre los ajustes y activa Safe Driver"
                      : "Accessibility service status: OFF — open settings and enable Safe Driver service")) + modeText);

        String decision = prefs.getString(Prefs.LAST_DECISION, es ? "Aún no se ha evaluado ninguna oferta." : "No offer evaluated yet.");
        String event = prefs.getString(Prefs.LAST_SPARK_EVENT, es ? "Aún no se ha recibido ningún evento de Spark." : "No Spark Accessibility event received yet.");
        String scan = prefs.getString(Prefs.LAST_SCAN_STATUS, es ? "Aún no se ha escaneado ninguna pantalla de Spark." : "No Spark screen scan yet.");
        String capture = prefs.getString(Prefs.LAST_CAPTURE, es ? "Aún no se ha capturado texto legible de Spark." : "No readable Spark text captured yet.");

        latestDecision.setText(localizeSafetyDecision(decision, es));
        diagnostics.setText(localizeSafetyStatus(event, es) + "\n\n"
                + localizeSafetyStatus(scan, es) + "\n\n"
                + (es ? "TEXTO DE SPARK:\n" : "VISIBLE SPARK TEXT:\n") + capture);
    }

    private String localizeSafetyDecision(String raw, boolean es) {
        String acceptedRaw = "ACCEPTED immediately. Rejections locked for 10 seconds.";
        String blockedRaw = "REJECTION BLOCKED by 10-second post-accept safety lock.";
        if (raw.contains(acceptedRaw)) {
            return raw.replace(acceptedRaw, es
                    ? "Pedido aceptado. Las acciones de rechazo están desactivadas durante 10 segundos."
                    : "Order accepted. Reject actions are disabled for 10 seconds.");
        }
        if (raw.contains(blockedRaw)) {
            return raw.replace(blockedRaw, es
                    ? "Rechazo bloqueado por el temporizador de seguridad posterior a la aceptación."
                    : "Reject blocked by post-accept safety timer.");
        }
        return raw;
    }

    private String localizeSafetyStatus(String raw, boolean es) {
        if (raw.contains("POST-ACCEPT SAFETY:")) {
            Matcher m = Pattern.compile("([0-9]+(?:\\.[0-9]+)?) more seconds").matcher(raw);
            String remaining = m.find() ? m.group(1) : null;
            String base = es
                    ? "Pedido aceptado. Las acciones de rechazo están desactivadas durante 10 segundos."
                    : "Order accepted. Reject actions are disabled for 10 seconds.";
            if (remaining != null) {
                base += es ? " Quedan " + remaining + " segundos." : " " + remaining + " seconds remaining.";
            }
            return base;
        }
        return localizeSafetyDecision(raw, es);
    }

    private boolean isAccessibilityServiceEnabled() {
        ComponentName expected = new ComponentName(this, SparkOfferAccessibilityService.class);
        AccessibilityManager manager = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (manager != null) {
            List<AccessibilityServiceInfo> enabled = manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
            for (AccessibilityServiceInfo info : enabled) {
                if (info == null) continue;
                if (info.getResolveInfo() != null && info.getResolveInfo().serviceInfo != null) {
                    ComponentName actual = new ComponentName(info.getResolveInfo().serviceInfo.packageName, info.getResolveInfo().serviceInfo.name);
                    if (expected.equals(actual)) return true;
                }
                String id = info.getId();
                if (id != null) {
                    ComponentName actual = ComponentName.unflattenFromString(id);
                    if (expected.equals(actual)) return true;
                }
            }
        }

        String rawEnabled = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
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
