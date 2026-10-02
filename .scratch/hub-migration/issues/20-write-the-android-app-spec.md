# Write the Android app spec

Type: task
Status: resolved
Blocked by: 19

Map: [Hub migration](../map.md)

## Question

Write `.scratch/hub-migration/spec-android.md`: the phone app as bridge and logger.

Cover: the foreground service, grants and what a run is; reconnect behaviour; the bridge's side of the lobby and registrations (citing the hub spec for the wire format, not restating it); the log of record; the main, Logs and Setup screens; team code generation; the simulated board and the debug build; the update line; the stack; the Android automated tests.

Sources: [Android headless operation constraints](02-android-headless-operation-constraints.md), [Android runtime and stack](07-android-runtime-and-stack.md), [What the phone log contains](08-what-the-phone-log-contains.md), [What the phone screen shows](09-what-the-phone-screen-shows.md), [How hub credentials are set and shared](10-how-hub-credentials-are-set-and-shared.md), [Board speed settings and firmware version](14-board-speed-settings-and-firmware-version.md), [Distribution](15-distribution.md), [Testing without the car](16-testing-without-the-car.md), [Self-run hub fallback](17-self-run-hub-fallback.md), and the assets in `../assets/09-phone-screen/`.

Follow [Where the specs live and how they are split](18-where-the-specs-live-and-how-they-are-split.md) for the path, the restate-not-link rule, the section layout and what to do on finding a contradiction. Use the vocabulary in `CONTEXT.md`.

## Answer

[`spec-android.md`](../spec-android.md) exists and no section is left open. Written on 2026-10-02.

- **Decided with the user while writing:**
  - The phone app's strings are English, as in the prototype. The "user-facing strings stay Turkish" rule in [Where the specs live and how they are split](18-where-the-specs-live-and-how-they-are-split.md) applies to the desktop app only.
  - The bridge sends `COMM_FW_VERSION` once after each Bluetooth connect and Setup shows the result ("Firmware 6.06"). It is not logged, not sent to viewers, and no version is refused or warned about. This settles what [Board speed settings and firmware version](14-board-speed-settings-and-firmware-version.md) left open, and it changed [`spec-hub.md`](../spec-hub.md): a third board command in 2.3, two more vectors (`request-fw-version`, `reply-fw-version`), and "three" in its out-of-scope line.
  - Stop on the notification opens the app on the "Stop redLINK?" dialog; it does not stop the run by itself.
  - When the log file cannot be written the run and the bridge continue, the Log row turns red ("Not writing"), a notification says so and the write is retried every second.
- **Filled in without asking,** each small and marked in the spec where it sits:
  - A field a GPS fix lacks is sent as 0 and logged empty.
  - Viewers counted on the screen are the active registrations (a heartbeat in the last 3 s).
  - A run found stored as active on the same boot, with no service running, is resumed when the app is opened. A phone restart is told apart by the boot count, and the app does not start at boot.
  - A log's duration on the Logs screen is the `elapsed_s` of its last row; a simulated run's entry is marked "SIM"; the file name is `SIM_redLINK_<start>.csv`.
  - Decimals per log column follow the wire's resolution.
  - Strings the prototype did not have: the two extra notifications, the new-code dialog, the firmware, version and update lines.
  - Four named test suites: `ProtocolVectorsTest`, `TeamCodeTest`, `BridgeLobbyTest`, `LogWriterTest`, run with `./gradlew testDebugUnitTest`.
- **Left to implementation:** which location API is used, under the condition that it works on a stock emulator image and on the reference phone; how Android brings the service back after a kill or crash.
- **For [Write the spec front page](22-write-the-spec-front-page.md):** the Android spec puts the manufacturer checklist in `README.md` and names the tag workflow's Android test command; the front page's desk checklist is cited for everything not established on a phone.
- **For [Write the desktop app spec](21-write-the-desktop-app-spec.md):** the direct link may ignore `COMM_FW_VERSION`; the hub spec does not ask the desktop to send it.
