# MyOfferFilter

MyOfferFilter is a personal Android companion for the Spark Driver app. It reads only visible Spark offer content through an Android Accessibility Service and applies user-selected offer rules.

## v1.1.0 rules

### Auto-Reject
- Always rejects an offer when neither **SAND SPRINGS** nor **SAPULPA** is visible.
- Optional: reject if **Shopping** is not visible.
- Optional: reject when calculated offer dollars per mile is below an editable threshold. Default: **$1.25/mi**.

### Auto-Accept
Auto-Accept is **OFF by default**. When enabled, every enabled acceptance criterion must pass:
- Optional minimum total order dollar amount.
- Optional minimum dollars per mile.
- Optional maximum miles.

If Auto-Accept is enabled but no acceptance criterion is checked, the app auto-accepts nothing. Auto-Reject rules always take priority over Auto-Accept.

### Chimes
- A distinct **accept chime** plays after a successful automatic Accept click.
- A distinct **reject chime** plays after a successful automatic Reject click.
- Decision chimes can be turned off.
- Chimes do not play in Test mode.

## Safety behavior
- Test mode is ON by default and logs what would happen without clicking Accept or Reject.
- The Accessibility Service only responds to `com.walmart.sparkdriver`.
- It waits 1.2 seconds for the visible offer screen to stop changing before evaluation.
- It requires readable pay and mileage before acting.
- It uses a visible Reject/Decline control as a conservative offer-screen guard.
- Duplicate automatic actions on the same visible offer are suppressed for 30 seconds.
- The app has no Internet permission and does not modify the Spark Driver APK.

Spark can change its UI at any time. Verify several real offer screens in Test mode while parked before enabling automatic actions after installing or after a Spark Driver update.

## Build

A GitHub Actions workflow in `.github/workflows/build-apk.yml` builds a debug APK on each push to `main` and uploads it as the `MyOfferFilter-debug-apk` artifact.
