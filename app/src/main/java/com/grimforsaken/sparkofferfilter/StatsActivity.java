package com.grimforsaken.sparkofferfilter;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TextView;

import java.text.DateFormat;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatsActivity extends Activity {
    private SharedPreferences prefs;
    private TabHost tabHost;
    private EditText gasPrice;
    private TextView title;
    private TextView description;
    private TextView gasHelp;
    private LinearLayout ordersList;
    private LinearLayout dayList;
    private LinearLayout weekList;
    private LinearLayout monthList;
    private LinearLayout yearList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);
        prefs = getSharedPreferences(Prefs.NAME, MODE_PRIVATE);
        LanguageText.ensureDefault(prefs);

        title = findViewById(R.id.statsTitle);
        description = findViewById(R.id.statsDescription);
        gasHelp = findViewById(R.id.gasHelp);
        gasPrice = findViewById(R.id.gasPrice);
        ordersList = findViewById(R.id.ordersList);
        dayList = findViewById(R.id.dayList);
        weekList = findViewById(R.id.weekList);
        monthList = findViewById(R.id.monthList);
        yearList = findViewById(R.id.yearList);
        tabHost = findViewById(android.R.id.tabhost);

        if (tabHost == null) {
            finish();
            return;
        }

        tabHost.setup();
        tabHost.addTab(tabHost.newTabSpec("orders").setIndicator("Orders").setContent(R.id.ordersTab));
        tabHost.addTab(tabHost.newTabSpec("day").setIndicator("Day").setContent(R.id.dayTab));
        tabHost.addTab(tabHost.newTabSpec("week").setIndicator("Week").setContent(R.id.weekTab));
        tabHost.addTab(tabHost.newTabSpec("month").setIndicator("Month").setContent(R.id.monthTab));
        tabHost.addTab(tabHost.newTabSpec("year").setIndicator("Year").setContent(R.id.yearTab));

        gasPrice.setText(String.format(Locale.US, "%.2f", prefs.getFloat(Prefs.GAS_PRICE, 0.0f)));
        gasPrice.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                try {
                    float value = Float.parseFloat(s.toString());
                    if (value >= 0.0f && value <= 100.0f) {
                        prefs.edit().putFloat(Prefs.GAS_PRICE, value).apply();
                    }
                } catch (NumberFormatException ignored) {}
            }
        });

        applyLanguage();
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyLanguage();
        refresh();
    }

    private void applyLanguage() {
        boolean es = LanguageText.isSpanish(prefs);
        title.setText(es ? "Comparación de ganancias" : "Order Earnings Comparison");
        description.setText(es
                ? "Las órdenes de Compras se registran cuando Safe Driver ve la pantalla activa «Stop #… for Trip … / CONFIRM ARRIVAL». Compara dólares por milla y dólares por hora. El combustible se estima con 35 MPG."
                : "Shopping orders are recorded when Safe Driver sees the active “Stop #… for Trip … / CONFIRM ARRIVAL” screen. Compare dollars per mile and dollars per hour. Fuel is estimated at 35 MPG.");
        gasHelp.setText(es
                ? "Precio actual de gasolina ($/gal). Este precio se mantiene para los días futuros hasta que lo cambies y se guarda con cada orden confirmada."
                : "Current gas price ($/gal). This carries forward day by day until you change it and is saved with each confirmed order.");

        if (tabHost.getTabWidget() != null && tabHost.getTabWidget().getTabCount() >= 5) {
            String[] labels = es
                    ? new String[]{"Órdenes", "Día", "Semana", "Mes", "Año"}
                    : new String[]{"Orders", "Day", "Week", "Month", "Year"};
            for (int i = 0; i < 5; i++) {
                TextView label = tabHost.getTabWidget().getChildTabViewAt(i).findViewById(android.R.id.title);
                if (label != null) label.setText(labels[i]);
            }
        }
    }

    private void refresh() {
        List<OrderRecord> records = AcceptedOrderStore.records(prefs);
        renderOrders(records);
        renderGroups(dayList, records, "day");
        renderGroups(weekList, records, "week");
        renderGroups(monthList, records, "month");
        renderGroups(yearList, records, "year");
    }

    private void renderOrders(List<OrderRecord> records) {
        ordersList.removeAllViews();
        if (records.isEmpty()) {
            addText(ordersList, emptyMessage(), false);
            return;
        }
        for (OrderRecord r : records) {
            String when = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                    .format(new java.util.Date(r.timestampMs));
            String place = !r.store.isEmpty() ? r.store : (!r.city.isEmpty() ? r.city : "Unknown location");
            String trip = r.tripId.isEmpty() ? "" : " • Trip " + r.tripId;
            String line1 = when + " • " + place + trip;
            String line2 = String.format(Locale.US,
                    "$%.2f • %.1f mi • %s\n$%.2f/mi  ↔  $%.2f/hr",
                    r.pay, r.miles, formatDuration(r.minutes),
                    r.dollarsPerMile(), r.dollarsPerHour());
            String line3;
            if (r.gasPrice > 0.0) {
                line3 = String.format(Locale.US,
                        "Gas $%.2f/gal @ 35 MPG: $%.2f • After fuel: $%.2f",
                        r.gasPrice, r.fuelCostAt35Mpg(), r.afterFuel());
            } else {
                line3 = LanguageText.isSpanish(prefs)
                        ? "Gasolina @ 35 MPG: precio no establecido para esta orden"
                        : "Gas @ 35 MPG: gas price was not set for this order";
            }
            addText(ordersList, line1 + "\n" + line2 + "\n" + line3, true);
        }
    }

    private void renderGroups(LinearLayout target, List<OrderRecord> records, String kind) {
        target.removeAllViews();
        if (records.isEmpty()) {
            addText(target, emptyMessage(), false);
            return;
        }

        Map<String, List<OrderRecord>> grouped = new LinkedHashMap<>();
        Map<String, String> labels = new LinkedHashMap<>();
        ZoneId zone = ZoneId.systemDefault();

        for (OrderRecord r : records) {
            LocalDate date = Instant.ofEpochMilli(r.timestampMs).atZone(zone).toLocalDate();
            String key;
            String label;
            if ("day".equals(kind)) {
                key = date.toString();
                label = date.format(DateTimeFormatter.ofPattern("EEE M/d/yyyy", Locale.getDefault()));
            } else if ("week".equals(kind)) {
                LocalDate monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                key = monday.toString();
                label = (LanguageText.isSpanish(prefs) ? "Semana del " : "Week of ")
                        + monday.format(DateTimeFormatter.ofPattern("M/d/yyyy"));
            } else if ("month".equals(kind)) {
                YearMonth month = YearMonth.from(date);
                key = month.toString();
                label = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()));
            } else {
                key = String.valueOf(date.getYear());
                label = key;
            }
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
            labels.putIfAbsent(key, label);
        }

        for (Map.Entry<String, List<OrderRecord>> entry : grouped.entrySet()) {
            OrderAnalytics.Summary s = OrderAnalytics.summarize(entry.getValue());
            String text = formatSummary(labels.get(entry.getKey()), s);
            addText(target, text, true);
        }
    }

    private String formatSummary(String label, OrderAnalytics.Summary s) {
        boolean es = LanguageText.isSpanish(prefs);
        String first = label + " • " + s.count + (es ? (s.count == 1 ? " orden" : " órdenes")
                : (s.count == 1 ? " order" : " orders"));
        String metrics = String.format(Locale.US,
                "$%.2f • %.1f mi • %s\n$%.2f/mi  ↔  $%.2f/hr",
                s.totalPay, s.totalMiles, formatDuration(s.totalMinutes),
                s.dollarsPerMile, s.dollarsPerHour);
        String gas;
        if (s.averageGasPrice > 0.0) {
            gas = String.format(Locale.US,
                    "%s $%.2f/gal @ 35 MPG • %s $%.2f • %s $%.2f",
                    es ? "Gasolina prom." : "Avg gas",
                    s.averageGasPrice,
                    es ? "Costo combustible:" : "Fuel cost:",
                    s.fuelCost,
                    es ? "Después de combustible:" : "After fuel:",
                    s.afterFuel);
        } else {
            gas = es ? "Gasolina @ 35 MPG: precio no establecido"
                    : "Gas @ 35 MPG: gas price not set";
        }
        return first + "\n" + metrics + "\n" + gas;
    }

    private void addText(LinearLayout target, String text, boolean divider) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(15f);
        view.setTextIsSelectable(true);
        view.setPadding(14, 14, 14, 14);
        if (divider) view.setBackgroundColor(0xFFF3F3F3);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 10);
        target.addView(view, lp);
    }

    private String emptyMessage() {
        return LanguageText.isSpanish(prefs)
                ? "Aún no hay órdenes de Compras confirmadas."
                : "No confirmed Shopping orders recorded yet.";
    }

    private String formatDuration(int minutes) {
        int h = minutes / 60;
        int m = minutes % 60;
        if (h == 0) return m + " min";
        if (m == 0) return h + " hr";
        return h + " hr " + m + " min";
    }
}
