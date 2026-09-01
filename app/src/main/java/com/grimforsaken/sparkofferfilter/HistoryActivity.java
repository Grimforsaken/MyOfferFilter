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
    private TextView historyTitle;
    private TextView historyDescription;
    private TabHost tabHost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        prefs = getSharedPreferences(Prefs.NAME, MODE_PRIVATE);
        LanguageText.ensureDefault(prefs);

        rejectedHistory = findViewById(R.id.rejectedHistory);
        acceptedHistory = findViewById(R.id.acceptedHistory);
        historyTitle = findViewById(R.id.historyTitle);
        historyDescription = findViewById(R.id.historyDescription);
        tabHost = findViewById(android.R.id.tabhost);

        if (tabHost == null || rejectedHistory == null || acceptedHistory == null) {
            finish();
            return;
        }

        tabHost.setup();
        tabHost.addTab(tabHost.newTabSpec("rejected")
                .setIndicator("Rejected")
                .setContent(R.id.rejectedTab));
        tabHost.addTab(tabHost.newTabSpec("accepted")
                .setIndicator("Accepted")
                .setContent(R.id.acceptedTab));
        applyLanguageText();
    }

    private void applyLanguageText() {
        if (prefs == null || tabHost == null) return;
        boolean es = LanguageText.isSpanish(prefs);

        if (historyTitle != null) {
            historyTitle.setText(es ? "Historial de ofertas" : "Offer History");
        }
        if (historyDescription != null) {
            historyDescription.setText(es
                    ? "Las aceptaciones automáticas en vivo se registran cuando se pulsa Aceptar. Los rechazos se registran solo después de pulsar la segunda confirmación RECHAZAR OFERTA."
                    : "Live automatic accepts are logged when Accept is pressed. Rejections are logged only after the second REJECT OFFER confirmation is pressed.");
        }

        if (tabHost.getTabWidget() != null && tabHost.getTabWidget().getTabCount() >= 2) {
            TextView rejectedLabel = tabHost.getTabWidget().getChildTabViewAt(0)
                    .findViewById(android.R.id.title);
            TextView acceptedLabel = tabHost.getTabWidget().getChildTabViewAt(1)
                    .findViewById(android.R.id.title);
            if (rejectedLabel != null) rejectedLabel.setText(es ? "Rechazados" : "Rejected");
            if (acceptedLabel != null) acceptedLabel.setText(es ? "Aceptados" : "Accepted");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (prefs == null || rejectedHistory == null || acceptedHistory == null) return;
        applyLanguageText();
        rejectedHistory.setText(OfferHistory.rejected(prefs));
        acceptedHistory.setText(OfferHistory.accepted(prefs));
    }
}
