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
    private static final long REJECT_CONFIRMATION_WINDOW_MS = 6000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ActionSafetyGuard safetyGuard = new ActionSafetyGuard();
    private final OfferDecisionGuard decisionGuard = new OfferDecisionGuard();
    private SharedPreferences prefs;
    private ToneGenerator toneGenerator;
    private String latestEventText = "";
    private long latestEventTextAt = 0L;
    private String lastActionKey = "";
    private long lastActionAt = 0L;
    private boolean awaitingRejectConfirmation = false;
    private long rejectConfirmationDeadline = 0L;
    private String pendingRejectSummary = "";
    private String pendingRejectOfferKey = "";

    private final Runnable retry40 = () -> evaluateCurrentOffer(null, "retry +40ms");
    private final Runnable retry120 = () -> evaluateCurrentOffer(null, "retry +120ms");
    private final Runnable retry300 = () -> evaluateCurrentOffer(null, "retry +300ms");
    private final Runnable retry650 = () -> evaluateCurrentOffer(null, "retry +650ms");
    private final Runnable retry1400 = () -> evaluateCurrentOffer(null, "retry +1400ms");

    @Override public void onServiceConnected() {
        super.onServiceConnected();
        prefs = getSharedPreferences(Prefs.NAME, MODE_PRIVATE);
        clearPendingRejectConfirmation();
        safetyGuard.clear();
        decisionGuard.clear();
        refreshLocationPolicy();
        try { toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85); }
        catch (RuntimeException ignored) { toneGenerator = null; }
        writeDecision("Service connected. Accepted Locations whitelist is active. Accepted offers remain protected from later rejection.");
        writeDiagnostic(Prefs.LAST_SCAN_STATUS, "Instant Scan ready; waiting for a Spark event or preloaded offer tree.");
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (prefs == null) prefs = getSharedPreferences(Prefs.NAME, MODE_PRIVATE);
        if (!prefs.getBoolean(Prefs.MASTER_ENABLED, false)) return;
        CharSequence packageName = event.getPackageName();
        if (packageName == null || !SPARK_PACKAGE.contentEquals(packageName)) return;

        String eventPayload = collectEventPayload(event);
        if (looksLikeOfferPayload(eventPayload)) {
            latestEventText = eventPayload;
            latestEventTextAt = System.currentTimeMillis();
        }
        writeDiagnostic(Prefs.LAST_SPARK_EVENT, timestamp() + " — Spark event type " + event.getEventType()
                + (eventPayload.isEmpty() ? "" : "; event payload captured"));

        evaluateCurrentOffer(event.getSource(), "immediate event");
        handler.removeCallbacks(retry40);
        handler.removeCallbacks(retry120);
        handler.removeCallbacks(retry300);
        handler.removeCallbacks(retry650);
        handler.removeCallbacks(retry1400);
        handler.postDelayed(retry40, 40L);
        handler.postDelayed(retry120, 120L);
        handler.postDelayed(retry300, 300L);
        handler.postDelayed(retry650, 650L);
        handler.postDelayed(retry1400, 1400L);
    }

    @Override public void onInterrupt() {
        handler.removeCallbacksAndMessages(null);
        clearPendingRejectConfirmation();
    }

    @Override public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        clearPendingRejectConfirmation();
        safetyGuard.clear();
        decisionGuard.clear();
        if (toneGenerator != null) {
            toneGenerator.release();
            toneGenerator = null;
        }
        super.onDestroy();
    }

    private void evaluateCurrentOffer(AccessibilityNodeInfo eventSource, String scanSource) {
        if (prefs == null || !prefs.getBoolean(Prefs.MASTER_ENABLED, false)) return;
        refreshLocationPolicy();
        long now = System.currentTimeMillis();

        List<AccessibilityNodeInfo> candidates = collectSparkCandidateRoots(eventSource);
        if (candidates.isEmpty()) {
            writeDiagnostic(Prefs.LAST_SCAN_STATUS,
                    timestamp() + " — Spark event received, but no Spark accessibility tree is attached yet (" + scanSource + ").");
            return;
        }

        if (safetyGuard.isRejectLocked(now) && awaitingRejectConfirmation) {
            clearPendingRejectConfirmation();
        }

        if (awaitingRejectConfirmation) {
            if (now > rejectConfirmationDeadline) {
                clearPendingRejectConfirmation();
            } else {
                for (AccessibilityNodeInfo root : candidates) {
                    String confirmationText = collectAllText(root);
                    if (!isRejectConfirmationScreen(confirmationText)) continue;
                    if (safetyGuard.isRejectLocked(now)
                            || decisionGuard.isRejectProtected(pendingRejectOfferKey, now)) {
                        clearPendingRejectConfirmation();
                        writeDecision("REJECTION BLOCKED by accepted-offer safety protection.");
                        writeDiagnostic(Prefs.LAST_SCAN_STATUS, safetyGuard.isRejectLocked(now)
                                ? lockoutMessage(now)
                                : timestamp() + " — SAME-OFFER SAFETY: reject confirmation canceled because this offer is being accepted or was already accepted.");
                        return;
                    }
                    AccessibilityNodeInfo confirmRejectNode = findRejectConfirmationControl(root);
                    if (confirmRejectNode == null) {
                        writeDiagnostic(Prefs.LAST_SCAN_STATUS,
                                timestamp() + " — Reject confirmation dialog found; waiting for the second REJECT OFFER button.");
                        return;
                    }
                    if (clickControl(confirmRejectNode)) {
                        String summary = pendingRejectSummary;
                        String key = pendingRejectOfferKey;
                        clearPendingRejectConfirmation();
                        recordAction(key, now);
                        clearFreshEventPayload();
                        decisionGuard.clearRejectCandidate();
                        playDecisionChime(false);
                        OfferHistory.addRejected(prefs, timestamp() + "\n" + summary);
                        writeDecision("REJECTED and confirmed. " + summary);
                    }
                    return;
                }
                return;
            }
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
            String controlStatus = "Accept=" + (acceptNode != null ? controlVisibility(acceptNode) : "missing")
                    + ", Reject/Decline=" + (rejectNode != null ? controlVisibility(rejectNode) : "missing")
                    + ". Controls: " + controls;

            OfferLocationPolicy.Decision location = OfferLocationPolicy.evaluate(treeText);
            if (!location.identified) {
                bestStatus = timestamp() + " — " + scanSource + ": " + location.reason + " " + controlStatus;
                continue;
            }

            OfferEvaluator.Result result = evaluateRules(currentText);
            boolean estimatedTotalScreen = OfferEvaluator.normalize(currentText).contains("ESTIMATED TOTAL");

            if (!location.allowed) {
                Double rate = result.pay != null && result.miles != null && result.miles > 0.0
                        ? result.pay / result.miles : null;
                if (estimatedTotalScreen) {
                    result = OfferEvaluator.Result.ready(false, false, false, result.hasShopping,
                            result.pay, result.miles, rate,
                            "Estimated total screen: automatic rejection is disabled. "
                                    + location.location + " is not checked in Accepted Locations.");
                } else {
                    result = OfferEvaluator.Result.ready(true, false, false, result.hasShopping,
                            result.pay, result.miles, rate,
                            location.location + " is not checked in Accepted Locations");
                }
            } else if (!result.ready) {
                bestStatus = timestamp() + " — " + scanSource + ": " + result.reason
                        + " Location=" + location.location + ". " + controlStatus;
                continue;
            }

            String offerKey = stableOfferKey(result, location.location);
            now = System.currentTimeMillis();
            String details = formatOffer(result) + " Location: " + location.location + ".";
            boolean dryRun = prefs.getBoolean(Prefs.DRY_RUN, true);

            if (result.shouldReject) {
                if (estimatedTotalScreen) {
                    decisionGuard.clearRejectCandidate();
                    writeDecision("REJECTION BLOCKED because this is the Estimated total screen. " + details);
                    writeDiagnostic(Prefs.LAST_SCAN_STATUS,
                            timestamp() + " — ESTIMATED TOTAL SAFETY: automatic rejection is disabled on this screen.");
                    return;
                }
                if (result.pay == null || result.miles == null || result.miles <= 0.0) {
                    bestStatus = timestamp() + " — REJECT decision known, but Safe Driver is waiting for complete pay/mileage identity before taking a reject action. " + controlStatus;
                    continue;
                }
                if (safetyGuard.isRejectLocked(now)) {
                    clearPendingRejectConfirmation();
                    decisionGuard.clearRejectCandidate();
                    writeDecision("REJECTION BLOCKED by 10-second post-accept safety lock. " + details
                            + " Reason would have been: " + result.reason);
                    writeDiagnostic(Prefs.LAST_SCAN_STATUS, lockoutMessage(now));
                    writeDiagnostic(Prefs.LAST_CAPTURE, truncate(currentText, 3500));
                    return;
                }
                if (decisionGuard.isRejectProtected(offerKey, now)) {
                    clearPendingRejectConfirmation();
                    decisionGuard.clearRejectCandidate();
                    writeDecision("REJECTION BLOCKED by one-decision-per-offer safety lock. " + details
                            + " Reason would have been: " + result.reason);
                    writeDiagnostic(Prefs.LAST_SCAN_STATUS,
                            timestamp() + " — SAME-OFFER SAFETY: this offer is being accepted or was already accepted, so Reject is disabled for this offer.");
                    writeDiagnostic(Prefs.LAST_CAPTURE, truncate(currentText, 3500));
                    return;
                }
                if (dryRun) {
                    writeDecision("TEST MODE — would REJECT after safety verification. " + details + " Reason: " + result.reason);
                    writeDiagnostic(Prefs.LAST_SCAN_STATUS, timestamp() + " — " + scanSource + ". " + controlStatus);
                    continue;
                }
                if (isDuplicateAction(offerKey, now)) continue;
                if (!decisionGuard.isRejectStable(offerKey, result.reason, now)) {
                    long remaining = decisionGuard.rejectStabilityRemainingMs(now);
                    bestStatus = timestamp() + " — REJECT candidate detected; waiting " + remaining
                            + " ms for a second consistent scan before acting. " + controlStatus;
                    continue;
                }
                if (rejectNode == null) {
                    bestStatus = timestamp() + " — REJECT decision verified, waiting for Reject/Decline control. " + controlStatus;
                    continue;
                }
                if (clickControl(rejectNode)) {
                    awaitingRejectConfirmation = true;
                    rejectConfirmationDeadline = now + REJECT_CONFIRMATION_WINDOW_MS;
                    pendingRejectOfferKey = offerKey;
                    pendingRejectSummary = details + " Reason: " + result.reason;
                    writeDecision("REJECT selected after safety verification; waiting for Spark confirmation. " + pendingRejectSummary);
                }
                writeDiagnostic(Prefs.LAST_CAPTURE, truncate(currentText, 3500));
                return;
            }

            if (result.shouldAccept) {
                if (!location.allowed) {
                    writeDecision("LEFT FOR MANUAL REVIEW. Location is not checked in Accepted Locations. " + details);
                    return;
                }
                decisionGuard.noteAcceptIntent(offerKey, now);
                if (awaitingRejectConfirmation && offerKey.equals(pendingRejectOfferKey)) {
                    clearPendingRejectConfirmation();
                }
                if (dryRun) {
                    writeDecision("TEST MODE — would ACCEPT immediately. " + details + " Reason: " + result.reason);
                    writeDiagnostic(Prefs.LAST_SCAN_STATUS, timestamp() + " — " + scanSource + ". " + controlStatus);
                    return;
                }
                if (isDuplicateAction(offerKey, now)) return;
                if (acceptNode == null) {
                    bestStatus = timestamp() + " — ACCEPT decision known; Reject is locked for this offer while waiting for the Accept control. " + controlStatus;
                    continue;
                }
                if (clickControl(acceptNode)) {
                    recordAction(offerKey, now);
                    clearPendingRejectConfirmation();
                    clearFreshEventPayload();
                    decisionGuard.noteAccepted(offerKey, now);
                    safetyGuard.onAccepted(now);
                    playDecisionChime(true);
                    String city = OfferCityDetector.detect(treeText);
                    if ("Unknown".equals(city) && !"Sam's Club".equals(location.location)) city = location.location;
                    String cityLabel = LanguageText.isSpanish(prefs) ? "Ciudad: " : "City: ";
                    String summary = cityLabel + city + "\n" + formatOffer(result) + " Reason: " + result.reason;
                    OfferHistory.addAccepted(prefs, timestamp() + "\n" + summary);
                    writeDecision("ACCEPTED immediately. Rejections locked for 10 seconds and this accepted offer is protected from later rejection. " + summary);
                    writeDiagnostic(Prefs.LAST_SCAN_STATUS, lockoutMessage(now));
                    writeDiagnostic(Prefs.LAST_CAPTURE, truncate(currentText, 3500));
                    return;
                }
                bestStatus = timestamp() + " — ACCEPT decision known; Reject is locked for this offer, but the Accept control could not yet be activated. " + controlStatus;
                continue;
            }

            writeDecision("LEFT FOR MANUAL REVIEW. " + details + " " + result.reason);
            writeDiagnostic(Prefs.LAST_SCAN_STATUS, timestamp() + " — " + scanSource + ". " + controlStatus);
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
                    timestamp() + " — Spark accessibility tree exists, but no readable offer text is exposed yet (" + scanSource + ").");
        }
    }

    private String lockoutMessage(long now) {
        long ms = safetyGuard.remainingRejectLockoutMs(now);
        return timestamp() + " — POST-ACCEPT SAFETY: all rejection actions blocked for "
                + String.format(Locale.US, "%.1f", ms / 1000.0) + " more seconds.";
    }

    private void refreshLocationPolicy() {
        CityPolicy.configure(
                prefs.getBoolean(Prefs.ALLOW_TULSA, false),
                prefs.getBoolean(Prefs.ALLOW_GLENPOOL, false),
                prefs.getBoolean(Prefs.ALLOW_JENKS, false),
                prefs.getBoolean(Prefs.ALLOW_SAMS_CLUB, false),
                prefs.getBoolean(Prefs.ALLOW_SAPULPA, true),
                prefs.getBoolean(Prefs.ALLOW_SAND_SPRINGS, true));
    }

    private OfferEvaluator.Result evaluateRules(String currentText) {
        return OfferEvaluator.evaluate(currentText,
                prefs.getBoolean(Prefs.REJECT_NO_SHOPPING, false),
                prefs.getBoolean(Prefs.REJECT_LOW_RATE, true),
                prefs.getFloat(Prefs.THRESHOLD, 1.25f),
                prefs.getBoolean(Prefs.REJECT_MIN_PAY_ENABLED, true),
                prefs.getFloat(Prefs.REJECT_MIN_PAY, 15.00f),
                prefs.getBoolean(Prefs.REJECT_MAX_MILES_ENABLED, false),
                prefs.getFloat(Prefs.REJECT_MAX_MILES, 20.0f),
                prefs.getBoolean(Prefs.AUTO_ACCEPT_ENABLED, false),
                prefs.getBoolean(Prefs.ACCEPT_MIN_PAY_ENABLED, false),
                prefs.getFloat(Prefs.ACCEPT_MIN_PAY, 20.00f),
                prefs.getBoolean(Prefs.ACCEPT_MIN_RATE_ENABLED, false),
                prefs.getFloat(Prefs.ACCEPT_MIN_RATE, 1.25f),
                prefs.getBoolean(Prefs.ACCEPT_MAX_MILES_ENABLED, false),
                prefs.getFloat(Prefs.ACCEPT_MAX_MILES, 10.0f),
                prefs.getBoolean(Prefs.ACCEPT_SHOPPING_ENABLED, false),
                prefs.getBoolean(Prefs.ACCEPT_NO_SHOPPING_ENABLED, false));
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
                    if (window != null) addCandidate(roots, seen, window.getRoot());
                }
            }
        } catch (RuntimeException ignored) {}
        return roots;
    }

    private void addCandidate(List<AccessibilityNodeInfo> roots, Set<String> seen, AccessibilityNodeInfo node) {
        if (node == null || node.getPackageName() == null || !SPARK_PACKAGE.contentEquals(node.getPackageName())) return;
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

    private void clearPendingRejectConfirmation() {
        awaitingRejectConfirmation = false;
        rejectConfirmationDeadline = 0L;
        pendingRejectSummary = "";
        pendingRejectOfferKey = "";
    }

    private boolean isRejectConfirmationScreen(String text) {
        String n = OfferEvaluator.normalize(text == null ? "" : text);
        return n.contains("ARE YOU SURE YOU WANT TO REJECT THIS OFFER")
                || (n.contains("KEEP OFFER") && n.contains("REJECT OFFER"));
    }

    private String collectEventPayload(AccessibilityEvent event) {
        StringBuilder out = new StringBuilder(1024);
        if (event == null) return "";
        for (CharSequence text : event.getText()) append(out, text);
        append(out, event.getContentDescription());
        append(out, event.getBeforeText());
        Object parcelable = event.getParcelableData();
        if (parcelable instanceof Notification) {
            Bundle extras = ((Notification) parcelable).extras;
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
        String n = OfferEvaluator.normalize(text);
        return text.contains("$") || n.contains("MILE") || n.contains("TULSA")
                || n.contains("GLENPOOL") || n.contains("JENKS")
                || n.contains("SAM'S CLUB") || n.contains("SAMS CLUB")
                || n.contains("SAND SPRINGS") || n.contains("SAPULPA")
                || n.contains("SHOPPING") || n.contains("OFFER");
    }

    private String stableOfferKey(OfferEvaluator.Result r, String location) {
        String pay = r.pay == null ? "?" : String.format(Locale.US, "%.2f", r.pay);
        String miles = r.miles == null ? "?" : String.format(Locale.US, "%.2f", r.miles);
        return pay + ":" + miles + ":" + r.hasShopping + ":" + (location == null ? "?" : location);
    }

    private boolean isDuplicateAction(String key, long now) {
        return key.equals(lastActionKey) && now - lastActionAt < DUPLICATE_ACTION_WINDOW_MS;
    }

    private void recordAction(String key, long now) {
        lastActionKey = key == null ? "" : key;
        lastActionAt = now;
    }

    private void playDecisionChime(boolean accepted) {
        if (prefs.getBoolean(Prefs.DECISION_CHIMES, true) && toneGenerator != null) {
            toneGenerator.startTone(accepted ? ToneGenerator.TONE_PROP_ACK : ToneGenerator.TONE_PROP_NACK,
                    accepted ? 260 : 340);
        }
    }

    private boolean clickControl(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo clickable = clickableNodeOrParent(node);
        if (clickable != null && clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true;
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

    private String formatOffer(OfferEvaluator.Result r) {
        List<String> parts = new ArrayList<>();
        if (r.pay != null) parts.add(String.format(Locale.US, "$%.2f", r.pay));
        if (r.miles != null) parts.add(String.format(Locale.US, "%.1f mi", r.miles));
        if (r.dollarsPerMile != null) parts.add(String.format(Locale.US, "$%.2f/mi", r.dollarsPerMile));
        if (parts.isEmpty()) parts.add("Offer details still loading");
        StringBuilder out = new StringBuilder(String.join(", ", parts)).append('.');
        if (r.hasShopping) out.append(" Shopping shown.");
        return out.toString();
    }

    private AccessibilityNodeInfo findDecisionControl(AccessibilityNodeInfo root, boolean accept) {
        if (root == null) return null;
        ArrayDeque<AccessibilityNodeInfo> q = new ArrayDeque<>();
        q.add(root);
        AccessibilityNodeInfo fallback = null;
        while (!q.isEmpty()) {
            AccessibilityNodeInfo node = q.removeFirst();
            String label = nodeLabel(node).trim().toLowerCase(Locale.US);
            if (accept) {
                if (label.equals("accept") || label.equals("accept offer") || label.equals("accept trip")) return node;
                if (fallback == null && label.contains("accept")
                        && !label.contains("accepted") && !label.contains("acceptance")) fallback = node;
            } else {
                if (label.equals("reject") || label.equals("decline") || label.equals("reject offer")
                        || label.equals("decline offer") || label.equals("reject trip") || label.equals("decline trip")) return node;
                if (fallback == null && (label.contains("reject") || label.contains("decline"))
                        && !label.contains("rejected") && !label.contains("declined")) fallback = node;
            }
            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo c = node.getChild(i);
                if (c != null) q.add(c);
            }
        }
        return fallback;
    }

    private AccessibilityNodeInfo findRejectConfirmationControl(AccessibilityNodeInfo root) {
        if (root == null) return null;
        ArrayDeque<AccessibilityNodeInfo> q = new ArrayDeque<>();
        q.add(root);
        AccessibilityNodeInfo fallback = null;
        while (!q.isEmpty()) {
            AccessibilityNodeInfo node = q.removeFirst();
            String label = nodeLabel(node).trim().toLowerCase(Locale.US).replaceAll("\\s+", " ");
            if (label.equals("reject offer")) return node;
            if (fallback == null && label.contains("reject offer")) fallback = node;
            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo c = node.getChild(i);
                if (c != null) q.add(c);
            }
        }
        return fallback;
    }

    private AccessibilityNodeInfo clickableNodeOrParent(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo c = node;
        for (int i = 0; c != null && i < 8; i++) {
            if (c.isClickable() && c.isEnabled()) return c;
            c = c.getParent();
        }
        return null;
    }

    private String collectAllText(AccessibilityNodeInfo root) {
        if (root == null) return "";
        StringBuilder out = new StringBuilder(6144);
        ArrayDeque<AccessibilityNodeInfo> q = new ArrayDeque<>();
        q.add(root);
        while (!q.isEmpty()) {
            AccessibilityNodeInfo n = q.removeFirst();
            CharSequence t = n.getText();
            CharSequence d = n.getContentDescription();
            append(out, t);
            if (d != null && (t == null || !d.toString().contentEquals(t))) append(out, d);
            for (int i = 0; i < n.getChildCount(); i++) {
                AccessibilityNodeInfo c = n.getChild(i);
                if (c != null) q.add(c);
            }
        }
        return out.toString();
    }

    private String collectClickableLabels(AccessibilityNodeInfo root) {
        if (root == null) return "(none exposed)";
        StringBuilder out = new StringBuilder();
        ArrayDeque<AccessibilityNodeInfo> q = new ArrayDeque<>();
        q.add(root);
        int count = 0;
        while (!q.isEmpty() && count < 40) {
            AccessibilityNodeInfo n = q.removeFirst();
            if (n.isClickable() || n.isLongClickable()) {
                String label = nodeLabel(n).trim().replaceAll("\\s+", " ");
                if (label.isEmpty() && n.getViewIdResourceName() != null) label = n.getViewIdResourceName();
                if (!label.isEmpty()) {
                    if (out.length() > 0) out.append(" | ");
                    if (!n.isVisibleToUser()) out.append("[preloaded] ");
                    out.append(truncate(label, 90));
                    count++;
                }
            }
            for (int i = 0; i < n.getChildCount(); i++) {
                AccessibilityNodeInfo c = n.getChild(i);
                if (c != null) q.add(c);
            }
        }
        return out.length() == 0 ? "(none exposed)" : out.toString();
    }

    private String controlVisibility(AccessibilityNodeInfo node) {
        return node == null ? "missing" : (node.isVisibleToUser() ? "visible" : "preloaded");
    }

    private void append(StringBuilder out, CharSequence value) {
        if (value == null) return;
        String s = value.toString().trim();
        if (!s.isEmpty()) out.append(s).append('\n');
    }

    private String nodeLabel(AccessibilityNodeInfo node) {
        StringBuilder v = new StringBuilder();
        if (node.getText() != null) v.append(node.getText()).append(' ');
        if (node.getContentDescription() != null) v.append(node.getContentDescription());
        return v.toString();
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
