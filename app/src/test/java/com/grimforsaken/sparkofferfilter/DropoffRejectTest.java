package com.grimforsaken.sparkofferfilter;

public final class DropoffRejectTest {
    public static void main(String[] args) {
        shouldParseSparkDropoffLabels();
        shouldNotRejectTwoDropoffs();
        shouldRejectThreeDropoffsWhenEnabled();
        shouldRejectMoreThanThreeDropoffsWhenEnabled();
        shouldNotRejectWhenDisabled();
        System.out.println("3+ drop-off reject tests passed.");
    }

    private static void shouldParseSparkDropoffLabels() {
        require(DropoffPolicy.parseDropoffs("3 drop-offs") == 3,
                "hyphenated Spark drop-off label should parse");
        require(DropoffPolicy.parseDropoffs("4 drop offs") == 4,
                "split drop offs label should parse");
        require(DropoffPolicy.parseDropoffs("1 drop-off") == 1,
                "singular drop-off label should parse");
    }

    private static void shouldNotRejectTwoDropoffs() {
        DropoffPolicy.configure(true);
        require(!DropoffPolicy.shouldReject("2 drop-offs"),
                "two drop-offs must remain allowed by the 3+ rule");
    }

    private static void shouldRejectThreeDropoffsWhenEnabled() {
        DropoffPolicy.configure(true);
        OfferEvaluator.Result r = OfferEvaluator.evaluate(
                "$32.14\n3.9 miles\nShopping\n3 drop-offs",
                false, false, 1.25,
                false, 15.00,
                false, 20.0,
                false,
                false, 20.00,
                false, 1.25,
                false, 10.0,
                false, false);
        require(r.ready && r.shouldReject && !r.shouldAccept,
                "three drop-offs must reject when the checkbox is enabled");
        require(r.reason.contains("3 drop-offs"),
                "reject history reason should identify the drop-off count");
    }

    private static void shouldRejectMoreThanThreeDropoffsWhenEnabled() {
        DropoffPolicy.configure(true);
        require(DropoffPolicy.shouldReject("5 drop-offs"),
                "all counts above three must reject when enabled");
    }

    private static void shouldNotRejectWhenDisabled() {
        DropoffPolicy.configure(false);
        OfferEvaluator.Result r = OfferEvaluator.evaluate(
                "$32.14\n3.9 miles\nShopping\n3 drop-offs",
                false, false, 1.25,
                false, 15.00,
                false, 20.0,
                false,
                false, 20.00,
                false, 1.25,
                false, 10.0,
                false, false);
        require(r.ready && !r.shouldReject,
                "3+ drop-off rule must do nothing while unchecked");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
