package com.grimforsaken.sparkofferfilter;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.SharedPreferences;
import android.graphics.Path;
import android.graphics.Rect;
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
    private static final long FIRST_SCAN_DELAY_MS = 350L;
    private static final long RETRY_SCAN_DELAY_MS = 900L;
    private static final long DUPLICATE_ACTION_WINDOW_MS = 15000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;
    private ToneGenerator toneGenerator;
    private boolean scanScheduled = false;
    private String lastActionKey = "";
    private long lastActionAt = 0L;

    private final Runnable scheduledScan = () -> {
        scanScheduled = false;
        evaluateCurrentOffer();
    };

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        prefs = getSharedPreferences(Prefs.NAME, MODE_PRIVATE);
        try {
            toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85);
        } catch (RuntimeException ignored) {
            toneGenerator = null;
        }
        writeDecision("Service connected. Open Spark and wait for an offer. Live diagnostics are enabled.");
        writeDiagnostic(Prefs.LAST_SCAN_STATUS, "Service connected; no Spark offer scanned yet.");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (prefs == null) prefs = getSharedPreferences(Prefs.NAME, MODE_PRIVATE);
        if (!prefs.getBoolean(Prefs.MASTER_ENABLED, false)) return;

        CharSequence packageName = event.getPackageName();
        if (packageName == null || !SPARK_PACKAGE.contentEquals(packageName)) return;

        writeDiagnostic(Prefs.LAST_SPARK_EVENT,
                timestamp() + " — Spark event type " + event.getEventType());
        scheduleScan(FIRST_SCAN_DELAY_MS);
    }

    @Override
    public void onInterrupt() {
        handler.removeCallbacks(scheduledScan);
        scanScheduled = false;
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

    private void scheduleScan(long delayMs) {
        if (scanScheduled) return;
        scanScheduled = true;
        handler.postDelayed(scheduledScan, delayMs);
    }

    private void evaluateCurrentOffer() {
        if (prefs == null || !prefs.getBoolean(Prefs.MASTER_ENABLED, false)) return;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            writeDiagnostic(Prefs.LAST_SCAN_STATUS, timestamp() + " — No active accessibility window.");
            return;
        }
        if (root.getPackageName() == null || !SPARK_PACKAGE.contentEquals(root.getPackageName())) {
            writeDiagnostic(Prefs.LAST_SCAN_STATUS, timestamp() + " — Active window is not Spark.");
            return;
        }

        String currentText = collectVisibleText(root);
        String normalizedCurrent = OfferEvaluator.normalize(currentText);
        String controls = collectClickableLabels(root);
        writeDiagnostic(Prefs.LAST_CAPTURE, truncate(currentText, 2400));

        if (normalizedCurrent.isEmpty()) {
            writeDiagnostic(Prefs.LAST_SCAN_STATUS,
                    timestamp() + " — Spark detected, but Accessibility exposed no readable text. Controls: " + controls);
            scheduleScan(RETRY_SCAN_DELAY_MS);
            return;
        }

        AccessibilityNodeInfo rejectNode = findDecisionControl(root, false);
        AccessibilityNodeInfo acceptNode = findDecisionControl(root, true);

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
        boolean acceptShoppingEnabled = prefs.getBoolean(Prefs.ACCEPT_SHOPPING_ENABLED, false);
        boolean acceptNoShippingEnabled = prefs.getBoolean(Prefs.ACCEPT_NO_SHIPPING_ENABLED, false);
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
                acceptMaxMiles,
                acceptShoppingEnabled,
                acceptNoShippingEnabled);

        String controlStatus = "Accept=" + (acceptNode != null ? "found" : "missing")
                + ", Reject/Decline=" + (rejectNode != null ? "found" : "missing")
                + ". Clickable labels: " + controls;

        if (!result.ready) {
            writeDiagnostic(Prefs.LAST_SCAN_STATUS,
                    timestamp() + " — Spark scanned. " + result.reason + " " + controlStatus);
            scheduleScan(RETRY_SCAN_DELAY_MS);
            return;
        }

        String offerKey = stableOfferKey(result);
        long now = System.currentTimeMillis();
        String details = formatOffer(result);

        if (result.shouldReject) {
            if (dryRun) {
                writeDecision("TEST MODE — would REJECT. " + details + " Reason: " + result.reason);
                writeDiagnostic(Prefs.LAST_SCAN_STATUS, timestamp() + " — " + controlStatus);
                return;
            }
            if (isDuplicateAction(offerKey, now)) return;
            if (rejectNode == null) {
                writeDecision("REJECT REQUIRED, but no Reject/Decline control was readable. "
                        + details + " Reason: " + result.reason);
                writeDiagnostic(Prefs.LAST_SCAN_STATUS, timestamp() + " — " + controlStatus);
                scheduleScan(RETRY_SCAN_DELAY_MS);
                return;
            }

            if (clickControl(rejectNode)) {
                recordAction(offerKey, now);
                playDecisionChime(false);
                writeDecision("REJECTED. " + details + " Reason: " + result.reason);
            } else {
                writeDecision("Wanted to REJECT but Android would not activate the detected control. "
                        + details + " Reason: " + result.reason);
            }
            writeDiagnostic(Prefs.LAST_SCAN_STATUS, timestamp() + " — " + controlStatus);
            return;
        }

        if (result.shouldAccept) {
            if (dryRun) {
                writeDecision("TEST MODE — would ACCEPT. " + details + " Reason: " + result.reason);
                writeDiagnostic(Prefs.LAST_SCAN_STATUS, timestamp() + " — " + controlStatus);
                return;
            }
            if (isDuplicateAction(offerKey, now)) return;
            if (acceptNode == null) {
                writeDecision("ACCEPT REQUIRED, but no Accept control was readable. "
                        + details + " Reason: " + result.reason);
                writeDiagnostic(Prefs.LAST_SCAN_STATUS, timestamp() + " — " + controlStatus);
                scheduleScan(RETRY_SCAN_DELAY_MS);
                return;
            }

            if (clickControl(acceptNode)) {
                recordAction(offerKey, now);
                playDecisionChime(true);
                writeDecision("ACCEPTED. " + details + " Reason: " + result.reason);
            } else {
                writeDecision("Wanted to ACCEPT but Android would not activate the detected control. "
                        + details + " Reason: " + result.reason);
            }
            writeDiagnostic(Prefs.LAST_SCAN_STATUS, timestamp() + " — " + controlStatus);
            return;
        }

        writeDecision("LEFT FOR MANUAL REVIEW. " + details + " " + result.reason);
        writeDiagnostic(Prefs.LAST_SCAN_STATUS, timestamp() + " — " + controlStatus);
    }

    private String stableOfferKey(OfferEvaluator.Result result) {
        return String.format(Locale.US, "%.2f:%.2f:%s:%s:%s",
                result.pay, result.miles,
                result.hasAllowedCity, result.hasShopping, result.hasShipping);
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
        if (clickable != null && clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            return true;
        }

        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        if (bounds.isEmpty()) return false;
        Path path = new Path();
        path.moveTo(bounds.exactCenterX(), bounds.exactCenterY());
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(path, 0, 80))
                .build();
        return dispatchGesture(gesture, null, null);
    }

    private String formatOffer(OfferEvaluator.Result result) {
        return String.format(Locale.US, "$%.2f, %.1f mi, $%.2f/mi.%s%s%s",
                result.pay,
                result.miles,
                result.dollarsPerMile,
                result.hasAllowedCity ? " Allowed city found." : "",
                result.hasShopping ? " Shopping shown." : "",
                result.hasShipping ? " Shipping shown." : "");
    }

    private AccessibilityNodeInfo findDecisionControl(AccessibilityNodeInfo root, boolean accept) {
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
        queue.add(root);
        AccessibilityNodeInfo fallback = null;

        while (!queue.isEmpty()) {
            AccessibilityNodeInfo node = queue.removeFirst();
            String label = nodeLabel(node).trim().toLowerCase(Locale.US);

            if (accept) {
                if (label.equals("accept") || label.equals("accept offer") || label.equals("accept trip")) return node;
                if (fallback == null && label.contains("accept")
                        && !label.contains("accepted") && !label.contains("acceptance")) fallback = node;
            } else {
                if (label.equals("reject") || label.equals("decline")
                        || label.equals("reject offer") || label.equals("decline offer")
                        || label.equals("reject trip") || label.equals("decline trip")) return node;
                if (fallback == null && (label.contains("reject") || label.contains("decline"))
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
        for (int i = 0; current != null && i < 8; i++) {
            if (current.isClickable() && current.isEnabled()) return current;
            current = current.getParent();
        }
        return null;
    }

    private String collectVisibleText(AccessibilityNodeInfo root) {
        if (root == null) return "";
        StringBuilder out = new StringBuilder(4096);
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
                if (node.getViewIdResourceName() != null) {
                    append(out, "[viewId=" + node.getViewIdResourceName() + "]");
                }
            }
            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) queue.add(child);
            }
        }
        return out.toString();
    }

    private String collectClickableLabels(AccessibilityNodeInfo root) {
        StringBuilder out = new StringBuilder();
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
        queue.add(root);
        int count = 0;
        while (!queue.isEmpty() && count < 30) {
            AccessibilityNodeInfo node = queue.removeFirst();
            if (node.isVisibleToUser() && (node.isClickable() || node.isLongClickable())) {
                String label = nodeLabel(node).trim().replaceAll("\\s+", " ");
                if (label.isEmpty() && node.getViewIdResourceName() != null) {
                    label = node.getViewIdResourceName();
                }
                if (!label.isEmpty()) {
                    if (out.length() > 0) out.append(" | ");
                    out.append(truncate(label, 90));
                    count++;
                }
            }
            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) queue.add(child);
            }
        }
        return out.length() == 0 ? "(none exposed)" : out.toString();
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

    private String truncate(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max) + "…";
    }

    private String timestamp() {
        return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(new Date());
    }

    private void writeDiagnostic(String key, String message) {
        if (prefs != null) prefs.edit().putString(key, message).apply();
    }

    private void writeDecision(String message) {
        if (prefs != null) prefs.edit().putString(Prefs.LAST_DECISION, timestamp() + "\n" + message).apply();
    }
}
