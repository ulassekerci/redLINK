# 36: Location during a run

**What to build:** during a run the phone knows where it is once a second, with the screen off, and says so on the main screen. The GPS row appears when a run starts and shows the latest fix's accuracy or that there is none. The latest fix is held by the service for the log and the bridge, which later tickets add.

Spec: [Android app spec](../spec-android.md) 2.7 and 3.1 (GPS row).

**Blocked by:** 35 (A run on the simulated board).

**Status:** ready-for-agent

- [ ] The service asks for precise location once a second during a run and not outside one
- [ ] Each fix is published inside the service as the latest fix, with a way for other parts to be told of every new fix
- [ ] A field the fix does not carry (altitude, speed, heading, accuracy) is kept as missing, not as 0
- [ ] The GPS row appears only during a run: `Fix, ±4 m` in green with a fix in the last 3 s, otherwise `No fix` in amber
- [ ] Location services switched off during a run is the same as no fix, and the run continues
- [ ] The simulated board does not touch location
- [ ] Fixes arrive on a stock Android Studio emulator image playing a route, with the screen off
- [ ] The choice between the platform's location API and Google Play services' is recorded where the next reader will find it
