package com.grimforsaken.sparkofferfilter;

final class UnknownLocationGuard {
    static final long WAIT_MS = 2_000L;
    static final long MANUAL_REVIEW_LOCK_MS = 60_000L;

    private String pendingKey = "";
    private long firstUnknownAt = 0L;
    private String manualReviewKey = "";
    private long manualReviewUntil = 0L;

    boolean shouldLeaveForManualReview(String offerKey, long now) {
        String key = safe(offerKey);
        if (key.isEmpty()) key = "unknown-offer";
        if (isManualReviewLocked(key, now)) return true;

        if (!key.equals(pendingKey)) {
            pendingKey = key;
            firstUnknownAt = now;
            return false;
        }

        if (now - firstUnknownAt >= WAIT_MS) {
            manualReviewKey = key;
            manualReviewUntil = now + MANUAL_REVIEW_LOCK_MS;
            pendingKey = "";
            firstUnknownAt = 0L;
            return true;
        }
        return false;
    }

    long remainingMs(String offerKey, long now) {
        String key = safe(offerKey);
        if (!key.equals(pendingKey) || firstUnknownAt == 0L) return WAIT_MS;
        return Math.max(0L, WAIT_MS - (now - firstUnknownAt));
    }

    boolean isManualReviewLocked(String offerKey, long now) {
        String key = safe(offerKey);
        return !key.isEmpty() && key.equals(manualReviewKey) && now < manualReviewUntil;
    }

    void onLocationIdentified(String offerKey) {
        String key = safe(offerKey);
        if (key.equals(pendingKey)) {
            pendingKey = "";
            firstUnknownAt = 0L;
        }
    }

    void clear() {
        pendingKey = "";
        firstUnknownAt = 0L;
        manualReviewKey = "";
        manualReviewUntil = 0L;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
