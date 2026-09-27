package com.grimforsaken.sparkofferfilter;

import java.time.LocalDate;
import java.time.ZoneId;

public final class DayRolloverGuardTest {
    public static void main(String[] args) {
        ZoneId zone = ZoneId.of("America/Chicago");
        DayRolloverGuard guard = new DayRolloverGuard(zone);

        long lateDay = LocalDate.of(2026, 9, 27)
                .atTime(23, 59, 50).atZone(zone).toInstant().toEpochMilli();
        long sameDay = LocalDate.of(2026, 9, 27)
                .atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli();
        long nextDay = LocalDate.of(2026, 9, 28)
                .atTime(0, 0, 1).atZone(zone).toInstant().toEpochMilli();
        long nextDayLater = LocalDate.of(2026, 9, 28)
                .atTime(8, 0).atZone(zone).toInstant().toEpochMilli();

        guard.initialize(lateDay);
        require(!guard.changed(sameDay), "same local calendar day must not reset state");
        require(guard.changed(nextDay), "first event after midnight must reset transient state");
        require(!guard.changed(nextDayLater), "rollover reset must happen only once per new day");

        System.out.println("Day rollover guard tests passed.");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
