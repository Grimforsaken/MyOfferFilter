package com.grimforsaken.sparkofferfilter;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TabHost;
import android.widget.TextView;

public class HistoryActivity extends Activity {
    private SharedPreferences prefs;
    private TextView rejectedHistory;
    private TextView acceptedHistory;
    private TabHost tabHost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        prefs = getSharedPreferences(Prefs.NAME, MODE_PRIVATE);
        LanguageText.ensureDefault(prefs);

        rejectedHistory = findViewById(R.id.rejectedHistory);
        acceptedHistory = findViewById(R.id.acceptedHistory);

        tabHost = findViewById(android.R.id.tabhost);
        tabHost.setup();
        applyLanguageTabs();
    }

    private void applyLanguageTabs() {
        boolean es = LanguageText.isSpanish(prefs);
        tabHost.clearAllTabs();
        tabHost.addTab(tabHost.newTabSpec("rejected")
                .setIndicator(es ? "Rechazados" : "Rejected")
                .setContent(R.id.rejectedTab));
        tabHost.addTab(tabHost.newTabSpec("accepted")
                .setIndicator(es ? "Aceptados" : "Accepted")
                .setContent(R.id.acceptedTab));

        ((TextView) findViewById(R.id.historyTitle)).setText(es ? "Historial de ofertas" : "Offer History");
        ((TextView) findViewById(R.id.historyDescription)).setText(es
                ? "Las aceptaciones automáticas en vivo se registran cuando se pulsa Aceptar. Los rechazos se registran solo después de pulsar la segunda confirmación RECHAZAR OFERTA."
                : "Live automatic accepts are logged when Accept is pressed. Rejections are logged only after the second REJECT OFFER confirmation is pressed.");
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyLanguageTabs();
        rejectedHistory.setText(OfferHistory.rejected(prefs));
        acceptedHistory.setText(OfferHistory.accepted(prefs));
    }
}
