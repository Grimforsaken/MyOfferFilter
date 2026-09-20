package com.grimforsaken.sparkofferfilter;

import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

final class AcceptedOrderStore {
    private static final long RECENT_CANDIDATE_MAX_AGE_MS = 10L * 60L * 1000L;
    private static final long PENDING_ACCEPT_MAX_AGE_MS = 2L * 60L * 60L * 1000L;
    private static final int MAX_RECORDS = 5000;

    private AcceptedOrderStore() {}

    static void rememberCandidate(SharedPreferences prefs, String offerText, String city, long now) {
        OrderRecord candidate = fromOfferText(prefs, offerText, city, now, "");
        if (candidate == null) return;
        prefs.edit()
                .putString(Prefs.RECENT_OFFER_RECORD, candidate.serialize())
                .putLong(Prefs.RECENT_OFFER_AT, now)
                .apply();
    }

    static void noteAutoAccepted(SharedPreferences prefs, String offerText, String city, long now) {
        OrderRecord candidate = fromOfferText(prefs, offerText, city, now, "");
        if (candidate == null) return;
        prefs.edit()
                .putString(Prefs.PENDING_ACCEPTED_ORDER, candidate.serialize())
                .putLong(Prefs.PENDING_ACCEPTED_AT, now)
                .putString(Prefs.RECENT_OFFER_RECORD, candidate.serialize())
                .putLong(Prefs.RECENT_OFFER_AT, now)
                .apply();
    }

    static Confirmation confirmFromShoppingTripScreen(SharedPreferences prefs, String screenText, long now) {
        if (!AcceptedShoppingScreenDetector.isConfirmedShoppingTripScreen(screenText)) {
            return Confirmation.notConfirmed();
        }

        String tripId = AcceptedShoppingScreenDetector.tripId(screenText);
        String lastTripId = prefs.getString(Prefs.LAST_CONFIRMED_TRIP_ID, "");
        if (!tripId.isEmpty() && tripId.equals(lastTripId)) {
            return Confirmation.duplicate();
        }

        String screenStore = AcceptedShoppingScreenDetector.storeLabel(screenText);
        String screenStoreNumber = AcceptedShoppingScreenDetector.storeNumber(screenText);
        String screenCity = OfferCityDetector.detect(screenText);

        OrderRecord source = recentRecord(
                prefs.getString(Prefs.PENDING_ACCEPTED_ORDER, ""),
                prefs.getLong(Prefs.PENDING_ACCEPTED_AT, 0L),
                now, PENDING_ACCEPT_MAX_AGE_MS);
        if (source != null && !matches(source, screenStoreNumber, screenCity)) source = null;

        if (source == null) {
            source = recentRecord(
                    prefs.getString(Prefs.RECENT_OFFER_RECORD, ""),
                    prefs.getLong(Prefs.RECENT_OFFER_AT, 0L),
                    now, RECENT_CANDIDATE_MAX_AGE_MS);
            if (source != null && !matches(source, screenStoreNumber, screenCity)) source = null;
        }

        SharedPreferences.Editor edit = prefs.edit()
                .remove(Prefs.PENDING_ACCEPTED_ORDER)
                .remove(Prefs.PENDING_ACCEPTED_AT);
        if (!tripId.isEmpty()) edit.putString(Prefs.LAST_CONFIRMED_TRIP_ID, tripId);
        edit.apply();

        if (source == null) {
            return Confirmation.confirmedWithoutMetrics(tripId, screenStore, screenCity);
        }

        double gasPrice = prefs.getFloat(Prefs.GAS_PRICE, 0.0f);
        String city = !"Unknown".equals(screenCity) ? screenCity : source.city;
        String store = !screenStore.isEmpty() ? screenStore : source.store;
        OrderRecord confirmed = new OrderRecord(
                now, source.pay, source.miles, source.minutes, gasPrice,
                city, store, tripId);
        appendRecord(prefs, confirmed);
        return Confirmation.confirmed(confirmed);
    }

    static List<OrderRecord> records(SharedPreferences prefs) {
        String raw = prefs.getString(Prefs.CONFIRMED_ORDER_RECORDS, "");
        if (raw == null || raw.trim().isEmpty()) return Collections.emptyList();
        List<OrderRecord> out = new ArrayList<>();
        String[] lines = raw.split("\\n");
        for (String line : lines) {
            OrderRecord r = OrderRecord.parse(line);
            if (r != null) out.add(r);
        }
        return out;
    }

    static int deleteRecords(SharedPreferences prefs, Set<String> recordKeys) {
        if (prefs == null || recordKeys == null || recordKeys.isEmpty()) return 0;
        List<OrderRecord> current = records(prefs);
        List<OrderRecord> kept = OrderAnalytics.withoutKeys(current, recordKeys);
        int deleted = current.size() - kept.size();
        if (deleted > 0) saveRecords(prefs, kept);
        return deleted;
    }

    private static OrderRecord fromOfferText(SharedPreferences prefs, String text, String city, long now, String tripId) {
        if (text == null) return null;
        String normalized = OfferEvaluator.normalize(text);
        boolean shopping = normalized.contains("SHOPPING")
                || normalized.contains("SHOP & DELIVER")
                || normalized.contains("SHOP AND DELIVER");
        if (!shopping) return null;

        Double pay = OfferEvaluator.parseBestPay(text);
        Double miles = OfferEvaluator.parseMiles(text);
        int minutes = TripDurationPolicy.parseTripMinutes(text);
        if (pay == null || pay <= 0.0 || miles == null || miles <= 0.0 || minutes <= 0) return null;

        String store = AcceptedShoppingScreenDetector.storeLabel(text);
        return new OrderRecord(now, pay, miles, minutes,
                prefs.getFloat(Prefs.GAS_PRICE, 0.0f),
                city == null ? "" : city, store, tripId);
    }

    private static OrderRecord recentRecord(String serialized, long at, long now, long maxAge) {
        if (serialized == null || serialized.isEmpty() || at <= 0L) return null;
        long age = now - at;
        if (age < 0L || age > maxAge) return null;
        return OrderRecord.parse(serialized);
    }

    private static boolean matches(OrderRecord record, String screenStoreNumber, String screenCity) {
        if (record == null) return false;
        if (screenStoreNumber != null && !screenStoreNumber.isEmpty()) {
            String recordStoreNumber = AcceptedShoppingScreenDetector.storeNumber(record.store);
            if (!recordStoreNumber.isEmpty()) return recordStoreNumber.equals(screenStoreNumber);
        }
        if (screenCity != null && !screenCity.isEmpty() && !"Unknown".equals(screenCity)
                && record.city != null && !record.city.isEmpty() && !"Unknown".equals(record.city)) {
            return record.city.equalsIgnoreCase(screenCity);
        }
        return true;
    }

    private static void appendRecord(SharedPreferences prefs, OrderRecord record) {
        List<OrderRecord> current = new ArrayList<>(records(prefs));
        current.add(0, record);
        if (current.size() > MAX_RECORDS) current = current.subList(0, MAX_RECORDS);
        saveRecords(prefs, current);
    }

    private static void saveRecords(SharedPreferences prefs, List<OrderRecord> records) {
        StringBuilder out = new StringBuilder();
        if (records != null) {
            for (OrderRecord r : records) {
                if (r == null) continue;
                if (out.length() > 0) out.append('\n');
                out.append(r.serialize());
            }
        }
        prefs.edit().putString(Prefs.CONFIRMED_ORDER_RECORDS, out.toString()).apply();
    }

    static final class Confirmation {
        final boolean screenConfirmed;
        final boolean duplicate;
        final OrderRecord record;
        final String tripId;
        final String store;
        final String city;

        private Confirmation(boolean screenConfirmed, boolean duplicate, OrderRecord record,
                             String tripId, String store, String city) {
            this.screenConfirmed = screenConfirmed;
            this.duplicate = duplicate;
            this.record = record;
            this.tripId = tripId == null ? "" : tripId;
            this.store = store == null ? "" : store;
            this.city = city == null ? "" : city;
        }

        static Confirmation notConfirmed() {
            return new Confirmation(false, false, null, "", "", "");
        }

        static Confirmation duplicate() {
            return new Confirmation(true, true, null, "", "", "");
        }

        static Confirmation confirmed(OrderRecord record) {
            return new Confirmation(true, false, record, record.tripId, record.store, record.city);
        }

        static Confirmation confirmedWithoutMetrics(String tripId, String store, String city) {
            return new Confirmation(true, false, null, tripId, store, city);
        }
    }
}
