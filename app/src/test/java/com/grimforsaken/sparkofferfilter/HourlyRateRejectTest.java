package com.grimforsaken.sparkofferfilter;

public final class HourlyRateRejectTest {
    public static void main(String[] args) {
        rejectsBelowHourlyMinimum();
        allowsExactlyAtHourlyMinimum();
        calculatesUsingHourAndMinuteDuration();
        ignoresArrivalWindowMinutes();
        waitsWhenTripDurationMissing();
        doesNothingWhenDisabled();
        System.out.println("Hourly-rate reject tests passed.");
    }

    private static void rejectsBelowHourlyMinimum() {
        HourlyRatePolicy.configure(true, 20.00);
        String text = "$19.00\n2 stops • 5.0 miles • 60 mins\nWalmart SAPULPA #123";
        Double hourly = HourlyRatePolicy.calculateDollarsPerHour(19.00, text);
        require(close(hourly, 19.00), "$19 over 60 minutes must calculate as $19/hr");
        require(HourlyRatePolicy.shouldReject(19.00, text), "$19/hr must reject below $20/hr");
    }

    private static void allowsExactlyAtHourlyMinimum() {
        HourlyRatePolicy.configure(true, 20.00);
        String text = "$10.00\n2 stops • 3.0 miles • 30 mins";
        require(close(HourlyRatePolicy.calculateDollarsPerHour(10.00, text), 20.00),
                "$10 over 30 minutes must calculate as $20/hr");
        require(!HourlyRatePolicy.shouldReject(10.00, text),
                "exactly equal to the minimum must not reject");
    }

    private static void calculatesUsingHourAndMinuteDuration() {
        HourlyRatePolicy.configure(true, 25.00);
        String text = "$30.00\n3 stops • 15.0 miles • 1 hr 30 mins";
        require(close(HourlyRatePolicy.calculateDollarsPerHour(30.00, text), 20.00),
                "$30 over 90 minutes must calculate as $20/hr");
        require(HourlyRatePolicy.shouldReject(30.00, text),
                "$20/hr must reject below a $25/hr minimum");
    }

    private static void ignoresArrivalWindowMinutes() {
        HourlyRatePolicy.configure(true, 25.00);
        String text = "$20.00\n2 stops • 3.7 miles • 36 mins\nASAP (arrive within 25 mins) • Shopping";
        Double hourly = HourlyRatePolicy.calculateDollarsPerHour(20.00, text);
        require(close(hourly, 33.3333333333),
                "hourly calculation must use the 36-minute trip summary, not arrive-within 25 mins");
        require(!HourlyRatePolicy.shouldReject(20.00, text),
                "$33.33/hr must not reject below a $25/hr minimum");
    }

    private static void waitsWhenTripDurationMissing() {
        HourlyRatePolicy.configure(true, 30.00);
        String text = "$10.00\nASAP (arrive within 25 mins) • Shopping";
        require(HourlyRatePolicy.calculateDollarsPerHour(10.00, text) == null,
                "missing trip-summary duration must not manufacture an hourly rate");
        require(!HourlyRatePolicy.shouldReject(10.00, text),
                "missing trip-summary duration must not trigger hourly rejection");
    }

    private static void doesNothingWhenDisabled() {
        HourlyRatePolicy.configure(false, 100.00);
        String text = "$10.00\n2 stops • 5.0 miles • 60 mins";
        require(!HourlyRatePolicy.shouldReject(10.00, text),
                "disabled hourly-rate rule must not reject");
    }

    private static boolean close(Double actual, double expected) {
        return actual != null && Math.abs(actual - expected) < 0.0001;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
