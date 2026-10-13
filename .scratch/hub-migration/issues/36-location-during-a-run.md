# 36: Location during a run

**What to build:** during a run the phone knows where it is once a second, with the screen off, and says so on the main screen. The GPS row appears when a run starts and shows the latest fix's accuracy or that there is none. The latest fix is held by the service for the log and the bridge, which later tickets add.

Spec: [Android app spec](../spec-android.md) 2.7 and 3.1 (GPS row).

**Blocked by:** 35 (A run on the simulated board).

**Status:** resolved

- [x] The service asks for precise location once a second during a run and not outside one
- [x] Each fix is published inside the service as the latest fix, with a way for other parts to be told of every new fix
- [x] A field the fix does not carry (altitude, speed, heading, accuracy) is kept as missing, not as 0
- [x] The GPS row appears only during a run: `Fix, ±4 m` in green with a fix in the last 3 s, otherwise `No fix` in amber
- [x] Location services switched off during a run is the same as no fix, and the run continues
- [x] The simulated board does not touch location
- [x] Fixes arrive on a stock Android Studio emulator image playing a route, with the screen off
- [x] The choice between the platform's location API and Google Play services' is recorded where the next reader will find it

## Comments

Implemented as `LocationUpdates` and `Fix` in the package `org.metucet.redlink.location`, owned by `RunService` for the length of a run, and the GPS row on the main screen. Run on the Android 16 emulator image with the simulated board and a route played into the emulator's GPS, where each line above was seen: no location request before Start; at Start one 1 s, high-accuracy request to the GPS provider and the row reading `Fix, ±5 m` in green; location services switched off, the row reading `No fix` in amber with the board still answering, and fixes coming back about 4 s after the switch was turned on again; `Save & stop` leaving the GPS provider with no request.

- **The API** is the platform's `LocationManager` and its GPS provider, not Google Play services. [ADR 0003](../../../docs/adr/0003-location-from-the-platform-gps-provider.md) says why, the spec's 2.7 points to it, and `LocationUpdates` says so where the request is made.
- **Screen off.** With the screen off and the emulator forced into deep idle, the app was given 30 fixes in 30 seconds.
- **Two ways to hear of fixes.** `latest` holds the latest fix for the screen; a collector that falls behind sees only the newest. `fixes` gives every fix in order, with room for about a minute of them, for the log (37) and the stream (40), which must not miss one. `Run` carries the latest fix to the activity.
- **Missing fields** are null in `Fix`, read from the provider's `has…` checks. A reading coarser than 6,553.5 m is not a fix, as 2.7 says, and never reaches either flow.
- **The row's age** is measured from when the fix was taken on the phone's monotonic clock, not from when the app received it. The row turns amber at 3 s with no new fix to redraw it.
- **Accuracy on the row** is rounded up to whole metres, so the row never claims better than the fix. A fix with no accuracy reads `Fix`. The spec gave neither; its 3.1 says both now.
- **Automated tests** cover the too-coarse check and the GPS row as plain functions. The service and the screen are not tested, as the spec's section 4 says.

Known and left:

- **A grant taken away during a run** reads as no fix for the rest of the run. Granting it again does not bring fixes back until the next run. The location switch is different: it does bring them back.
- **The emulator repeats its last position** every second, so the GPS row's amber state shows there only with location services switched off.
- **Altitude** is the provider's height above the WGS 84 ellipsoid, not above sea level. The spec does not say which height it wants.

Not tried here:

- **The reference phone** with the screen off, which 2.7 asks for and 2.14 lists as not established. That is for the desk checklist (48).
