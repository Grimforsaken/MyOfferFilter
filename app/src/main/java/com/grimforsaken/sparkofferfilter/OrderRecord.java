package com.grimforsaken.sparkofferfilter;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

final class OrderRecord {
    final long timestampMs;
    final double pay;
    final double miles;
    final int minutes;
    final double gasPrice;
    final String city;
    final String store;
    final String tripId;

    OrderRecord(long timestampMs, double pay, double miles, int minutes, double gasPrice,
                String city, String store, String tripId) {
        this.timestampMs = timestampMs;
        this.pay = pay;
        this.miles = miles;
        this.minutes = minutes;
        this.gasPrice = gasPrice;
        this.city = clean(city);
        this.store = clean(store);
        this.tripId = clean(tripId);
    }

    double dollarsPerMile() {
        return miles > 0.0 ? pay / miles : 0.0;
    }

    double dollarsPerHour() {
        return minutes > 0 ? pay * 60.0 / minutes : 0.0;
    }

    double fuelGallonsAt35Mpg() {
        return miles > 0.0 ? miles / 35.0 : 0.0;
    }

    double fuelCostAt35Mpg() {
        return gasPrice > 0.0 ? fuelGallonsAt35Mpg() * gasPrice : 0.0;
    }

    double afterFuel() {
        return pay - fuelCostAt35Mpg();
    }

    String serialize() {
        return timestampMs + "|" + pay + "|" + miles + "|" + minutes + "|" + gasPrice + "|"
                + enc(city) + "|" + enc(store) + "|" + enc(tripId);
    }

    static OrderRecord parse(String line) {
        if (line == null || line.trim().isEmpty()) return null;
        String[] p = line.split("\\|", -1);
        if (p.length != 8) return null;
        try {
            return new OrderRecord(
                    Long.parseLong(p[0]),
                    Double.parseDouble(p[1]),
                    Double.parseDouble(p[2]),
                    Integer.parseInt(p[3]),
                    Double.parseDouble(p[4]),
                    dec(p[5]), dec(p[6]), dec(p[7]));
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String enc(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(clean(value).getBytes(StandardCharsets.UTF_8));
    }

    private static String dec(String value) {
        try {
            return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim().replaceAll("[\\r\\n]+", " ");
    }
}
