# Android runtime and stack

Type: grilling
Status: resolved
Blocked by: 02

Map: [Hub migration](../map.md)

## Question

How is the Android app built so it runs as a headless logger and bridge with the screen off?

Decide: the service model and what the user must grant at first launch; reconnect behaviour when BLE or the hub connection drops mid-run; what starts and stops a run; UI toolkit, minimum Android version and BLE library.

## Answer

A run is one lifetime of a single foreground service, started and stopped by a person, and it resumes by itself if Android kills it. Decided by grilling on 2026-10-01 and 2026-10-02, on top of [Android headless operation constraints](02-android-headless-operation-constraints.md). Nothing here was run on a phone.

- **Run:** begins when someone presses Start and ends when someone presses Stop, or when the phone restarts. It never ends on its own otherwise, however long the board is unreachable. Bridging and logging happen only during a run. A run can start with the board off or out of range; viewers see "board unreachable" until it answers. Automatic start on Bluetooth connect was rejected: it would cut the log at every drop.
- **Stop:** in the app, and as an action on the notification with a confirmation. The notification shows the run's duration.
- **Service:** one foreground service typed `connectedDevice|location`, holding a partial wake lock for the whole run. It owns the board link, the hub sockets, location and the log; the activity only observes it.
- **Automatic restart:** the app stores "a run is active" on Start and clears it on Stop. If Android kills the service or the app crashes, the service comes back and resumes the same run; the gap shows as missing samples. After 3 automatic restarts within one minute the app stops resuming, ends the run and posts a notification. A phone restart is not resumed: the next time the app is opened it says the last run ended when the phone restarted, and Start begins a new run. No restart at all was rejected because permission prompts are no burden on the team's own phones.
- **Grants:** Bluetooth, precise location, location "all the time", location services on, battery-optimisation exemption and notifications. A setup screen lists each as a row with its state, and Start is disabled until all six are in place. "All the time" location and the battery exemption are required by automatic restart: Android blocks starting a foreground service from the background without the exemption, and location in a service started that way needs background location (from the Android documentation, not checked on a phone). Manufacturer settings the app cannot read, such as Samsung's "never sleeping apps", are a written checklist.
- **Board:** the app remembers one board, picked once on a setup screen and stored by address; Start connects without scanning. "Change board" lives in settings.
- **Board link drops:** retry until Stop. A direct connect at once, then Android's `autoConnect` after a few failures. The log stays open, and GPS keeps being logged and streamed.
- **Hub connection drops:** logging is unaffected. The bridge re-opens the lobby only: first retry at once, then 250 ms doubling to a 10 s cap, each wait randomised by 20% either way, and at once when Android reports a network change. Viewers' registrations are not restored; viewers come back through the lobby.
- **Stack:** Kotlin, Jetpack Compose, Nordic Android-BLE-Library 2.x. Minimum Android 12 (API 31), targeting the current API. The reference phone is the driver's Samsung on Android 16; another team member's phone may stand in. Kable was rejected: it pays off only if the later iOS app shares Kotlin code.

Found on the way: a silent loss of the phone's network leaves every registration "registered" on the hub, possibly for minutes, so `PING` answers `PONG` for a dead registration. The "phone lost" rule in [What travels on the stream](05-what-travels-on-the-stream.md) is amended there. Whether a stale lobby can be re-registered is unknown and goes to [Stale re-registration on the public hub](13-stale-re-registration-on-the-public-hub.md).

Handed on: whether a resumed run appends to its file or opens a second one goes to [What the phone log contains](08-what-the-phone-log-contains.md); the setup screen and notification layout go to [What the phone screen shows](09-what-the-phone-screen-shows.md); the randomised lobby retry on the viewer goes to [Desktop app architecture](11-desktop-app-architecture.md).

Amended 2026-10-02 by [Testing without the car](16-testing-without-the-car.md): with the simulated board selected in a debug build, the Bluetooth grants are not asked for and do not block Start.
