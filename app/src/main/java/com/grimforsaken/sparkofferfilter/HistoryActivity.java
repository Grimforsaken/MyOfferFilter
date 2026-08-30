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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        prefs = getSharedPreferences(Prefs.NAME, MODE_PRIVATE);

        rejectedHistory = findViewById(R.id.rejectedHistory);
        acceptedHistory = findViewById(R.id.acceptedHistory);

        TabHost tabHost = findViewById(android.R.id.tabhost);
        tabHost.setup();
        tabHost.addTab(tabHost.newTabSpec("rejected")
                .setIndicator("Rejected")
                .setContent(R.id.rejectedTab));
        tabHost.addTab(tabHost.newTabSpec("accepted")
                .setIndicator("Accepted")
                .setContent(R.id.acceptedTab));
    }

    @Override
    protected void onResume() {
        super.onResume();
        rejectedHistory.setText(OfferHistory.rejected(prefs));
        acceptedHistory.setText(OfferHistory.accepted(prefs));
    }
}
