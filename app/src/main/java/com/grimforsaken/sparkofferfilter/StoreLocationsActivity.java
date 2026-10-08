package com.grimforsaken.sparkofferfilter;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.graphics.Typeface;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

public class StoreLocationsActivity extends Activity {
    private SharedPreferences prefs;
    private LinearLayout storeList;
    private TextView title;
    private TextView help;
    private TextView empty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(Prefs.NAME, MODE_PRIVATE);
        LanguageText.ensureDefault(prefs);
        StoreLocationRegistry.ensureFreshDynamicList(prefs);
        setContentView(R.layout.activity_store_locations);

        title = findViewById(R.id.storeLocationsTitle);
        help = findViewById(R.id.storeLocationsHelp);
        empty = findViewById(R.id.storeLocationsEmpty);
        storeList = findViewById(R.id.storeLocationsList);
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyLanguage();
        renderStores();
    }

    private void applyLanguage() {
        boolean es = LanguageText.isSpanish(prefs);
        title.setText(es ? "UBICACIONES DE TIENDAS" : "STORE LOCATIONS");
        help.setText(es
                ? "Safe Driver agrega automáticamente una tienda cuando lee una ubicación confiable de Walmart o Sam’s Club en Spark. Las tiendas nuevas empiezan sin marcar. «Aceptada» permite que la tienda pase el filtro de ubicación. «Auto-Aceptar» es una lista separada y solo permite la aceptación automática si también se cumplen todas las demás reglas."
                : "Safe Driver automatically adds a store whenever it reads a reliable Walmart or Sam’s Club location in Spark. New stores start unchecked. “Accepted” allows the store through the location filter. “Auto-Accept” is separate and only permits automatic acceptance when every other Auto-Accept rule also passes.");
        empty.setText(es
                ? "Todavía no se han detectado tiendas. Mantén activado el servicio de Accesibilidad de Safe Driver y abre ofertas de Spark; las tiendas aparecerán aquí automáticamente."
                : "No stores have been discovered yet. Keep the Safe Driver Accessibility service enabled and view Spark offers; stores will appear here automatically.");
    }

    private void renderStores() {
        storeList.removeAllViews();
        List<StoreLocationRegistry.StoreEntry> stores = StoreLocationRegistry.stores(prefs);
        empty.setVisibility(stores.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
        boolean es = LanguageText.isSpanish(prefs);

        for (StoreLocationRegistry.StoreEntry entry : stores) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(12, 12, 12, 12);
            row.setBackgroundColor(0xFFF3F3F3);

            TextView label = new TextView(this);
            label.setText(entry.label);
            label.setTextSize(17f);
            label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            label.setTextIsSelectable(true);
            row.addView(label, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            CheckBox accepted = new CheckBox(this);
            accepted.setText(es ? "Aceptada" : "Accepted");
            accepted.setChecked(entry.accepted);

            CheckBox autoAccept = new CheckBox(this);
            autoAccept.setText(es ? "Auto-Aceptar" : "Auto-Accept");
            autoAccept.setChecked(entry.autoAccept);

            accepted.setOnCheckedChangeListener((buttonView, isChecked) -> {
                StoreLocationRegistry.setAccepted(prefs, entry.key, isChecked);
                if (!isChecked && autoAccept.isChecked()) {
                    autoAccept.setOnCheckedChangeListener(null);
                    autoAccept.setChecked(false);
                    autoAccept.setOnCheckedChangeListener((v, checked) -> onAutoChanged(entry, accepted, v, checked));
                }
            });
            autoAccept.setOnCheckedChangeListener((buttonView, isChecked) ->
                    onAutoChanged(entry, accepted, buttonView, isChecked));

            row.addView(accepted);
            row.addView(autoAccept);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, 12);
            storeList.addView(row, lp);
        }
    }

    private void onAutoChanged(StoreLocationRegistry.StoreEntry entry, CheckBox accepted,
                               android.widget.CompoundButton source, boolean isChecked) {
        StoreLocationRegistry.setAutoAccept(prefs, entry.key, isChecked);
        if (isChecked && !accepted.isChecked()) {
            accepted.setChecked(true);
        }
    }
}
