package com.grimforsaken.sparkofferfilter;

public final class TripDurationRejectTest {
    public static void main(String[] args) {
        shouldRejectMinutesOverLimit();
        shouldNotRejectExactlyAtLimit();
        shouldParseHoursAndMinutes();
        shouldIgnoreArrivalWindowMinutes();
        shouldDoNothingWhenDisabled();
        System.out.println("Trip duration reject tests passed.");
    }

    private static void shouldRejectMinutesOverLimit() {
        TripDurationPolicy.configure(true, 0, 45);
        String text = "$32.14\n3 stops • 3.9 miles • 54 mins\nWalmart SAPULPA #123\nREJECT\nACCEPT";
        require(TripDurationPolicy.parseTripMinutes(text) == 54, "54-minute trip must parse");
        require(TripDurationPolicy.shouldReject(text), "54 minutes must reject over a 45-minute maximum");
    }

    private static void shouldNotRejectExactlyAtLimit() {
        TripDurationPolicy.configure(true, 0, 54);
        String text = "3 stops • 3.9 miles • 54 mins";
        require(!TripDurationPolicy.shouldReject(text), "equal duration must not reject; only greater-than rejects");
    }

    private static void shouldParseHoursAndMinutes() {
        TripDurationPolicy.configure(true, 1, 15);
        String text = "4 stops • 22.0 miles • 1 hr 16 mins";
        require(TripDurationPolicy.parseTripMinutes(text) == 76, "1 hr 16 mins must parse as 76 minutes");
        require(TripDurationPolicy.shouldReject(text), "76 minutes must reject over 1 hr 15 min");
    }

    private static void shouldIgnoreArrivalWindowMinutes() {
        TripDurationPolicy.configure(true, 0, 20);
        String text = "2 stops • 3.7 miles • 20 mins\nASAP (arrive within 25 mins) • Shopping";
        require(TripDurationPolicy.parseTripMinutes(text) == 20,
                "trip parser must use summary duration, not arrive-within time");
        require(!TripDurationPolicy.shouldReject(text),
                "arrival-window text must not cause a false duration rejection");
    }

    private static void shouldDoNothingWhenDisabled() {
        TripDurationPolicy.configure(false, 0, 1);
        String text = "5 stops • 40 miles • 2 hrs 5 mins";
        require(!TripDurationPolicy.shouldReject(text), "disabled duration rule must not reject");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
