package com.grimforsaken.sparkofferfilter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

final class DayRolloverGuard {
    private final ZoneId zoneId;
    private LocalDate activeDay;

    DayRolloverGuard() {
        this(ZoneId.systemDefault());
    }

    DayRolloverGuard(ZoneId zoneId) {
        this.zoneId = zoneId == null ? ZoneId.systemDefault() : zoneId;
    }

    void initialize(long now) {
        activeDay = localDate(now);
    }

    boolean changed(long now) {
        LocalDate currentDay = localDate(now);
        if (activeDay == null) {
            activeDay = currentDay;
            return false;
        }
        if (activeDay.equals(currentDay)) return false;
        activeDay = currentDay;
        return true;
    }

    private LocalDate localDate(long now) {
        return Instant.ofEpochMilli(now).atZone(zoneId).toLocalDate();
    }
}
