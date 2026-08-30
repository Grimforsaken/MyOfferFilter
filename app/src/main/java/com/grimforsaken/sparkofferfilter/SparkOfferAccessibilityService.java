package com.grimforsaken.sparkofferfilter;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.app.Notification;
import android.content.SharedPreferences;
import android.graphics.Path;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import java.text.DateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SparkOfferAccessibilityService extends AccessibilityService {
    private static final String SPARK_PACKAGE = "com.walmart.sparkdriver";
    private static final long EVENT_PAYLOAD_MAX_AGE_MS = 2500L;
    private static final long DUPLICATE_ACTION_WINDOW_MS = 15000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;
    private ToneGenerator toneGenerator;
    private String latestEventText = "";
    private long latestEventTextAt = 0L;
    private String lastActionKey = "";
    private long lastActionAt = 0L;

    private final Runnable retry40 = () -> evaluateCurrentOffer(null, "retry +40ms");
    private final Runnable retry120 = () -> evaluateCurrentOffer(null, "retry +120ms");
    private final Runnable retry300 = () -> evaluateCurrentOffer(null, "retry +300ms");

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        prefs = getSharedPreferences(Prefs.NAME, MODE_PRIVATE);
        try {
            toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85);
        } catch (RuntimeException ignored) {
            toneGenerator = null;
        }
        writeDecision("Service connected. Instant Scan is active: first complete Spark offer is evaluated immediately.");
        writeDiagnostic(Prefs.LAST_SCAN_STATUS,
                "Instant Scan ready; waiting for a Spark event or preloaded offer tree.");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (prefs == null) prefs = getSharedPreferences(Prefs.NAME, MODE_PRIVATE);
        if (!prefs.getBoolean(Prefs.MASTER_ENABLED, false)) return;

        CharSequence packageName = event.getPackageName();
        if (packageName == null || !SPARK_PACKAGE.contentEquals(packageName)) return;

        String eventPayload = collectEventPayload(event);
        if (looksLikeOfferPayload(eventPayload)) {
            latestEventText = eventPayload;
            latestEventTextAt = System.currentTimeMillis();
        }

        writeDiagnostic(Prefs.LAST_SPARK_EVENT,
                timestamp() + " — Spark event type " + event.getEventType()
                        + (eventPayload.isEmpty() ? "" : "; event payload captured"));

        AccessibilityNodeInfo source = event.getSource();
        evaluateCurrentOffer(source, "immediate event");

        handler.removeCallbacks(retry40);
        handler.removeCallbacks(retry120);
        handler.removeCallbacks(retry300);
        handler.postDelayed(retry40, 40L);
        handler.postDelayed(retry120, 120L);
        handler.postDelayed(retry300, 300L);
    }

    @Override
    public void onInterrupt() {
        handler.removeCallbacksAndMessages(null);
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

    private void evaluateCurrentOffer(AccessibilityNodeInfo eventSource, String scanSource) {
        if (prefs == null || !prefs.getBoolean(Prefs.MASTER_ENABLED, false)) return;

        List<AccessibilityNodeInfo> candidates = collectSparkCandidateRoots(eventSource);
        if (candidates.isEmpty()) {
            writeDiagnostic(Prefs.LAST_SCAN_STATUS,
                    timestamp() + " — Spark event received, but no Spark accessibility tree is attached yet ("
                            + scanSource + ").");
            return;
        }

        boolean sawReadableText = false;
        String bestStatus = "";
        String bestCapture = "";

        for (AccessibilityNodeInfo root : candidates) {
            String treeText = collectAllText(root);
            if (!treeText.trim().isEmpty()) {
                sawReadableText = true;
                if (treeText.length() > bestCapture.length()) bestCapture = treeText;
            }

            String currentText = mergeWithFreshEventPayload(treeText);
            if (OfferEvaluator.normalize(currentText).isEmpty()) continue;

            AccessibilityNodeInfo rejectNode = findDecisionControl(root, false);
            AccessibilityNodeInfo acceptNode = findDecisionControl(root, true);
            String controls = collectClickableLabels(root);

            OfferEvaluator.Result result = evaluateRules(currentText);
            String controlStatus = "Accept=" + (acceptNode != null ? controlVisibility(acceptNode) : "missing")
                    + ", Reject/Decline=" + (rejectNode != null ? controlVisibility(rejectNode) : "missing")
                    + ". Controls: " + controls;

            if (!result.ready) {
                bestStatus = timestamp() + " — " + scanSource + ": " + result.reason + " " + controlStatus;
                continue;
            }

            String offerKey = stableOfferKey(result);
            long now = System.currentTimeMillis();
            String details = formatOffer(result);
            boolean dryRun = prefs.getBoolean(Prefs.DRY_RUN, true);

            if (result.shouldReject) {
                if (dryRun) {
                    writeDecision("TEST MODE — would REJECT immediately. " + details
                            + " Reason: " + result.reason);
                    writeDiagnostic(Prefs.LAST_SCAN_STATUS,
                            timestamp() + " — " + scanSource + ". " + controlStatus);
                    writeDiagnostic(Prefs.LAST_CAPTURE, truncate(currentText, 3500));
                    return;
                }
                if (isDuplicateAction(offerKey, now)) return;

                if (rejectNode == null) {
                    bestStatus = timestamp() + " — REJECT decision already known, waiting only for Spark to attach "
                            + "a Reject/Decline control. " + controlStatus;
                    continue;
                }

                if (clickControl(rejectNode)) {
                    recordAction(offerKey, now);
                    clearFreshEventPayload();
                    playDecisionChime(false);
                    writeDecision("REJECTED immediately. " + details + " Reason: " + result.reason);
                } else {
                    bestStatus = timestamp() + " — REJECT decision known; detected control could not yet be activated. "
                            + controlStatus;
                    continue;
                }
                writeDiagnostic(Prefs.LAST_SCAN_STATUS,
                        timestamp() + " — " + scanSource + ". " + controlStatus);
                writeDiagnostic(Prefs.LAST_CAPTURE, truncate(currentText, 3500));
                return;
            }

            if (result.shouldAccept) {
                if (dryRun) {
                    writeDecision("TEST MODE — would ACCEPT immediately. " + details
                            + " Reason: " + result.reason);
                    writeDiagnostic(Prefs.LAST_SCAN_STATUS,
                            timestamp() + " — " + scanSource + ". " + controlStatus);
                    writeDiagnostic(Prefs.LAST_CAPTURE, truncate(currentText, 3500));
                    return;
                }
                if (isDuplicateAction(offerKey, now)) return;

                if (acceptNode == null) {
                    bestStatus = timestamp() + " — ACCEPT decision already known, waiting only for Spark to attach "
                            + "an Accept control. " + controlStatus;
                    continue;
                }

                if (clickControl(acceptNode)) {
                    recordAction(offerKey, now);
                    clearFreshEventPayload();
                    playDecisionChime(true);
                    writeDecision("ACCEPTED immediately. " + details + " Reason: " + result.reason);
                } else {
                    bestStatus = timestamp() + " — ACCEPT decision known; detected control could not yet be activated. "
                            + controlStatus;
                    continue;
                }
                writeDiagnostic(Prefs.LAST_SCAN_STATUS,
                        timestamp() + " — " + scanSource + ". " + controlStatus);
                writeDiagnostic(Prefs.LAST_CAPTURE, truncate(currentText, 3500));
                return;
            }

            writeDecision("LEFT FOR MANUAL REVIEW. " + details + " " + result.reason);
            writeDiagnostic(Prefs.LAST_SCAN_STATUS,
                    timestamp() + " — " + scanSource + ". " + controlStatus);
            writeDiagnostic(Prefs.LAST_CAPTURE, truncate(currentText, 3500));
            return;
        }

        if (!bestCapture.isEmpty()) {
            writeDiagnostic(Prefs.LAST_CAPTURE, truncate(mergeWithFreshEventPayload(bestCapture), 3500));
        }
        if (!bestStatus.isEmpty()) {
            writeDiagnostic(Prefs.LAST_SCAN_STATUS, bestStatus);
        } else if (!sawReadableText) {
            writeDiagnostic(Prefs.LAST_SCAN_STATUS,
                    timestamp() + " — Spark accessibility tree exists, but no readable offer text is exposed yet ("
                            + scanSource + ").");
        }
    }

    private OfferEvaluator.Result evaluateRules(String currentText) {
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

        return OfferEvaluator.evaluate(
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
    }

    private List<AccessibilityNodeInfo> collectSparkCandidateRoots(AccessibilityNodeInfo eventSource) {
        List<AccessibilityNodeInfo> roots = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        addCandidate(roots, seen, eventSource);
        addCandidate(roots, seen, getRootInActiveWindow());

        try {
            List<AccessibilityWindowInfo> windows = getWindows();
            if (windows != null) {
                for (AccessibilityWindowInfo window : windows) {
                    if (window == null) continue;
                    addCandidate(roots, seen, window.getRoot());
                }
            }
        } catch (RuntimeException ignored) {
        }

        return roots;
    }

    private void addCandidate(List<AccessibilityNodeInfo> roots, Set<String> seen,
                              AccessibilityNodeInfo node) {
        if (node == null || node.getPackageName() == null
                || !SPARK_PACKAGE.contentEquals(node.getPackageName())) return;

        String key = node.getWindowId() + ":" + node.hashCode();
        if (seen.add(key)) roots.add(node);
    }

    private String mergeWithFreshEventPayload(String treeText) {
        long age = System.currentTimeMillis() - latestEventTextAt;
        if (!latestEventText.isEmpty() && age >= 0 && age <= EVENT_PAYLOAD_MAX_AGE_MS) {
            return latestEventText + "\n" + (treeText == null ? "" : treeText);
        }
        return treeText == null ? "" : treeText;
    }

    private void clearFreshEventPayload() {
        latestEventText = "";
        latestEventTextAt = 0L;
    }

    private String collectEventPayload(AccessibilityEvent event) {
        StringBuilder out = new StringBuilder(1024);
        if (event == null) return "";

        for (CharSequence text : event.getText()) append(out, text);
        append(out, event.getContentDescription());
        append(out, event.getBeforeText());

        Object parcelable = event.getParcelableData();
        if (parcelable instanceof Notification) {
            Notification notification = (Notification) parcelable;
            Bundle extras = notification.extras;
            if (extras != null) {
                append(out, extras.getCharSequence(Notification.EXTRA_TITLE));
                append(out, extras.getCharSequence(Notification.EXTRA_TEXT));
                append(out, extras.getCharSequence(Notification.EXTRA_BIG_TEXT));
                append(out, extras.getCharSequence(Notification.EXTRA_SUB_TEXT));
                append(out, extras.getCharSequence(Notification.EXTRA_INFO_TEXT));
            }
        }

        return out.toString();
    }

    private boolean looksLikeOfferPayload(String text) {
        if (text == null || text.trim().isEmpty()) return false;
        String normalized = OfferEvaluator.normalize(text);
        return text.contains("$")
                || normalized.contains("MILE")
                || normalized.contains("SAND SPRINGS")
                || normalized.contains("SAPULPA")
                || normalized.contains("SHOPPING")
                || normalized.contains("SHIPPING")
                || normalized.contains("OFFER");
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
        toneGenerator.startTone(
                accepted ? ToneGenerator.TONE_PROP_ACK : ToneGenerator.TONE_PROP_NACK,
                accepted ? 260 : 340);
    }

    private boolean clickControl(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo clickable = clickableNodeOrParent(node);
        if (clickable != null && clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            return true;
        }

        if (!node.isVisibleToUser()) return false;

        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        if (bounds.isEmpty()) return false;
        Path path = new Path();
        path.moveTo(bounds.exactCenterX(), bounds.exactCenterY());
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(path, 0, 60))
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
        if (root == null) return null;
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
        queue.add(root);
        AccessibilityNodeInfo fallback = null;

        while (!queue.isEmpty()) {
            AccessibilityNodeInfo node = queue.removeFirst();
            String label = nodeLabel(node).trim().toLowerCase(Locale.US);

            if (accept) {
                if (label.equals("accept") || label.equals("accept offer") || label.equals("accept trip")) {
                    return node;
                }
                if (fallback == null && label.contains("accept")
                        && !label.contains("accepted") && !label.contains("acceptance")) {
                    fallback = node;
                }
            } else {
                if (label.equals("reject") || label.equals("decline")
                        || label.equals("reject offer") || label.equals("decline offer")
                        || label.equals("reject trip") || label.equals("decline trip")) {
                    return node;
                }
                if (fallback == null && (label.contains("reject") || label.contains("decline"))
                        && !label.contains("rejected") && !label.contains("declined")) {
                    fallback = node;
                }
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

    private String collectAllText(AccessibilityNodeInfo root) {
        if (root == null) return "";
        StringBuilder out = new StringBuilder(6144);
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
        queue.add(root);

        while (!queue.isEmpty()) {
            AccessibilityNodeInfo node = queue.removeFirst();
            CharSequence text = node.getText();
            CharSequence description = node.getContentDescription();
            append(out, text);
            if (description != null && (text == null || !description.toString().contentEquals(text))) {
                append(out, description);
            }

            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) queue.add(child);
            }
        }
        return out.toString();
    }

    private String collectClickableLabels(AccessibilityNodeInfo root) {
        if (root == null) return "(none exposed)";
        StringBuilder out = new StringBuilder();
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
        queue.add(root);
        int count = 0;

        while (!queue.isEmpty() && count < 40) {
            AccessibilityNodeInfo node = queue.removeFirst();
            if (node.isClickable() || node.isLongClickable()) {
                String label = nodeLabel(node).trim().replaceAll("\\s+", " ");
                if (label.isEmpty() && node.getViewIdResourceName() != null) {
                    label = node.getViewIdResourceName();
                }
                if (!label.isEmpty()) {
                    if (out.length() > 0) out.append(" | ");
                    if (!node.isVisibleToUser()) out.append("[preloaded] ");
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

    private String controlVisibility(AccessibilityNodeInfo node) {
        if (node == null) return "missing";
        return node.isVisibleToUser() ? "visible" : "preloaded";
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
        return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM)
                .format(new Date());
    }

    private void writeDiagnostic(String key, String message) {
        if (prefs != null) prefs.edit().putString(key, message).apply();
    }

    private void writeDecision(String message) {
        if (prefs != null) {
            prefs.edit().putString(Prefs.LAST_DECISION, timestamp() + "\n" + message).apply();
        }
    }
}
