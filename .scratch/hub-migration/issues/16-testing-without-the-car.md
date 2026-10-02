# Testing without the car

Type: grilling
Status: resolved

Map: [Hub migration](../map.md)

## Question

The acceptance session is a real track session with the car ([Repo layout and cutover](12-repo-layout-and-cutover.md)), but both apps must be exercisable on a desk before that. What stands in for the board and for the hub?

Decide: what plays the board for the Android app and for the desktop's direct link (a spare VESC on a bench supply, a BLE peripheral that replays recorded replies, or a fake behind the app's Bluetooth seam); whether desk tests use the public hub or a locally run stock hub; how a viewer is exercised with no phone at all; which of these stand-ins the specs require to be built, and where they live in the repo beside `protocol/vectors.json`.

## Answer

A simulated board inside the phone app's debug build stands in for the board, the public hub stands in for itself, and a viewer with no phone is tested against that debug build on an Android emulator. Decided by grilling on 2026-10-02. Nothing here was built or run.

- **What plays the board for the Android app:** a simulated board behind the app's Bluetooth seam, which answers `COMM_GET_VALUES_SETUP` and decoded ADC with generated values. It runs the real foreground service, hub connection, log and screen with no hardware, and it works on an emulator, which Bluetooth does not. A separate BLE peripheral that replays replies was rejected: a second device to maintain in order to test only the Bluetooth layer, which a real board tests better.
- **How it is chosen:** an entry on the Setup screen beside the real board, present only in debug builds ("redLINK dev"). One debug build then serves the emulator, a real phone on a desk, and a real board. The code lives in `android/` in the debug source set, so release builds do not contain it. A separate build flavour and automatic emulator detection were rejected.
- **Emulator requirement:** the Android spec requires that a debug build on a stock Android Studio emulator image completes a run against the public hub on the simulated board. With the simulated board selected, the Bluetooth grants are not asked for and do not block Start; the other grants behave as on a phone.
- **Marking a simulated run:** the log file name gets a `SIM_` prefix and the Vehicle row on the main screen reads "Simulated". The debug build has its own team code ([Distribution](15-distribution.md)), so a laptop set up for the race phone cannot join a simulated run by accident.
- **Generated values:** a fixed lap of about 2 minutes that repeats: accelerate, cruise, coast to a stop. Speed, current, voltage sag, distance and watt-hours agree with each other; distance and energy only grow across laps. ADC follows the throttle. One fault code is raised for a few seconds once per lap so the fault warning is seen. The same inputs always give the same outputs, so the log-writer test compares against a fixed expected file. Replaying a recorded log of record was left out: no `COMM_GET_VALUES_SETUP` recording exists yet.
- **GPS:** the simulated board plays only the board. GPS always comes from the real location provider: the emulator's route playback, or a fix that does not move on a desk phone. The real location code path is therefore exercised.
- **One debug control:** "stop answering" on the Setup screen, which produces board unreachable on the phone and on every viewer.
- **What plays the board for the desktop's direct link:** a real board only: the car on a stand with its wheels lifted, the spare VESC, or the team's ESP32 mimic. The desktop app has no selectable simulated board. The direct link's polling loop and timeouts are unit-tested against a fake transport object.
- **The ESP32 mimic:** left as it is, outside the specs and outside this repo. It answers the old polling; updating it for command 47 would be a convenience, not a deliverable.
- **Hub:** the public hub for every manual desk test. Its quirks (bytes discarded after login, one of several lobby requests arriving, strangers' connections) are what the lobby design was built around, and a quiet local hub would hide them. Automated tests never open a socket. A locally run hub is left to [Self-run hub fallback](17-self-run-hub-fallback.md).
- **A viewer with no phone:** the Android debug build on an emulator with the simulated board, on the same laptop as the viewer. A "fake bridge" script in `desktop/` was rejected: a second implementation of the bridge's lobby logic to keep in step with the real one.
- **Driving the viewer's states:** hub unreachable, phone not found, joining, live and phone lost are produced by hand (wrong host, Stop, Start, cutting the emulator's network); board unreachable by "stop answering"; version mismatch only by an automated test.
- **Several viewers on one desk:** the desktop app takes no single-instance lock, so one laptop runs 8 windows against one emulator or phone. Each makes a fresh token per lobby visit, so the hub sees 8 viewers. They share one settings file and one team code.
- **Automated tests the specs require:**
  - Both languages decode and encode every frame in `protocol/vectors.json`.
  - Desktop: the hub client's lobby visit, heartbeat, retry pacing and state transitions (including version mismatch) against scripted bytes at its socket seam; the direct link's polling timeouts against a fake transport.
  - Android: the bridge's lobby handling, including the repeated-request no-op, and the log writer against the simulated board.
  - They run locally and as the first step of the tag workflow, so a failing test blocks a release. No workflow runs on ordinary pushes.
- **Desk checklist,** passed before the acceptance session is booked:
  - A real phone of the race phone's model does a simulated run for 2 hours with the screen off, and its log has no gaps. This is where manufacturer app-killing, untested since [Android headless operation constraints](02-android-headless-operation-constraints.md), is caught before the track.
  - 8 viewer windows on one laptop stay live through that run, on macOS and on Windows.
  - Each status-line state is produced once by hand.
  - With the car on a stand and its wheels lifted, the phone and then one laptop on a direct link each connect to the board over Bluetooth and show sane values while the wheel turns.

  Passing the checklist is not the acceptance session, which keeps its definition.

Amends: [Android runtime and stack](07-android-runtime-and-stack.md) (Bluetooth grants do not block Start on the simulated board), [What the phone log contains](08-what-the-phone-log-contains.md) (`SIM_` file name prefix), [What the phone screen shows](09-what-the-phone-screen-shows.md) (Vehicle row reads "Simulated"; Setup gains two debug-only entries), [Desktop app architecture](11-desktop-app-architecture.md) (no single-instance lock) and [Distribution](15-distribution.md) (tests are the first step of the tag workflow).

Added to `CONTEXT.md`: **Simulated board**.

Amended 2026-10-02 by [Write the spec front page](22-write-the-spec-front-page.md): the desk checklist's long run is 1 hour, not 2, done once on mobile data with 8 viewer windows on one laptop; the other system's 8 windows and the status-line states move to a short second run, which also gains a kill-and-resume item; the direct link on the stand is tried on one macOS and one Windows laptop.
