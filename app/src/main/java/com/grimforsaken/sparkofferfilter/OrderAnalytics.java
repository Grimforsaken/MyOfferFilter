package com.grimforsaken.sparkofferfilter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class OrderAnalytics {
    static final double MPG = 35.0;

    private OrderAnalytics() {}

    static List<OrderRecord> withoutKeys(List<OrderRecord> records, Set<String> keys) {
        List<OrderRecord> kept = new ArrayList<>();
        if (records == null) return kept;
        for (OrderRecord record : records) {
            if (record == null) continue;
            if (keys != null && keys.contains(record.stableKey())) continue;
            kept.add(record);
        }
        return kept;
    }

    static Summary summarize(List<OrderRecord> records) {
        int count = 0;
        double pay = 0.0;
        double miles = 0.0;
        int minutes = 0;
        double fuelCost = 0.0;
        double gasWeighted = 0.0;
        double gasMiles = 0.0;

        if (records != null) {
            for (OrderRecord r : records) {
                if (r == null) continue;
                count++;
                pay += r.pay;
                miles += r.miles;
                minutes += r.minutes;
                fuelCost += r.fuelCostAt35Mpg();
                if (r.gasPrice > 0.0 && r.miles > 0.0) {
                    gasWeighted += r.gasPrice * r.miles;
                    gasMiles += r.miles;
                }
            }
        }

        double dpm = miles > 0.0 ? pay / miles : 0.0;
        double dph = minutes > 0 ? pay * 60.0 / minutes : 0.0;
        double avgGas = gasMiles > 0.0 ? gasWeighted / gasMiles : 0.0;
        return new Summary(count, pay, miles, minutes, dpm, dph, fuelCost, pay - fuelCost, avgGas);
    }

    static final class Summary {
        final int count;
        final double totalPay;
        final double totalMiles;
        final int totalMinutes;
        final double dollarsPerMile;
        final double dollarsPerHour;
        final double fuelCost;
        final double afterFuel;
        final double averageGasPrice;

        Summary(int count, double totalPay, double totalMiles, int totalMinutes,
                double dollarsPerMile, double dollarsPerHour, double fuelCost,
                double afterFuel, double averageGasPrice) {
            this.count = count;
            this.totalPay = totalPay;
            this.totalMiles = totalMiles;
            this.totalMinutes = totalMinutes;
            this.dollarsPerMile = dollarsPerMile;
            this.dollarsPerHour = dollarsPerHour;
            this.fuelCost = fuelCost;
            this.afterFuel = afterFuel;
            this.averageGasPrice = averageGasPrice;
        }
    }
}
