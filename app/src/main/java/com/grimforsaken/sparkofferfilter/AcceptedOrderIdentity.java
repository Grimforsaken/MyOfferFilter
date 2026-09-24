package com.grimforsaken.sparkofferfilter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

final class AcceptedOrderIdentity {
    private AcceptedOrderIdentity() {}

    static boolean isDuplicate(List<OrderRecord> records, String tripId, String storeNumber, long now) {
        if (tripId == null || tripId.trim().isEmpty() || records == null) return false;
        LocalDate currentDay = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate();

        for (OrderRecord record : records) {
            if (record == null || !tripId.equals(record.tripId)) continue;
            LocalDate recordDay = Instant.ofEpochMilli(record.timestampMs)
                    .atZone(ZoneId.systemDefault()).toLocalDate();
            if (!currentDay.equals(recordDay)) continue;

            if (storeNumber != null && !storeNumber.isEmpty()) {
                String recordStore = AcceptedShoppingScreenDetector.storeNumber(record.store);
                if (!recordStore.isEmpty() && !storeNumber.equals(recordStore)) continue;
            }
            return true;
        }
        return false;
    }
}
