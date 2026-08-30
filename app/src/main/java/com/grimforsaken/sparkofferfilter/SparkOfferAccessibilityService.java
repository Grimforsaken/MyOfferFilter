package com.grimforsaken.sparkofferfilter;

import android.accessibilityservice.AccessibilityService;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.text.DateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Locale;

public class SparkOfferAccessibilityService extends AccessibilityService {
    private static final String SPARK_PACKAGE = "com.walmart.sparkdriver";
    private static final long STABILITY_DELAY_MS = 1200L;
    private static final long DUPLICATE_ACTION_WINDOW_MS = 30000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;
    private ToneGenerator toneGenerator;
    private String pendingSnapshot = "";
    private String lastActionKey = "";
    private long lastActionAt = 0L;

    private final Runnable evaluateStableOffer = this::evaluateCurrentOffer;

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        prefs = getSharedPreferences(Prefs.NAME, MODE_PRIVATE);
        try {
            toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85);
        } catch (RuntimeException ignored) {
            toneGenerator = null;
        }
        writeStatus("Service connected. Waiting for a stable Spark offer screen.");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (prefs == null) prefs = getSharedPreferences(Prefs.NAME, MODE_PRIVATE);
        if (!prefs.getBoolean(Prefs.MASTER_ENABLED, false)) return;

        CharSequence packageName = event.getPackageName();
        if (packageName == null || !SPARK_PACKAGE.contentEquals(packageName)) return;

        String snapshot = collectVisibleText(getRootInActiveWindow());
        if (snapshot.isEmpty()) return;

        pendingSnapshot = OfferEvaluator.normalize(snapshot);
        handler.removeCallbacks(evaluateStableOffer);
        handler.postDelayed(evaluateStableOffer, STABILITY_DELAY_MS);
    }

    @Override
    public void onInterrupt() {
        handler.removeCallbacks(evaluateStableOffer);
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (toneGenerator != null) {
            toneGenerator.release();
            toneGenerator = null;
        }
        super.onDestroy();
    }

    private void evaluateCurrentOffer() {
        if (!prefs.getBoolean(Prefs.MASTER_ENABLED, false)) return;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null || root.getPackageName() == null
                || !SPARK_PACKAGE.contentEquals(root.getPackageName())) return;

        String currentText = collectVisibleText(root);
        String normalizedCurrent = OfferEvaluator.normalize(currentText);
        if (normalizedCurrent.isEmpty() || !normalizedCurrent.equals(pendingSnapshot)) return;

        AccessibilityNodeInfo rejectNode = findDecisionControl(root, false);
        if (rejectNode == null) return; // conservative offer-screen guard

        boolean rejectNoShopping = prefs.getBoolean(Prefs.REJECT_NO_SHOPPING, false);
        boolean rejectLowRate = prefs.getBoolean(Prefs.REJECT_LOW_RATE, true);
        double rejectThreshold = prefs.getFloat(Prefs.THRESHOLD, 1.25f);

        boolean autoAcceptEnabled = prefs.getBoolean(Prefs.AUTO_ACCEPT_ENABLED, false);
        boolean acceptMinPayEnabled = prefs.getBoolean(Prefs.ACCEPT_MIN_PAY_ENABLED, false);
        double acceptMinPay = prefs.getFloat(Prefs.ACCEPT_MIN_PAY, 20.00f);
        boolean acceptMinRateEnabled = prefs.getBoolean(Prefs.ACCEPT_MIN_RATE_ENABLED, false);
        double acceptMinRate = prefs.getFloat(Prefs.ACCEPT_MIN_RATE, 1.25f);
        boolean acceptMaxMilesEnabled = prefs.getBoolean(Prefs.ACCEPT_MAX_MILES_ENABLED, false);
        double acceptMaxMiles = prefs.getFloat(Prefs.ACCEPT_MAX_MILES, 10.0f);
        boolean dryRun = prefs.getBoolean(Prefs.DRY_RUN, true);

        OfferEvaluator.Result result = OfferEvaluator.evaluate(
                currentText,
                rejectNoShopping,
                rejectLowRate,
                rejectThreshold,
                autoAcceptEnabled,
                acceptMinPayEnabled,
                acceptMinPay,
                acceptMinRateEnabled,
                acceptMinRate,
                acceptMaxMilesEnabled,
                acceptMaxMiles);

        if (!result.ready) return;

        String offerKey = Integer.toHexString(normalizedCurrent.hashCode())
                + ":" + result.pay + ":" + result.miles;
        long now = System.currentTimeMillis();
        String details = formatOffer(result);

        if (result.shouldReject) {
            if (dryRun) {
                writeDecision("TEST MODE — would REJECT. " + details + " Reason: " + result.reason);
                return;
            }
            if (isDuplicateAction(offerKey, now)) return;

            if (clickControl(rejectNode)) {
                recordAction(offerKey, now);
                playDecisionChime(false);
                writeDecision("REJECTED. " + details + " Reason: " + result.reason);
            } else {
                writeDecision("Wanted to reject but could not click the visible Reject/Decline control. "
                        + details + " Reason: " + result.reason);
            }
            return;
        }

        if (result.shouldAccept) {
            AccessibilityNodeInfo acceptNode = findDecisionControl(root, true);
            if (dryRun) {
                writeDecision("TEST MODE — would ACCEPT. " + details + " Reason: " + result.reason);
                return;
            }
            if (isDuplicateAction(offerKey, now)) return;
            if (acceptNode == null) {
                writeDecision("Wanted to accept but no visible Accept control was found. "
                        + details + " Reason: " + result.reason);
                return;
            }

            if (clickControl(acceptNode)) {
                recordAction(offerKey, now);
                playDecisionChime(true);
                writeDecision("ACCEPTED. " + details + " Reason: " + result.reason);
            } else {
                writeDecision("Wanted to accept but could not click the visible Accept control. "
                        + details + " Reason: " + result.reason);
            }
            return;
        }

        writeDecision("LEFT FOR MANUAL REVIEW. " + details + " " + result.reason);
    }

    private boolean isDuplicateAction(String offerKey, long now) {
        return offerKey.equals(lastActionKey) && now - lastActionAt < DUPLICATE_ACTION_WINDOW_MS;
    }

    private void recordAction(String offerKey, long now) {
        lastActionKey = offerKey;
        lastActionAt = now;
    }

    private void playDecisionChime(boolean accepted) {
        if (!prefs.getBoolean(Prefs.DECISION_CHIMES, true) || toneGenerator == null) return;
        toneGenerator.startTone(accepted ? ToneGenerator.TONE_PROP_ACK : ToneGenerator.TONE_PROP_NACK,
                accepted ? 260 : 340);
    }

    private boolean clickControl(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo clickable = clickableNodeOrParent(node);
        return clickable != null && clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK);
    }

    private String formatOffer(OfferEvaluator.Result result) {
        return String.format(Locale.US, "$%.2f, %.1f mi, $%.2f/mi.%s%s",
                result.pay,
                result.miles,
                result.dollarsPerMile,
                result.hasAllowedCity ? " Allowed city found." : "",
                result.hasShopping ? " Shopping shown." : "");
    }

    private AccessibilityNodeInfo findDecisionControl(AccessibilityNodeInfo root, boolean accept) {
        String verb = accept ? "accept" : "reject";
        String alternate = accept ? "accept offer" : "decline";
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
        queue.add(root);
        AccessibilityNodeInfo fallback = null;

        while (!queue.isEmpty()) {
            AccessibilityNodeInfo node = queue.removeFirst();
            String label = nodeLabel(node).trim().toLowerCase(Locale.US);

            if (accept) {
                if (label.equals("accept") || label.equals("accept offer")) return node;
                if (fallback == null && label.contains(verb) && !label.contains("accepted")) fallback = node;
            } else {
                if (label.equals("reject") || label.equals("decline")
                        || label.equals("reject offer") || label.equals("decline offer")) return node;
                if (fallback == null && (label.contains(verb) || label.contains(alternate))
                        && !label.contains("rejected") && !label.contains("declined")) fallback = node;
            }

            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) queue.add(child);
            }
        }
        return fallback;
    }

    private AccessibilityNodeInfo clickableNodeOrParent(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo current = node;
        for (int i = 0; current != null && i < 5; i++) {
            if (current.isClickable() && current.isEnabled()) return current;
            current = current.getParent();
        }
        return null;
    }

    private String collectVisibleText(AccessibilityNodeInfo root) {
        if (root == null) return "";
        StringBuilder out = new StringBuilder(2048);
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
        queue.add(root);

        while (!queue.isEmpty()) {
            AccessibilityNodeInfo node = queue.removeFirst();
            if (node.isVisibleToUser()) {
                CharSequence text = node.getText();
                CharSequence description = node.getContentDescription();
                append(out, text);
                if (description != null && (text == null || !description.toString().contentEquals(text))) {
                    append(out, description);
                }
            }
            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) queue.add(child);
            }
        }
        return out.toString();
    }

    private void append(StringBuilder out, CharSequence value) {
        if (value == null) return;
        String s = value.toString().trim();
        if (!s.isEmpty()) out.append(s).append('\n');
    }

    private String nodeLabel(AccessibilityNodeInfo node) {
        StringBuilder value = new StringBuilder();
        if (node.getText() != null) value.append(node.getText()).append(' ');
        if (node.getContentDescription() != null) value.append(node.getContentDescription());
        return value.toString();
    }

    private void writeStatus(String message) {
        writeDecision(message);
    }

    private void writeDecision(String message) {
        String timestamp = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM)
                .format(new Date());
        prefs.edit().putString(Prefs.LAST_DECISION, timestamp + "\n" + message).apply();
    }
}
