package com.grimforsaken.sparkofferfilter;

final class ActionSafetyGuard {
    static final long POST_ACCEPT_REJECT_LOCKOUT_MS = 10_000L;

    private long rejectLockoutUntil = 0L;

    void onAccepted(long now) {
        rejectLockoutUntil = Math.max(rejectLockoutUntil, now + POST_ACCEPT_REJECT_LOCKOUT_MS);
    }

    boolean isRejectLocked(long now) {
        return now < rejectLockoutUntil;
    }

    long remainingRejectLockoutMs(long now) {
        return Math.max(0L, rejectLockoutUntil - now);
    }

    void clear() {
        rejectLockoutUntil = 0L;
    }
}
