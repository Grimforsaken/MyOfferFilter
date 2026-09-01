package com.grimforsaken.sparkofferfilter;

final class OfferDecisionGuard {
    static final long ACCEPT_INTENT_PROTECTION_MS = 30_000L;
    static final long ACCEPTED_OFFER_PROTECTION_MS = 60_000L;
    static final long REJECT_STABILITY_MS = 650L;

    private String acceptIntentKey = "";
    private long acceptIntentUntil = 0L;
    private String acceptedKey = "";
    private long acceptedUntil = 0L;

    private String rejectCandidateKey = "";
    private String rejectCandidateReason = "";
    private long rejectCandidateFirstSeenAt = 0L;

    void noteAcceptIntent(String offerKey, long now) {
        if (offerKey == null || offerKey.isEmpty()) return;
        acceptIntentKey = offerKey;
        acceptIntentUntil = now + ACCEPT_INTENT_PROTECTION_MS;
        clearRejectCandidate();
    }

    void noteAccepted(String offerKey, long now) {
        if (offerKey == null || offerKey.isEmpty()) return;
        acceptedKey = offerKey;
        acceptedUntil = now + ACCEPTED_OFFER_PROTECTION_MS;
        acceptIntentKey = "";
        acceptIntentUntil = 0L;
        clearRejectCandidate();
    }

    boolean isRejectProtected(String offerKey, long now) {
        if (offerKey == null || offerKey.isEmpty()) return false;
        boolean accepted = offerKey.equals(acceptedKey) && now < acceptedUntil;
        boolean accepting = offerKey.equals(acceptIntentKey) && now < acceptIntentUntil;
        return accepted || accepting;
    }

    boolean isRejectStable(String offerKey, String reason, long now) {
        if (offerKey == null || offerKey.isEmpty()) return false;
        String safeReason = reason == null ? "" : reason;
        if (!offerKey.equals(rejectCandidateKey) || !safeReason.equals(rejectCandidateReason)) {
            rejectCandidateKey = offerKey;
            rejectCandidateReason = safeReason;
            rejectCandidateFirstSeenAt = now;
            return false;
        }
        return now - rejectCandidateFirstSeenAt >= REJECT_STABILITY_MS;
    }

    long rejectStabilityRemainingMs(long now) {
        if (rejectCandidateKey.isEmpty()) return REJECT_STABILITY_MS;
        return Math.max(0L, REJECT_STABILITY_MS - (now - rejectCandidateFirstSeenAt));
    }

    void clearRejectCandidate() {
        rejectCandidateKey = "";
        rejectCandidateReason = "";
        rejectCandidateFirstSeenAt = 0L;
    }

    void clear() {
        acceptIntentKey = "";
        acceptIntentUntil = 0L;
        acceptedKey = "";
        acceptedUntil = 0L;
        clearRejectCandidate();
    }
}
