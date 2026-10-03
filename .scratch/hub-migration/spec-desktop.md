# Desktop app spec

Part of the [hub migration spec](spec.md). Read the front page first, then the [hub usage spec](spec-hub.md): this file cites it for every message name, state name and timing on the hub and does not restate them.

Vocabulary is from `CONTEXT.md`. Where this spec and a ticket disagree, this spec wins.

## 1. Purpose

The desktop app is what the pit crew watches. On each laptop it joins the bridge through the hub by itself, as a viewer, and shows the board's stream on today's dashboard: gauges, ADC, map, trip meter and CSV export. Nothing is added to that dashboard and nothing is sent to the board. The same app can also connect straight to the board over Bluetooth, the direct link, for testing the vehicle with no phone. It runs on macOS and Windows.

The app is the existing React dashboard in `web/`, renamed to `desktop/` and put inside Electron, with its data layer replaced. What the dashboard shows and how it looks stay as they are unless this spec says otherwise.

## 2. Behaviour

### 2.1 Shell

- **One window.** Closing it quits the app, on macOS as on Windows.
- **No single-instance lock.** Starting the app again opens a second, independent instance. Eight windows on one laptop are eight viewers, each with its own token, sharing one settings file and one team code. This is how several viewers are tested on a desk.
- **Processes:** the main process owns the hub client and the settings file. The renderer is the dashboard and owns the direct link. The renderer runs with context isolation and without Node integration, and reaches main only through the preload API in 2.3.
- **Routing:** `BrowserRouter` becomes `HashRouter`, because the packaged app is loaded from a file. Routes: the gauges, the map and settings.
- **Fonts:** the Inter font is bundled with the app, not loaded from `rsms.me` as `web/index.html` does, so a direct link in a workshop with no internet looks the same. Map tiles still come from the network (2.9).

Tickets: [Desktop app architecture](issues/11-desktop-app-architecture.md), [Testing without the car](issues/16-testing-without-the-car.md). Bundling the font was decided while writing this spec.

### 2.2 The hub client

The viewer's whole behaviour on the hub is in the hub spec: "Joining, viewer side", "Our messages", "Protocol version", "Team code and hub identities" and the state table in its section 3. The hub client implements those as written. What is specific to this app:

- **It lives in main.** Socket, login line, lobby visit, `PING`, heartbeat, waiting between visits, deframing and parsing are all in the main process, on Node's `net` sockets. The renderer never sees bytes. A backgrounded window throttles its timers, which would starve the heartbeat; main's are not throttled.
- **It starts on launch.** With a team code stored, the app begins its first lobby visit as soon as it starts, with no click, and keeps trying for as long as it is open. There is no source picker, no connect button and no retry button.
- **It stops** while there is no team code, and while a direct link is in use (2.5).
- **Seams:** the hub client takes its socket factory, its clock and its random source from outside, so the tests in section 4 drive it with scripted bytes and a fake clock. Nothing else in the app opens a socket to the hub.
- **Protocol version:** the app's own major version (2.11), compared with the byte in the status message.
- **Unknown input:** a frame with a command ID the app does not handle, a `COMM_CUSTOM_APP_DATA` message of an unknown type and extra bytes at the end of a known message are ignored, as the hub spec's tolerance rules say. `COMM_FW_VERSION` is never sent and a reply to it is ignored.
- **What it sends:** the lobby request and the heartbeat. Nothing else is ever written to the hub.

Tickets: [Desktop app architecture](issues/11-desktop-app-architecture.md), [How desktop VESC Tool behaves as a hub client](issues/03-how-desktop-vesc-tool-behaves-as-a-hub-client.md) (dispatch by command ID alone), [How several pit laptops watch at once](issues/04-how-several-pit-laptops-watch-at-once.md).

### 2.3 What main hands the renderer

Main sends the renderer parsed samples and one connection state. The preload script exposes exactly this:

- subscribe to samples;
- subscribe to the connection state, and read it once on start;
- read and write the settings (2.7);
- subscribe to the list of Bluetooth devices found by a scan, and pick one or cancel (2.5);
- tell main that a direct link has started or ended (2.5);
- save the CSV (2.10);
- read the app's version and the update line (2.11).

**Samples,** each stamped with its arrival time in main (the laptop's clock, Unix milliseconds):

| Sample | From | Fields |
|---|---|---|
| board sample | a `COMM_GET_VALUES_SETUP` reply | every field of the reply, named as the log of record names its columns: `speed_m_s`, `distance_m`, `distance_abs_m`, `erpm`, `duty_cycle`, `battery_voltage_v`, `battery_current_a`, `motor_current_a`, `battery_level`, `energy_used_wh`, `energy_charged_wh`, `charge_used_ah`, `charge_charged_ah`, `mosfet_temp_c`, `motor_temp_c`, `fault_code`, `odometer_m`, `board_uptime_ms` |
| ADC sample | a `COMM_GET_DECODED_ADC` reply | `adc_level1`, `adc_voltage1`, `adc_level2`, `adc_voltage2` |
| GPS fix | a GPS message | `gps_fix_time_utc`, `gps_lat_deg`, `gps_lon_deg`, `gps_alt_m`, `gps_speed_m_s`, `gps_heading_deg`, `gps_accuracy_m` |

- A field missing from a short setup reply is absent from the sample, not zero.
- Position, board ID, number of boards and battery capacity are parsed and not handed on.
- In version mismatch main hands the renderer no samples.

**Connection state:** one value, sent whenever it changes: one of the eight states in the hub spec's section 3, with the two version numbers when the state is version mismatch.

**Stores:** the vehicle store holds the latest of each of the three samples and is renamed to match these field names. `useVehicleData` keeps the derived values the dashboard shows:

- speed in km/h: `speed_m_s` x 3.6;
- power: battery voltage x battery current;
- motor voltage: battery voltage x duty cycle;
- net energy: energy used minus energy charged.

Speed and distance are the board's own figures. The wheel, gear and pole constants in `utils/erpm.ts` are gone, and the app computes neither from ERPM or tachometer counts.

Tickets: [Desktop app architecture](issues/11-desktop-app-architecture.md), [What the phone log contains](issues/08-what-the-phone-log-contains.md).

### 2.4 The protocol module

- Framing, the CRC, the request encoders, the reply parsers, our four messages and the team code functions (check character, normalising a typed code, lobby ID, viewer ID, password) are one plain TypeScript module inside `desktop/`, with no Electron, Node or DOM imports.
- Main uses it for the hub and the renderer uses it for the direct link, so both paths share one parser.
- Its decoder buffers and scans as the hub spec's "Framing" says. This replaces `services/bluetooth/packet.ts`, which treats one Bluetooth notification as one whole frame.
- It is what the vectors test in section 4 runs against.

Tickets: [Desktop app architecture](issues/11-desktop-app-architecture.md), [Repo layout and cutover](issues/12-repo-layout-and-cutover.md).

### 2.5 The direct link

The direct link connects the app to the board over Bluetooth with no bridge and no hub. It is for testing the vehicle. An app on a direct link is not a viewer.

- **Where it runs:** Web Bluetooth in the renderer, on the Nordic UART service (`6e400001-b5a3-f393-e0a9-e50e24dcca9e`), writing to the RX characteristic (`6e400002-…`) and listening on the TX characteristic (`6e400003-…`), as `web/` does today. A native Bluetooth library in main is not used.
- **Starting it:** the "connect to board directly" action, on the settings screen only (3.4), calls `navigator.bluetooth.requestDevice()` with the Nordic UART service as its only filter, which starts a scan. Devices are not filtered by name, as the phone's picker does not: the car, the spare VESC and any other board offering the service all appear. Today's `redBLE` name filter is dropped.
- **Picking the board:** the person picks. Electron has no built-in chooser, so the app draws its own: main receives Electron's `select-bluetooth-device` event, which repeats with the devices found so far, and passes the list to the renderer, which shows it in the middle section (3.2). Clicking an entry makes main answer the event with that device; cancelling makes main answer with none, which ends the scan. A device that reports no name is listed by its ID. The list is shown even when it has one entry, and nothing is picked automatically. The scan has no time limit: it runs until a pick or a cancel. While the list is open the app stays on the hub as it was.
- **Hub and direct link are exclusive.** When a board has been picked, the renderer tells main, and main leaves the hub: it closes its socket and stops visiting the lobby. Heartbeats stop, so the bridge closes the registration by itself. From then on the store is written only by the direct link.
- **Polling:** the renderer polls as the bridge does, with the cycle and the 250 ms and 2 s timeouts of the hub spec's "Polling and the stream": `COMM_GET_VALUES_SETUP`, then `COMM_GET_DECODED_ADC`, every 50 ms. It writes board samples and ADC samples into the same store, stamped on arrival. It does not send `COMM_FW_VERSION`.
- **No GPS.** There are no GPS fixes on a direct link; the map says so (2.9).
- **No protocol version.** The direct link talks to the board, not the bridge, so no version is compared.
- **When the Bluetooth link drops by itself,** or the board stops answering for 2 s, the app stays on the direct link: the gauges keep their last values, dimmed, the status line says the board is not answering (3.1), and the renderer reconnects to the same device every 2 s. It does not fall back to the hub.
- **Ending it:** the disconnect action (3.2). The renderer closes the Bluetooth link and tells main, which starts visiting the lobby again.
- **The seam:** the polling loop talks to a transport object (write bytes, receive bytes, connected or not). Web Bluetooth is one implementation; the tests in section 4 use a fake one. There is no selectable simulated board in the desktop app.
- **Removed from `web/`:** the `Mock` device name that skipped the CRC check, and `bluetooth/test.ts`.

Tickets: [Desktop app architecture](issues/11-desktop-app-architecture.md), [Testing without the car](issues/16-testing-without-the-car.md), [What the phone log contains](issues/08-what-the-phone-log-contains.md). The device list replaces that ticket's "first device whose name starts with `redBLE`", which picked wrongly with more than one board powered; it, the filter by service instead of by name, and staying on the hub until a board is picked were decided while writing this spec.

### 2.6 The team code

- The phone generates the team code; the pit crew types it into each laptop once. This app never generates one.
- **First launch:** with no code stored, the app opens on the settings screen.
- **Typing:** lower case is accepted, dashes and spaces are ignored, and a code whose check character is wrong is rejected with a message (3.4) and not stored. The rules and the check character are the hub spec's "Team code and hub identities".
- **Changing it:** on the settings screen, at any time. Saving a different code makes main leave the hub and join with the new one.
- **Where it is shown:** on the waiting screen (3.2) as `XXXX-XXXX`, so a laptop set up with another phone's code can be spotted. A valid code from the wrong phone looks the same as the phone not running.
- **Derived values:** the lobby ID, the viewer ID, the password and the token are built as the hub spec says, in the protocol module.

Tickets: [How hub credentials are set and shared](issues/10-how-hub-credentials-are-set-and-shared.md).

### 2.7 The settings file

- One JSON file, `settings.json`, in Electron's user-data folder, read and written by main only. It holds the team code (without its dash), the hub host and the hub port.
- It is plain text. The keychain and the Windows credential store are not used: the code crosses the internet in clear text on every connection.
- A missing or unreadable file is treated as no settings: no team code, and the public hub's host and port.
- Several instances share the file. An instance reads it on launch and when it saves; a change saved in one window reaches the others when they are restarted.
- The gauge style toggle (`uiState`) stays where it is, in the renderer's local storage.

Tickets: [Desktop app architecture](issues/11-desktop-app-architecture.md), [How hub credentials are set and shared](issues/10-how-hub-credentials-are-set-and-shared.md). The file's name, and that other windows see a change only on restart, were decided while writing this spec.

### 2.8 Hub host and port

- Settings has a host and a port, pre-filled with the public hub's from the hub spec, edited by hand. Moving to a self-hosted hub is typing its host on each laptop.
- They can be changed at any time, unlike on the phone. Saving a change makes main leave the hub and join on the new host.
- A host field left empty, or a port that is not a number from 1 to 65535, is saved as the public hub's, and the field then shows it. There is no message: clearing the host is how a laptop is put back on the public hub.
- When they differ from the public hub's, the waiting screen shows `host:port` beside the team code (3.2). A laptop left on the old host otherwise looks the same as the phone not running. When they are the public hub's they are shown only in settings.
- The team code is unaffected by a change of host.

Tickets: [Self-run hub fallback](issues/17-self-run-hub-fallback.md), [How hub credentials are set and shared](issues/10-how-hub-credentials-are-set-and-shared.md). Saving an empty host as the public hub's was decided after ticket 29, on seeing the app.

### 2.9 The dashboard

Today's dashboard is the feature bar. Kept as they are: the two gauges with their outer rings, the bottom section, the fault warning, the map and the trip meter.

- **Gauges:** the left gauge shows speed in km/h with duty cycle on its outer ring and the board's absolute distance in metres under the number. The right gauge shows power in watts with ADC level 1 on its outer ring and energy used in watt-hours under the number.
- **Bottom section:** battery percentage, battery voltage and battery current on the left; motor current, motor voltage and MOSFET temperature on the right. The battery percentage stays the one computed from the pack voltage with the Aspilsan cell curve (`utils/aspilsan.ts`), not the board's battery level. In the middle: the switch between gauges and map, and the settings entry, which replaces today's Bluetooth/socket switch.
- **Fault warning:** a non-zero fault code shows the warning banner with the fault's name: the firmware 6.06 fault code name without the `FAULT_CODE_` prefix, as on the phone. A code the app has no name for is shown as its number.
- **Map:** the latest GPS fix as a marker on the existing map style. With no fix yet, and always on a direct link, the map says there is no location (3.3). The style, tiles, sprites and glyphs are fetched from `tiles.openfreemap.org`, so the map needs internet; a viewer has it by definition.
- **Middle section:** while the app is not live it is the waiting screen. Once live it is the trip meter. Section 3.2 has the detail.
- **Dimming:** in board unreachable and phone lost, and on a direct link whose board is not answering, the gauges and the bottom section keep their last values at reduced opacity, and the trip meter stays up. In version mismatch nothing from the stream is shown.
- **Before the first sample** everything reads zero, as today.

**Dropped from `web/`:** `socket.io-client`, `services/socket`, `store/socket.ts`, `components/MiddleSection/Sources.tsx`, `utils/erpm.ts`, `services/bluetooth/commands/get-values.ts` (a setup-values parser in the protocol module replaces it), `services/bluetooth/test.ts`, the `Mock` device CRC bypass, and the `VITE_SOCKET_URL` setting.

Tickets: [Desktop app architecture](issues/11-desktop-app-architecture.md), [What the phone log contains](issues/08-what-the-phone-log-contains.md). Keeping the Aspilsan percentage, and showing fault names without their prefix, were decided while writing this spec.

### 2.10 The trip meter and the CSV export

**Trip meter.** Each laptop has its own, in the renderer's memory. It is lost when the app quits. It is kept across phone lost and rejoining, and across a switch between the hub and a direct link.

- **Rows:** distance, time, average speed and consumption (km/kWh), as today.
- **Inputs:** the board's absolute distance, energy used and energy charged. A trip is a baseline of those three plus a start time; the rows are the current values minus the baseline.
- **New trip:** Space sets the baseline to the current values and the start time to now. Before the first Space the baseline is zero and the time row reads `00:00:00`, as today. Space does nothing while a text field has the focus.
- **Board power-cycled:** if the absolute distance or either energy value falls below its baseline, that baseline moves to the new value, so a row never goes negative. The trip loses what the board had counted since the baseline; it does not jump.
- Laptops will show different trips. The log of record knows nothing about trips and is the shared truth.

**CSV export.** A convenience copy of what this laptop saw. The log of record on the phone is authoritative.

- **Rows:** one per board sample, held in memory, covering the current trip: a new trip clears them. About 72,000 rows an hour. Nothing is written to disk until a save.
- **Columns:** the log of record's column names, units and order (the Android spec's "The log of record"), with these differences:
  - `time_utc` is the laptop's arrival time for the board sample.
  - `elapsed_s` is left out: a viewer does not know when the run started.
  - `adc_level1` and `adc_level2` are from the latest ADC sample, and the GPS columns from the latest GPS fix, repeated on every row. On a direct link the GPS cells are empty. A field the phone's fix lacked reads 0 here, as the GPS message carries it, where the log of record leaves the cell empty.
  - `power_w` is computed on the laptop, as on the phone.
- **Layout:** comma-separated, decimal points, the column names as the first line, an empty cell for a missing value.
- **Saving:** Cmd+S on macOS and Ctrl+S on Windows open the system's save dialog through main, with the default name `redLINK_<local time>.csv`, for example `redLINK_2026-09-26_14-22-09.csv`. With no rows nothing happens. Saving does not clear the rows.
- Rows are collected on a direct link as on the hub, and not in version mismatch.

Tickets: [Desktop app architecture](issues/11-desktop-app-architecture.md), [What the phone log contains](issues/08-what-the-phone-log-contains.md). Leaving out `elapsed_s`, and keeping the trip across a switch between hub and direct link, were decided while writing this spec.

### 2.11 Version and the update line

- The app's version is stamped at build time: from the tag in the release workflow, and from the latest version tag with `-dev` added in a local build (the hub spec's "Protocol version" has the rule). Settings shows it.
- **Update line:** on launch the app asks GitHub for the repo's latest release (`https://api.github.com/repos/ulassekerci/redLINK/releases/latest`, which leaves out pre-releases). When that version is newer than its own, one line is shown on the waiting screen and in settings (3.2, 3.4), and clicking it opens the Release page in the browser. A failed check says nothing. The check is made once per launch. The app never downloads or installs anything itself.

Tickets: [Distribution](issues/15-distribution.md). Showing the line on the waiting screen as well as in settings was decided while writing this spec: the pit crew does not open settings after the first launch.

### 2.12 Stack and packaging

What is the app's own. The release workflow, the tags and the version scheme are on the front page.

- **Stack:** Electron with `electron-vite` and electron-builder; the renderer keeps React, Zustand, Tailwind, `motion`, MapLibre and `react-router`. Electron Forge is not used.
- **Project:** `desktop/` in this repo, a standalone package on pnpm with no workspace and no tooling at the repo root. `desktop/package.json` pins pnpm in `packageManager` and allows Electron's install script; `desktop/.npmrc` sets `node-linker=hoisted`. These two pnpm settings were written from memory and must be checked on the first build.
- **Names:** the product name is `redLINK`; the bundle ID is `org.metucet.redlink`.
- **macOS:** arm64 only, a `dmg`, ad-hoc signed and not notarized. `NSBluetoothAlwaysUsageDescription` is `redLINK, araca doğrudan bağlanmak için Bluetooth kullanır.`
- **Windows:** x64 only, a zip that is unzipped anywhere and run in place. No installer and not the single-file portable target.
- **First open:** `README.md` carries the steps for the unsigned app: "Open Anyway" in macOS System Settings, "Run anyway" on Windows SmartScreen.
- **Not established.** None of this was built or run. Whether the board advertises the Nordic UART service, which a Web Bluetooth service filter needs in order to list it; whether Electron's Web Bluetooth holds 20 Hz against the board on both systems, and whether reconnecting to the same device without a new scan works on Windows, are found out on the front page's desk checklist.

Tickets: [Distribution](issues/15-distribution.md), [Repo layout and cutover](issues/12-repo-layout-and-cutover.md).

## 3. States and messages shown to the user

All strings are Turkish and are given verbatim. In Turkish the board is "araç", as the phone screen calls it "Vehicle".

### 3.1 The status line

One line at the top of the middle section. As a viewer it shows the hub spec's eight states:

| State | Text |
|---|---|
| no team code | `Takım kodu girilmedi` |
| hub unreachable | `Hub'a ulaşılamıyor` |
| phone not found | `Telefon bulunamadı` |
| joining | `Bağlanılıyor` |
| live | nothing |
| board unreachable | `Araç yanıt vermiyor` |
| phone lost | `Telefon bağlantısı koptu, yeniden bağlanılıyor` |
| version mismatch | `Telefon sürüm 1, bu uygulama sürüm 2` (the two major versions) |

Version mismatch recommends no action. Phone not found is also what a code from the wrong phone, or a laptop left on another hub's host, looks like.

On and around a direct link:

| Condition | Text |
|---|---|
| the device list is open | the viewer's state, unchanged |
| a board was picked, connecting | `Araca bağlanılıyor` |
| Bluetooth off or not permitted | `Bluetooth kullanılamıyor`, for 5 s, then the viewer's state again |
| direct link, board answering | nothing |
| direct link, board not answering | `Araç yanıt vermiyor, yeniden deneniyor` |

### 3.2 The middle section

| State | Shows |
|---|---|
| no team code, hub unreachable, phone not found, joining, version mismatch | the waiting screen |
| live, board unreachable, phone lost after having been live | the status line and the trip meter |
| direct link | the status line, the trip meter, the label `Doğrudan bağlantı` and the action `Bağlantıyı kes` |
| the device list is open, in any state | the status line and the device list |

**The device list:** the title `Araç seçin`; one row per device found, showing its Bluetooth name (or its ID when it has none), added as the scan finds them; `Araç aranıyor` while the list is empty; and `Vazgeç`, which cancels. Pressing the action from settings returns to the gauges, where the list is. The gauges and bottom section keep showing the stream while the list is open.

**The waiting screen:** the logo; the status line; the team code in use, `Takım kodu: K7QM-3XPC`; under it `host:port` when they are not the public hub's; and, when there is one, the update line `Güncelleme var: 1.3.0`.

**The trip meter's rows:** `Mesafe`, `Süre`, `Ort. Hız`, `Tüketim`, as today.

### 3.3 Gauges, bottom section and map

- Gauge units: `km/h` and `watt`.
- Bottom section entries: `Harita` on the gauges, `Göstergeler` on the map, and `Ayarlar`.
- Fault warning: title `Uyarı`, with the fault's name under it, for example `OVER_TEMP_FET`.
- Map with no fix: `Konum verisi yok`.

### 3.4 Settings

- Title `Ayarlar`, with `Geri` to return.
- `Takım kodu`: a text field. A rejected code: `Takım kodu hatalı. Telefondaki kodu kontrol edin.`
- `Hub adresi` and `Port`: two fields.
- `Kaydet` saves the three together.
- `Doğrudan bağlan`, or `Bağlantıyı kes` while on a direct link. Settings is the only place a direct link is started: it is rarely used, so it is kept off the waiting screen. `Bağlantıyı kes` is also in the middle section (3.2).
- `Sürüm 1.2.0`. When a newer release exists: `Güncelleme var: 1.3.0`, which opens the Release page.

`Uyarı`, `Harita`, `Göstergeler`, `Konum verisi yok`, the trip meter's rows and the gauge units are today's. Keeping `Doğrudan bağlan` off the waiting screen was decided after ticket 28, on seeing the app. The version mismatch line and the Bluetooth permission text are from [Distribution](issues/15-distribution.md). Every other string was written with this spec.

## 4. Required automated tests

Five suites in `desktop/`, run with `pnpm test` (Vitest). They need no Electron window, no Bluetooth and no network, and no test opens a socket to a hub. They run locally and as the first step of the tag workflow, where a failure blocks the release.

- **`protocol-vectors`:** reads `protocol/vectors.json` from the sibling directory and runs every frame case from the hub spec's "Vectors" against the protocol module: each encodes or decodes to the expected fields, or is rejected or ignored as the case says.
- **`team-code`:** runs every team-code case from the same file (validity, normalised code, lobby ID, password, viewer ID). Also: a generated token is 8 characters of the alphabet.
- **`hub-client`:** drives the hub client with scripted bytes at its socket seam, a fake clock and a fixed random source, and covers the hub spec's list for the viewer:
  - the lobby visit step by step, including that the lobby request is never in the same write as the login line and is repeated every 100 ms for about 500 ms;
  - the `PING` of the viewer's own ID every 250 ms for 2 s, and attaching on `PONG`;
  - the 1 to 2 s wait between visits, the change to 5 s after a minute, and a `PONG` from the lobby restoring the fast pace;
  - the heartbeat once a second, as its own write;
  - every state in the hub spec's section 3 and the moves between them, version mismatch included, in which the client stays attached, keeps its heartbeat and hands on no samples;
  - phone lost after 3 s without status, returning to the lobby with a fresh token and no `PING` of the old ID;
  - leaving the hub when a direct link starts and visiting the lobby again when it ends;
  - a changed team code or host making the client leave and join again.
- **`direct-link`:** drives the polling loop against a fake transport and a fake clock: the 50 ms cycle and the order of the two requests; giving up on a command after 250 ms; board not answering after 2 s of silence and at once on a disconnect; a reconnect attempt every 2 s; samples resuming when the transport answers again.
- **`trip-and-csv`:** a new trip sets the baseline and clears the rows; a value falling below its baseline moves the baseline and no row goes negative; the CSV has the columns of 2.10 in order, empty GPS cells with no fix, and one row per board sample.

Not automated: the Electron shell, Web Bluetooth itself, the settings screen and the dashboard's rendering. They are exercised by the front page's desk checklist, where each status-line state is produced once by hand and a macOS and a Windows laptop on a direct link each show sane values from a real board.

Tickets: [Testing without the car](issues/16-testing-without-the-car.md), [Distribution](issues/15-distribution.md). The `trip-and-csv` suite, the last two `hub-client` cases and Vitest were added while writing this spec.

## 5. Out of scope

- New dashboard features beyond today's: gauges, ADC, map, trip meter, CSV export.
- Sending anything to the board through the hub, and any control, configuration or firmware command. The dashboard is read-only.
- A source picker, a connect button and a retry button for the hub.
- Remembering the board picked for a direct link: every start shows the list.
- A simulated board in the desktop app, and a fake bridge script. A viewer with no phone is tested against the Android debug build on an emulator.
- GPS on a direct link, and reading the board's firmware version.
- Generating a team code, a QR code or a config file for entering one, and keeping the code in the keychain.
- A trip meter shared between laptops or kept across restarts, and writing the CSV to disk as samples arrive.
- A single-instance lock.
- Signing with a paid certificate, notarization, an installer on Windows, auto-update, and builds for Intel Macs, Windows on ARM or Linux.
- A native Bluetooth library in the main process.
- Reading an older or newer major's protocol. On a version mismatch the app shows nothing from the stream.
- An English or other translation of the app.
