# Android app spec

Part of the [hub migration spec](spec.md). Read the front page first, then the [hub usage spec](spec-hub.md): this file cites it for every message name, state name and timing on the hub and does not restate them.

Vocabulary is from `CONTEXT.md`. Where this spec and a ticket disagree, this spec wins.

## 1. Purpose

The phone app rides in the car. During a run it is the bridge, carrying the board's replies and the phone's GPS to every viewer through the hub, and it writes the log of record. It must do both for hours with the screen off and nobody touching it. When someone does look at the screen, it says in a few rows whether each link is up. It shows no telemetry.

## 2. Behaviour

### 2.1 The run

- A run begins when someone presses Start and ends when someone presses Stop, or when the phone restarts. It never ends by itself otherwise, however long the board is unreachable or the hub is gone. The one exception is the restart limit in 2.3.
- Bridging and logging happen only during a run. Outside a run the app holds no board link, no hub socket, no location updates and no wake lock.
- A run can start with the board off or out of range. Viewers see board unreachable until it answers.
- Start is disabled until every grant in 2.4 is in place and a board is picked. Nothing starts a run automatically; in particular a Bluetooth connect does not.
- Stop is on the main screen and on the notification. Both ask first (3.5).

Tickets: [Android runtime and stack](issues/07-android-runtime-and-stack.md).

### 2.2 The foreground service

- A run is one lifetime of a single foreground service, typed `connectedDevice|location`, started from the visible activity when Start is pressed.
- The service owns the board link, the hub sockets, location updates and the log. The activity only observes it: closing the activity, or swiping the app out of recents, changes nothing about the run.
- The service holds a partial wake lock for the whole run.
- Manifest permissions: `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_CONNECTED_DEVICE`, `FOREGROUND_SERVICE_LOCATION`, `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`, `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`, `POST_NOTIFICATIONS`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, `WAKE_LOCK`, `INTERNET`, `ACCESS_NETWORK_STATE`. The `dataSync` service type is not used: Android caps it at 6 hours a day.
- The service's notification is described in 3.4.

Tickets: [Android headless operation constraints](issues/02-android-headless-operation-constraints.md), [Android runtime and stack](issues/07-android-runtime-and-stack.md).

### 2.3 Automatic restart

- On Start the app stores that a run is active, with the run's start time, its log file and the phone's boot count. On Stop it clears this.
- If Android kills the service or the app crashes, the service comes back and resumes the same run: same log file, same lobby ID, `elapsed_s` still counted from the original Start. The gap shows as missing rows.
- If the app is opened and finds a run stored as active, on the same boot, with no service running, it resumes the run the same way.
- **Restart limit:** after 3 automatic restarts within one minute the app stops resuming. It ends the run, clears the stored state and posts the notification in 3.4.
- **Phone restart:** a run is not resumed after the phone restarts. The app does not start at boot. The next time it is opened it sees a stored run from an earlier boot, clears it and shows the restart notice (3.1). Start then begins a new run with a new file.
- Automatic restart is why "all the time" location and the battery-optimisation exemption are grants: Android blocks starting a foreground service from the background without the exemption, and location in a service started that way needs background location.

Tickets: [Android runtime and stack](issues/07-android-runtime-and-stack.md).

### 2.4 Grants

Six grants, each a row on the Setup screen with its state and a Grant button. Start is disabled until all are in place.

| Row | What it is on Android |
|---|---|
| Bluetooth | `BLUETOOTH_CONNECT` and `BLUETOOTH_SCAN` ("Nearby devices") |
| Precise location | `ACCESS_FINE_LOCATION`, with precise chosen |
| Location "all the time" | `ACCESS_BACKGROUND_LOCATION` |
| Location services on | the system location switch |
| Battery optimisation exemption | the app is on the ignore-battery-optimisations list |
| Notifications | `POST_NOTIFICATIONS` on Android 13 and later; on Android 12, notifications not blocked for the app |

- The app re-reads all six whenever the Setup or main screen is shown, since any can be taken away in system settings.
- With the simulated board selected (2.12) the Bluetooth row is not asked for and does not block Start. The other five behave as on a phone.
- Settings the app cannot read, such as a manufacturer's list of apps that are never put to sleep, are not rows. They are the written checklist in 2.14.

Tickets: [Android runtime and stack](issues/07-android-runtime-and-stack.md), [Android headless operation constraints](issues/02-android-headless-operation-constraints.md), [Testing without the car](issues/16-testing-without-the-car.md).

### 2.5 The board link

- **Picking:** the app remembers one board. It is picked once on the Setup screen from a scan for devices offering the Nordic UART service (`6e400001-b5a3-f393-e0a9-e50e24dcca9e`) and stored by address. "Change vehicle" on Setup picks another. The board cannot be changed during a run.
- **Connecting:** Start connects to the stored address without scanning. The app requests the largest MTU (517), writes to the RX characteristic (`6e400002-…`) and listens for notifications on the TX characteristic (`6e400003-…`). A frame can still span several notifications, so the decoder buffers as the hub spec's framing section describes.
- **Firmware version:** after each connect, before polling starts, the bridge sends `COMM_FW_VERSION` once and waits up to 250 ms. The major and minor from the reply are remembered and shown on Setup under the picked board (3.3). The reply is not logged and not copied to any registration. No reply leaves the version unknown and polling starts anyway. No version is refused or warned about.
- **Polling:** as the hub spec's "Polling and the stream" says, including the 250 ms and 2 s timeouts and what makes the board unreachable.
- **When the link drops:** the bridge retries until Stop: a direct connect at once, and after a few failures Android's `autoConnect`. The log stays open, and GPS keeps being logged and streamed. Bluetooth switched off on the phone is the same as a dropped link.
- **Faults:** a non-zero fault code in a setup reply turns the Vehicle row red and names the fault (3.1). Names are the firmware 6.06 fault code names without the `FAULT_CODE_` prefix; a code the app has no name for is shown as its number.
- **The seam:** everything above the board link talks to one interface (send a request, receive bytes, connection state). The Bluetooth implementation and the simulated board (2.12) are the two implementations.

Tickets: [Android runtime and stack](issues/07-android-runtime-and-stack.md), [Android headless operation constraints](issues/02-android-headless-operation-constraints.md), [Board speed settings and firmware version](issues/14-board-speed-settings-and-firmware-version.md), [What the phone screen shows](issues/09-what-the-phone-screen-shows.md). Reading the firmware version and showing it on Setup was decided while writing this spec.

### 2.6 The bridge on the hub

The bridge's whole behaviour on the hub is in the hub spec: "The lobby, bridge side", "Polling and the stream", "Our messages" and "Team code and hub identities". This app implements those sections as written. What is specific to Android:

- Every registration is its own TCP socket with keep-alive on, owned by the service.
- The lobby is registered when the run starts and dropped when it ends. Stop closes every socket, so the hub closes each viewer's socket and viewers go to phone not found.
- "Android reports a network change" in the hub spec's re-registration rule means a `ConnectivityManager` network callback for a newly available default network. While Android reports no network at all, the Hub row says so (3.1).
- A hub connection that drops never touches the log or the board link.
- **Viewers counted** on the screen and in the notification are the active registrations, in the hub spec's sense: those with a heartbeat in the last 3 s.
- The protocol version in the status message is the major of the app's own version (2.13).

Tickets: [How several pit laptops watch at once](issues/04-how-several-pit-laptops-watch-at-once.md), [What travels on the stream](issues/05-what-travels-on-the-stream.md), [Android runtime and stack](issues/07-android-runtime-and-stack.md).

### 2.7 Location

- During a run the service asks for precise location once a second. Each fix is sent as a GPS message and kept as the latest fix for the log.
- A field the fix does not carry (altitude, speed, heading or accuracy) is sent as 0, as the hub spec's "Our messages" says, and left empty in the log.
- A fix whose accuracy is worse than 6,553.5 m, the most the GPS message carries, is not a fix: it is not sent, not kept for the log and not shown on the screen as one.
- With no fix, nothing is sent and the log's GPS cells stay empty until the first one. Location services switched off during a run is the same as no fix; the run continues.
- Which location API is used (the platform's or Google Play services') is left to implementation, under one condition: it gives fixes on a stock Android Studio emulator image playing a route, and on the reference phone with the screen off.
- The simulated board does not touch location: GPS always comes from the real provider.

Tickets: [What travels on the stream](issues/05-what-travels-on-the-stream.md), [Testing without the car](issues/16-testing-without-the-car.md). Sending 0 for a field the fix lacks was decided while writing this spec.

### 2.8 The log of record

One CSV file per run.

- **Place and name:** the phone's shared `Documents/redLINK/` folder, written through MediaStore, which needs no storage permission. The name is `redLINK_<local start time>.csv`, for example `redLINK_2026-09-26_14-22-09.csv`. A simulated run's file is `SIM_redLINK_<local start time>.csv`.
- **Layout:** comma-separated, decimal points, the column names as the first line, no comment lines. An empty cell is a missing value.
- **Rows:** one per poll cycle in which at least one reply arrived. A missing reply leaves its cells empty. While the board is unreachable there is one row per GPS fix, with the board cells empty. The latest GPS fix is repeated on every row.
- **Flushing:** the file is flushed to storage once a second, so a kill or a dead battery loses at most about a second.
- **Resuming:** a run resumed by automatic restart appends to the same file. The column names are not written again.
- **When the file cannot be written** (storage full, an I/O error): the run and the bridge continue and viewers are unaffected. The Log row turns red (3.1), a notification says so (3.4), and the app tries again every second. Rows from the failed span are lost; the gap shows in the time columns.
- **Retention:** nothing is deleted automatically. A person deletes logs on the Logs screen. About 15 MB per hour of running.
- **After a reinstall** the app no longer owns earlier files. They drop out of its list and stay in the folder, reachable from the Files app and over USB.
- **Trips:** the log knows nothing about trips. A file is a run.

**Columns,** in order. Board values are as decoded from the hub spec's "Board commands" tables, written with the decimals the wire carries. `power_w` is the only value the phone computes.

| Column | Source | Form |
|---|---|---|
| `time_utc` | the phone's wall clock when the row is written | ISO 8601 with milliseconds, `2026-09-26T11:22:09.350Z` |
| `elapsed_s` | monotonic clock, seconds since Start | three decimals, `12.350` |
| `speed_m_s` | setup reply: speed | |
| `distance_m` | setup reply: distance | |
| `distance_abs_m` | setup reply: absolute distance | |
| `erpm` | setup reply: ERPM | |
| `duty_cycle` | setup reply: duty cycle | -1 to 1 |
| `battery_voltage_v` | setup reply: battery voltage | |
| `battery_current_a` | setup reply: battery current | |
| `motor_current_a` | setup reply: motor current | |
| `power_w` | battery voltage x battery current | two decimals |
| `battery_level` | setup reply: battery level | 0 to 1 |
| `energy_used_wh` | setup reply: energy used | |
| `energy_charged_wh` | setup reply: energy charged | |
| `charge_used_ah` | setup reply: charge used | |
| `charge_charged_ah` | setup reply: charge charged | |
| `mosfet_temp_c` | setup reply: MOSFET temperature | |
| `motor_temp_c` | setup reply: motor temperature | |
| `fault_code` | setup reply: fault code | the number |
| `adc_level1` | ADC reply: level 1 | |
| `adc_level2` | ADC reply: level 2 | |
| `odometer_m` | setup reply: odometer | |
| `board_uptime_ms` | setup reply: board uptime | |
| `gps_fix_time_utc` | latest fix: fix time | as `time_utc` |
| `gps_lat_deg` | latest fix | seven decimals |
| `gps_lon_deg` | latest fix | seven decimals |
| `gps_alt_m` | latest fix | two decimals |
| `gps_speed_m_s` | latest fix | two decimals |
| `gps_heading_deg` | latest fix | two decimals |
| `gps_accuracy_m` | latest fix | one decimal |

Position, board ID, number of boards, battery capacity and the two ADC voltages are received and not logged.

Speed and distance are the board's own figures, computed from the wheel diameter, gear ratio and pole count set on the board. The app holds no such constants. Until the mechanics team's measured values are on the board (a desk checklist item on the front page), every log's speed and distance are off by whatever the board's estimates are off by.

Tickets: [What the phone log contains](issues/08-what-the-phone-log-contains.md), [Board speed settings and firmware version](issues/14-board-speed-settings-and-firmware-version.md), [Testing without the car](issues/16-testing-without-the-car.md). The behaviour when the file cannot be written was decided while writing this spec.

### 2.9 The team code

- The phone generates the team code on first launch, in the shape and with the check character the hub spec's "Team code and hub identities" gives, and keeps it across runs in the app's private settings. It is not put in the keystore: it crosses the internet in clear text on every connection.
- Setup shows the code with one action, "New code". It asks first (3.5), because it locks out every laptop, and it is unavailable during a run.
- A code cannot be typed into the phone. A replacement phone gets its own code and the laptops are updated. Reinstalling the app makes a new code. So does the debug build, which is a separate app with its own settings (2.12).
- The lobby ID, the viewer IDs and the password are derived from the code as the hub spec says.

Tickets: [How hub credentials are set and shared](issues/10-how-hub-credentials-are-set-and-shared.md).

### 2.10 Hub host and port

- Setup has a host and a port, pre-filled with the public hub's from the hub spec, edited by hand.
- They cannot be changed during a run. Moving to a self-hosted hub is Stop, edit, Start, which splits the log of record into two files.
- When they differ from the public hub's, the Hub row on the main screen shows them (3.1). Otherwise the main screen does not show them.
- The team code is unaffected by a change of host.

Tickets: [Self-run hub fallback](issues/17-self-run-hub-fallback.md), [How hub credentials are set and shared](issues/10-how-hub-credentials-are-set-and-shared.md).

### 2.11 Screens

Three screens: main, Logs and Setup. Their rows and strings are in section 3. The chosen prototype is variant D of [assets/09-phone-screen/prototype.html](assets/09-phone-screen/prototype.html); open it with `?variant=D`.

- **Main:** the redLINK wordmark with the run's duration beside it; any notices; one row each for Vehicle, Hub, Viewers, GPS and Log, each a coloured dot, the label and a short state; then two plain rows, Logs and Setup, with no subtitles; one wide Start/Stop button pinned to the bottom. The GPS row appears only during a run.
- **No live values:** no speed, voltage or other telemetry anywhere in the app. Nobody reads the phone while driving.
- **Logs:** the number of logs and their total size; during a run, the log in progress, which cannot be selected, shared or deleted; then past logs, newest first, each with its start time, duration and size. One or several can be selected, then shared through the share sheet as CSV files, or deleted after a confirmation. A log's duration is the `elapsed_s` of its last row. A simulated run's entry is marked "SIM".
- **Setup:** the grants; the picked board with its firmware version; the hub's host and port; the team code; the app's version and the update line. In debug builds, the simulated board's two entries (2.12). The board, the hub and the team code are locked during a run.
- **Look:** dark only, on a pure black background. redLINK red `#E11D48` for the wordmark and the Start button. Stop is a quiet dark button. Dots are green, amber, red or grey. Stock Material 3 components; not the Material 3 Expressive look.
- **Language:** every string in the phone app is English. The Turkish-strings rule of the specs applies to the desktop app only.

Tickets: [What the phone screen shows](issues/09-what-the-phone-screen-shows.md), [What the phone log contains](issues/08-what-the-phone-log-contains.md). That the phone app is English was confirmed while writing this spec.

### 2.12 The simulated board and the debug build

- **Debug build:** builds from Android Studio have the application ID `org.metucet.redlink.debug` and the name "redLINK dev", so they install beside the release app and have their own settings and team code. A laptop set up for the race phone cannot join a debug build's run by accident.
- **Simulated board:** a second implementation of the board link's seam (2.5) that answers `COMM_GET_VALUES_SETUP`, `COMM_GET_DECODED_ADC` and `COMM_FW_VERSION` with generated values. Its code is in the debug source set of `android/`; a release build does not contain it. There is no separate build flavour and no emulator detection.
- **Choosing it:** in debug builds the board picker on Setup offers "Simulated board" beside the scanned boards. With it picked, the Bluetooth grant is not needed (2.4), and a run uses the real service, hub connection, location, log and screens.
- **Marking:** the Vehicle row reads "Simulated" where it would read "Connected", the log file gets its `SIM_` prefix, and the Logs screen marks the entry.
- **Generated values:** a fixed lap of about 2 minutes that repeats: accelerate, cruise, coast to a stop. Speed, currents, voltage sag, distance and watt-hours agree with each other; distance, odometer and energy only grow across laps. ADC levels follow the throttle. One fault code is raised for a few seconds once per lap. Values are a function of time since the run started and nothing else, so the same run always gives the same replies.
- **"Stop answering":** a switch on Setup in debug builds, usable during a run. While on, the simulated board answers nothing, which makes the board unreachable on the phone and on every viewer.
- **Emulator requirement:** a debug build on a stock Android Studio emulator image must complete a run on the simulated board against the public hub, with a viewer on the same laptop going live. This is how a viewer is tested with no phone.

Tickets: [Testing without the car](issues/16-testing-without-the-car.md), [Distribution](issues/15-distribution.md).

### 2.13 Version and the update line

- The app's version is stamped at build time: from the tag in the release workflow, and from the latest version tag with `-dev` added in a local build (the hub spec's "Protocol version" has the rule). Setup shows it.
- The version code is major x 10000 + minor x 100 + patch.
- The release app's application ID is `org.metucet.redlink`. It is installed by sideloading the APK from the GitHub Release, and a newer APK installs over an older one. Uninstalling discards the team code.
- **Update line:** when the app is opened with no run active, it asks GitHub for the repo's latest release (`https://api.github.com/repos/ulassekerci/redLINK/releases/latest`, which leaves out pre-releases). When that version is newer than its own, Setup shows one line that opens the Release page in the browser (3.3). It is never a notification and never on the main screen. A failed check says nothing. Nothing is checked during a run.

Tickets: [Distribution](issues/15-distribution.md).

### 2.14 Stack

- Kotlin, Jetpack Compose with Material 3, Nordic Android-BLE-Library 2.x. Kable was rejected.
- Minimum Android 12 (API 31), targeting the current API.
- The project is `android/` in this repo, with no tooling at the repo root. It reads `protocol/vectors.json` from the sibling directory in its tests.
- The reference phone is the driver's Samsung on Android 16.
- **Manufacturer checklist:** `README.md` carries the steps for settings the app cannot read, written for the reference phone during implementation: the app is put on the manufacturer's list of apps never put to sleep, and is not on any list of restricted or deep-sleeping apps. The 1-hour screen-off item on the front page's desk checklist is what proves them.

**Not established.** None of this was run on a phone; the front page's desk checklist is where each is found out.

- No official sentence says sockets, Bluetooth notifications and location survive Doze under a foreground service; it rests on AOSP source.
- How Android brings the service back after a kill and after a crash, and how fast. The behaviour in 2.3 is the requirement; the mechanism is implementation.
- How the reference phone's manufacturer skin treats the app.
- The board's MTU and characteristic properties, and whether 20 Hz holds over its Bluetooth link.
- Whether mobile carriers drop the idle lobby socket, and how soon keep-alive notices.

Tickets: [Android runtime and stack](issues/07-android-runtime-and-stack.md), [Android headless operation constraints](issues/02-android-headless-operation-constraints.md), [Repo layout and cutover](issues/12-repo-layout-and-cutover.md).

## 3. States and messages shown to the user

All strings are English and are given verbatim. Dot colours: green is working, amber is waiting or incomplete, red is broken, grey is idle.

### 3.1 Main screen

**Vehicle row**

| Condition | Text | Dot |
|---|---|---|
| no board picked | `No vehicle picked` | amber |
| no run | `Not connected` | grey |
| run, board answering | `Connected` | green |
| run, simulated board answering | `Simulated` | green |
| run, board answering with a fault | `Connected · fault OVER_TEMP_FET` (the fault's name) | red |
| run, board unreachable | `Reconnecting` | red |

**Hub row.** When host or port differ from the public hub's, `host:port` is shown as a second line under the label, in every condition.

| Condition | Text | Dot |
|---|---|---|
| no run | `Not connected` | grey |
| run, lobby registered | `Connected` | green |
| run, lobby lost, waiting to retry | `Reconnecting in 4 s` (the wait left) | red |
| run, Android reports no network | `No network` | red |

**Viewers row**

| Condition | Text | Dot |
|---|---|---|
| no run | `–` | grey |
| run, lobby not registered | `0 (hub down)` | amber |
| run, no viewers | `0 viewers` | amber |
| run, viewers | `1 viewer`, `3 viewers` | green |

**GPS row,** during a run only

| Condition | Text | Dot |
|---|---|---|
| a fix in the last 3 s | `Fix, ±4 m` (the fix's accuracy) | green |
| otherwise | `No fix` | amber |

**Log row**

| Condition | Text | Dot |
|---|---|---|
| no run | `Idle` | grey |
| run, writing | `Recording, 1.25 MB` (the file's size) | green |
| run, the file cannot be written | `Not writing` | red |

**Notices,** above the rows:

- Setup incomplete, tapping opens Setup: `Setup incomplete: 2 grants missing. Start is disabled. Open setup →` or `Setup incomplete: no vehicle picked. Start is disabled. Open setup →`.
- After a phone restart ended a run, shown once: `The last run ended when the phone restarted.` with `Dismiss`.

**Button:** `Start` (red, disabled while setup is incomplete) or `Stop` (dark).

**Duration** beside the wordmark, during a run: `12:44`, and `1:02:44` from one hour.

### 3.2 Logs screen

- Title `Logs`. Under it: `5 logs · 49.7 MB in Documents/redLINK`.
- The log in progress: `Current log`, with `12:44 · in progress, cannot be shared yet`.
- A past log: its start time, `2026-09-26 14:22`, with `41:00 · 10.2 MB`. A simulated run: `SIM 2026-09-26 14:22`.
- No logs: `No logs yet`.
- Actions: `Share 2`, `Delete 2` (disabled with nothing selected, the number is the selection's size), and `Select all` or `Select none`.

### 3.3 Setup screen

- Title `Setup`.
- **Grants,** headed `Grants (4 of 6)`: `Bluetooth`, `Precise location`, `Location "all the time"`, `Location services on`, `Battery optimisation exemption`, `Notifications`. Each shows `Granted` or a `Grant` button.
- **Board:** the board's Bluetooth name with its address under it and `Change vehicle`; or `No vehicle picked` and `Pick vehicle`. Once a version has been read: `Firmware 6.06`. In debug builds the picker also lists `Simulated board`.
- **Hub:** `veschub.vedder.se:65101` with `Edit`.
- **Team code:** `K7QM-3XPC` with `New code`.
- **Version:** `Version 1.2.0`. When a newer release exists: `Update available: 1.3.0`, which opens the Release page.
- **Debug builds only:** a `Stop answering` switch.

`Change vehicle`, `Edit` and `New code` are disabled during a run.

### 3.4 Notifications

- **During a run:** title `redLINK` with the run's duration counting beside it, text `Vehicle connected · 3 viewers` (the Vehicle row's text in lower case, then the Viewers row's text), and one action, `Stop`. Tapping `Stop` opens the app's main screen with the stop dialog showing; it does not stop the run by itself. Tapping the notification opens the main screen.
- **Log not being written:** title `redLINK is not logging`, text `The log file cannot be written. Viewers are not affected.` It is removed when writing works again.
- **Restart limit reached:** title `redLINK stopped`, text `The app restarted 3 times in a minute. The run has ended and the log is saved.`

### 3.5 Dialogs

- **Stop:** title `Stop redLINK?`, text `Bridging and logging end. The log is saved.`, buttons `Keep running` and `Save & stop`.
- **Delete logs:** title `Delete 2 logs?` (`Delete 1 log?`), text `The files are removed from Documents/redLINK. This cannot be undone.`, buttons `Cancel` and `Delete`.
- **New code:** title `Make a new team code?`, text `Every laptop must be given the new code before it can watch.`, buttons `Cancel` and `New code`.

The strings of the two extra notifications, of the `Not writing` state, of the new-code dialog, of the firmware, version and update lines and of the `SIM` mark were written with this spec. The rest are the prototype's.

## 4. Required automated tests

Four suites of local unit tests in `android/`, run with `./gradlew testDebugUnitTest` (the debug variant, because the simulated board is in the debug source set). They need no device, no emulator and no network, and no test opens a socket to a hub. They run locally and as the first step of the tag workflow, where a failure blocks the release.

- **`ProtocolVectorsTest`:** reads `protocol/vectors.json` and runs every frame case from the hub spec's "Vectors": each encodes or decodes to the expected fields, or is rejected or ignored as the case says.
- **`TeamCodeTest`:** runs every team-code case from the same file (validity, normalised code, lobby ID, password, viewer ID). Also: a generated code is 8 characters of the alphabet and passes its own check.
- **`BridgeLobbyTest`:** drives the bridge's hub logic against a fake socket seam and a fake clock, and covers the hub spec's list for the bridge: a lobby request opens a registration; a repeated request for the same token is a no-op; a malformed request is ignored; nothing is written before the first heartbeat; writing stops at 3 s and the registration closes at 10 s without a heartbeat; status is sent on the first heartbeat and on a board state change. Also: after the hub connection drops, only the lobby is registered again.
- **`LogWriterTest`:** runs the log writer on the simulated board with a fake clock and a fixed GPS fix for a set span, and compares the output with a fixed expected file byte for byte. Further cases: a missing reply leaves empty cells; with the board unreachable there is one row per fix; a resumed run appends without a second line of column names.

Not automated: the foreground service, the Bluetooth link, the grants and the screens. They are exercised by the emulator requirement in 2.12 and by the front page's desk checklist.

Tickets: [Testing without the car](issues/16-testing-without-the-car.md), [Distribution](issues/15-distribution.md).

## 5. Out of scope

- Live telemetry on the phone screen, and anything for the driver to read while driving.
- An iOS app.
- Starting a run automatically: on Bluetooth connect, at boot, or after a phone restart.
- Typing an existing team code into the phone, a QR code, and keeping the code in the keystore.
- Deleting logs automatically, sharing the log in progress, and a log format VESC Tool can read.
- Trips. The trip meter is the desktop app's.
- The Play Store, and updating the app from inside the app.
- A light theme.
- A simulated board in release builds, a BLE peripheral that replays replies, and simulated GPS.
- Forwarding anything from a viewer to the board, and any board command beyond the three the hub spec lists.
- Refusing or warning about a board firmware version.
