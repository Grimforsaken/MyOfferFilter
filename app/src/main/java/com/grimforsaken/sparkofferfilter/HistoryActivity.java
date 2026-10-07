package com.grimforsaken.sparkofferfilter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TabHost;
import android.widget.TextView;

public class HistoryActivity extends Activity {
    private SharedPreferences prefs;
    private TextView rejectedHistory;
    private TextView acceptedHistory;
    private TextView historyTitle;
    private TextView historyDescription;
    private Button clearHistoryButton;
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
        clearHistoryButton = findViewById(R.id.clearHistoryButton);
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
        if (clearHistoryButton != null) {
            clearHistoryButton.setOnClickListener(v -> confirmClearHistory());
        }
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
                    ? "Las aceptaciones automáticas en vivo se registran cuando se pulsa Aceptar. Los rechazos se registran cuando Safe Driver selecciona Rechazar; la confirmación de Spark se rastrea por separado."
                    : "Live automatic accepts are logged when Accept is pressed. Rejections are logged when Safe Driver selects Reject; Spark confirmation is tracked separately.");
        }

        if (clearHistoryButton != null) {
            clearHistoryButton.setText(es ? "Borrar historial" : "Clear History");
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

    private void confirmClearHistory() {
        boolean es = LanguageText.isSpanish(prefs);
        new AlertDialog.Builder(this)
                .setTitle(es ? "Borrar historial" : "Clear history")
                .setMessage(es
                        ? "¿Borrar permanentemente todo el historial de pedidos aceptados y rechazados? Esto no borrará los datos de Comparación de ganancias."
                        : "Permanently clear all Accepted / Rejected Order History? This will not delete Order Earnings Comparison data.")
                .setNegativeButton(es ? "Cancelar" : "Cancel", null)
                .setPositiveButton(es ? "Borrar" : "Clear", (dialog, which) -> {
                    OfferHistory.clearAll(prefs);
                    rejectedHistory.setText(OfferHistory.rejected(prefs));
                    acceptedHistory.setText(OfferHistory.accepted(prefs));
                })
                .show();
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
