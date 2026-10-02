# Desktop app architecture

Type: grilling
Status: resolved
Blocked by: 04, 05, 13

Map: [Hub migration](../map.md)

## Question

The Electron renderer is the existing React dashboard with its data layer swapped. How is the hub client placed in the Electron app?

Decide: what runs in the main process versus the renderer; the shape of the data handed to the existing stores; what replaces the current source picker; how the direct Bluetooth connection to the board (no phone, used while testing the vehicle) is provided in Electron and how it polls there; which parts of `web/` are dropped (Socket.IO); where the trip meter and CSV export live now that each laptop has its own copy of the stream; how connection failures are shown.

Also carry this requirement from [Android runtime and stack](07-android-runtime-and-stack.md): a viewer waits a random 1 to 2 s between lobby attempts, so that 8 laptops returning together after the phone drops do not displace each other round after round.

And these facts from [Stale re-registration on the public hub](13-stale-re-registration-on-the-public-hub.md): the hub discards bytes sent in the same write as the login line; another party's slow connection can make it discard a viewer's bytes for up to 5 s or reset the viewer; of several lobby requests within about 100 ms only one arrives. Decide how the viewer sends its lobby request given that it cannot know when its login took effect.

And these from [What the phone log contains](08-what-the-phone-log-contains.md): the stream now carries `COMM_GET_VALUES_SETUP` (47) replies instead of `COMM_GET_VALUES` (4), so the viewer parses that reply, takes speed and distance from it, and drops the wheel, gear and pole constants in `web/src/utils/erpm.ts`; the direct Bluetooth path polls the same command. The log of record knows nothing about trips, so the trip meter is the viewer's alone.

And these from [How hub credentials are set and shared](10-how-hub-credentials-are-set-and-shared.md): the app asks for the team code on first launch and keeps it, with the hub's host and port, in its settings; it rejects a code whose check character is wrong; the waiting screen shows the code in use; the viewer builds the lobby ID, its own ID and the password from the code and an 8-character token made fresh for each lobby visit.

## Answer

The main process owns the hub; the renderer stays a display and owns only the direct link. Decided by grilling on 2026-10-02. Nothing here was built or run.

- **Main process:** socket, login, lobby visit, `PING`, heartbeat, retry, deframing and parsing all live in main. A byte pipe with the state machine in the renderer was rejected: main must deframe anyway to see status messages, and timers in a backgrounded window are throttled, which would starve the heartbeat.
- **One protocol module:** framing, CRC and the parsers are one plain TypeScript module with no Electron or DOM imports, used by main for the hub and by the renderer for the direct link.
- **What main hands the renderer:** three kinds of sample, each stamped with its arrival time in main: a board sample (the `COMM_GET_VALUES_SETUP` fields, named as in the log of record: `speed_m_s`, `distance_abs_m`, `energy_used_wh` and so on), an ADC sample and a GPS fix; plus the connection state whenever it changes. The vehicle store is renamed to match. `useVehicleData` keeps the km/h conversion and the trivial derived values.
- **Direct link:** Web Bluetooth stays in the renderer. Main answers Electron's `select-bluetooth-device` event by picking the first device whose name starts with `redBLE`, so there is no chooser. A native BLE library in main was rejected: a native module to build and sign for two systems. The renderer polls as the phone does: `COMM_GET_VALUES_SETUP` (47) then decoded ADC (32) on a 50 ms cycle, giving up on a command after 250 ms, board unreachable after 2 s of silence. It writes the same three samples into the same store. There is no GPS on this path.
- **Hub and direct link are exclusive:** starting a direct link makes main leave the hub, so heartbeats stop and the bridge closes the registration after 10 s. The disconnect action returns to the lobby. If the Bluetooth link drops on its own the app stays on the direct link, dims the gauges, says board unreachable and retries the same device every 2 s.
- **No source picker:** the app joins on launch with no click. While not live the middle section shows the logo, one status line, the team code in use and a "connect to board directly" action. Once live it shows the trip meter. On a direct link the trip meter carries a "direct link" label and the disconnect action. Settings (team code, host, port, and the direct link action again so it is reachable while live) is its own screen, opened automatically on first launch when there is no code.
- **A lobby visit:** `PING` the lobby; on `NULL` the phone is not found. On `PONG`: connect, write the login line, then write the lobby request as separate writes every 100 ms for about 500 ms and close. Then `PING` the viewer's own ID every 250 ms for 2 s; on `PONG` attach and start the heartbeat, otherwise wait a random 1 to 2 s and visit again with a fresh token. Sending once and relying on the retry was rejected: repeats ride out the short windows in which the hub discards bytes. Staying longer was rejected because the lobby holds one viewer at a time.
- **Waiting with nothing to join:** random 1 to 2 s between attempts for the first minute, then every 5 s for as long as the app is open. No give-up and no retry button. A `PONG` from the lobby restores the fast pace.
- **States on the status line,** in Turkish like the rest of the dashboard: no team code; hub unreachable; phone not found (also what a code from the wrong phone looks like); joining; live; board unreachable; phone lost (no status for 3 s, rejoining); protocol version newer than this app knows. In board unreachable and phone lost the gauges keep their last values, dimmed, and the trip meter stays up. On a newer protocol version the app says to update and shows nothing from the stream.
- **Trip meter:** each laptop has its own, in memory only: lost when the app restarts, kept across phone lost and rejoin. Inputs are the board's absolute distance and watt-hours; Space starts a new trip. If distance or energy falls below the trip's baseline (the board was power-cycled), the baseline moves to the new value instead of the trip going negative. Laptops will show different trips; the log of record is the shared truth.
- **CSV export:** the log of record's column names and units for every field the viewer has, with `time_utc` being the laptop's arrival time. One row per board sample, covering the current trip (a new trip clears it), held in memory, about 72,000 rows an hour. Cmd/Ctrl+S opens a native save dialog with the default name `redLINK_<local time>.csv`.
- **Dropped from `web/`:** `socket.io-client`, `services/socket`, `store/socket.ts`, `Sources.tsx`, `utils/erpm.ts`, `commands/get-values.ts` (replaced by a setup-values parser), the `Mock` device CRC bypass and `bluetooth/test.ts`. Kept: gauges, bottom section, fault warning, map, trip meter, `uiState`.
- **Shell:** one window; closing it quits the app on both systems. The renderer runs with context isolation and reaches main through a small preload API: subscribe to samples and state, read and write settings, start and stop the direct link, save the CSV. `BrowserRouter` becomes `HashRouter`. Settings are a JSON file in Electron's user-data folder, owned by main.

Handed on: the bridge must treat a repeated lobby request for a token that already has a registration as a no-op; this amends [What travels on the stream](05-what-travels-on-the-stream.md) and belongs in the Android spec. Build tooling, packaging, signing and the macOS Bluetooth permission text stay with Distribution on the map. Directory names and whether `web/` is copied or converted go to [Repo layout and cutover](12-repo-layout-and-cutover.md).

Added to `CONTEXT.md`: **Direct link**.

Amended 2026-10-02 by [Distribution](15-distribution.md): the state "protocol version newer than this app knows" becomes "version mismatch". It applies when the bridge's major version differs from the app's in either direction; the status line states both versions and recommends no action. The app still shows nothing from the stream in that state.

Amended 2026-10-02 by [Testing without the car](16-testing-without-the-car.md): the app takes no single-instance lock, so several viewer windows can run on one laptop.

Amended 2026-10-02 by [Write the desktop app spec](21-write-the-desktop-app-spec.md): main no longer picks the first device whose name starts with `redBLE`. The app draws its own device list from the `select-bluetooth-device` event and the person picks; "so there is no chooser" no longer holds.
