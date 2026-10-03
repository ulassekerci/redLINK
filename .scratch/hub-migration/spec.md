# Hub migration spec

The front page. Read it first, then the part you are building:

- [Hub usage spec](spec-hub.md): the wire format and the protocol the bridge and the viewers run on the hub. Both app specs cite it.
- [Android app spec](spec-android.md): the phone app.
- [Desktop app spec](spec-desktop.md): the Electron app.

This file holds what belongs to no single part: the repo layout, the cutover, the release workflow, the version scheme, the desk checklist and the acceptance session.

Vocabulary is from `CONTEXT.md`. Each section ends with links to its tickets, for the reasoning and the rejected alternatives only. Where a spec and a ticket disagree, the spec wins.

## 1. The system

redLINK carries telemetry from a Shell Eco-marathon car to its pit crew. The board, a VESC motor controller, is polled over Bluetooth by an Android phone that rides in the car. During a run the phone app is the bridge: it copies the board's replies and the phone's GPS fixes through the hub, a third-party relay nobody on the team runs, to every viewer, and it writes the log of record, one CSV per run. A viewer is the desktop app on a pit laptop. It needs one team code typed in once, joins by itself, and shows the stream on today's read-only dashboard: gauges, ADC, map, trip meter and CSV export. Between 3 and 8 laptops watch at once with no shared network. The desktop app can also connect straight to the board over Bluetooth, the direct link, for testing the vehicle with no phone.

The migration replaces two things: a relay the team hosted itself, now shut down, and a React Native phone app that was unreliable in the car. The team writes no server code. The stream is pure VESC framing, so the hub path and the direct link share one parser. Compatibility with VESC Tool is not a goal: only our own apps attach to the bridge's registrations.

Android comes first. An iOS app is a later effort.

Tickets: the standing decisions in the [map](map.md)'s Notes.

## 2. Repo layout

After the cutover in section 3 the repo root holds:

| Path | What it is |
|---|---|
| `android/` | The phone app, a Gradle project. |
| `desktop/` | The desktop app, a standalone pnpm package. It is today's `web/`, renamed. |
| `protocol/` | What the two apps share: `README.md` (the wire format in prose), `vectors.json` (the test cases both test suites read) and `tcp-hub.md` (notes on the hub). |
| `docs/` | `adr/` and `agents/`, unchanged. |
| `README.md`, `CONTEXT.md`, `CLAUDE.md` | At the root, as today. |

- The directories name the platform, not the technology, and leave room for an `ios/` sibling.
- There is no tooling at the root: no root `package.json` and no workspace. The two apps share only `protocol/`, which each reads as files.
- The TypeScript protocol module is inside `desktop/` and the Kotlin one inside `android/`. `protocol/` holds no code. `vectors.json` is what keeps the two implementations from drifting apart silently.

Tickets: [Repo layout and cutover](issues/12-repo-layout-and-cutover.md).

## 3. Cutover

The old stack (`mobile/`, `server/`, `web/`) is already out of use and the relay is shut down. Nothing waits for the new apps to catch up: the old stack goes at the start of implementation. These are the first steps of implementation, in order.

1. **Tag.** The last commit before implementation is tagged `legacy-stack`. The React Native app's VESC parsing code stays reachable through the tag, as a reference for the Kotlin implementation.
2. **Delete.** The first implementation commit deletes `mobile/` and `server/` and updates `README.md` to describe the new stack.
3. **Rename.** `web/` becomes `desktop/` with `git mv`, so file history and `git blame` carry over. The desktop app is built in place there, not copied. In the same step `package-lock.json` is replaced by `pnpm-lock.yaml`, and the `packageManager` field in `desktop/package.json` pins the pnpm version.
4. **Create `protocol/`.** `tcp-hub.md` moves in from the root. The sections of the hub spec marked **(moves)** (framing, board commands, our messages and the vector list) move into `protocol/README.md`, and the hub spec keeps a link in their place. `vectors.json` is created with the cases the hub spec names; computing their hex and expected values is part of this step. References to `tcp-hub.md` at its old path are updated.
5. **Create `android/`.** A new Gradle project.

On the phone, the old React Native app has the application ID the new release app takes. It is uninstalled before the new app is first installed.

Passing the acceptance session (section 7) is the bar for the migration being done. It triggers no deletion.

Tickets: [Repo layout and cutover](issues/12-repo-layout-and-cutover.md), [Where the specs live and how they are split](issues/18-where-the-specs-live-and-how-they-are-split.md).

## 4. Release workflow

Both apps are built by GitHub Actions from a version tag and fetched by hand from the GitHub Release. Nothing is signed with a paid certificate and nothing installs itself.

- **One workflow, on `v*` tags only.** There is no manual trigger and no workflow on ordinary pushes: a push to main builds nothing and tests nothing. Actions costs nothing here because the repo is public.
- **Tests first.** The workflow's first step runs both apps' automated suites, named in section 4 of the Android and desktop specs. A failing test produces no release.
- **What it builds:** the release APK, the macOS app and the Windows app, each stamped with the version from the tag. How each is packaged is in the part specs.
- **Where they go:** all three are attached to one GitHub Release for the tag. A plain tag (`v1.2.0`) publishes a release. A tag with a suffix (`v1.2.0-rc1`) publishes a pre-release.
- **Release candidates** are how a build is tried, and how the workflow itself is fixed, without using up a version number. The apps' update lines ignore pre-releases.
- **Android signing:** the release keystore is made once and kept as an Actions secret, with a copy the maintainer holds offline. If it is lost, a newer APK cannot install over the old app: the app is uninstalled, which makes a new team code for every laptop to type in again.
- **Local builds** stay possible for both apps and need nothing from the workflow.
- **Getting a release:** the APK is sideloaded onto the phone; a laptop downloads its package from the Release page. Each app shows one "update available" line when a newer release exists and does nothing more.
- **Older releases stay on the Releases page,** so a laptop can be put back on the major version the phone runs.

Tickets: [Distribution](issues/15-distribution.md), [Testing without the car](issues/16-testing-without-the-car.md).

## 5. Version scheme

- **One number for the whole repo,** taken from the tag alone. No file in the repo holds it. The workflow stamps it into both apps, and each shows it on its settings screen.
- **The protocol version is the app's major version.** A phone and a laptop on the same major work together; on different majors, in either direction, the viewer shows nothing from the stream and says so. The rule is in the hub spec's "Protocol version" and in [ADR 0002](../../docs/adr/0002-protocol-version-is-the-app-major-version.md).
- **What each part of the number means:** a change to the messages that an older app cannot read is a new major release of both apps. Within a major, messages change only by addition. Minor and patch carry no protocol meaning.
- **A candidate and its final release** (`v1.2.0-rc1`, `v1.2.0`) have the same major, minor and patch, so they share an Android version code, and Android allows installing one over the other.
- **A local build** takes its version from the latest version tag in git with `-dev` added, such as `1.2.0-dev`. The hub spec's "Protocol version" has the rule and its cost.

Tickets: [Distribution](issues/15-distribution.md), [Write the hub usage spec](issues/19-write-the-hub-usage-spec.md).

## 6. Desk checklist

Passed in full before the acceptance session is booked. It is where the facts the part specs list as not established are found out, without the car on a track. Passing it is not the acceptance session.

Desk tests use the public hub. The phone is a real phone of the race phone's model, running the debug build ("redLINK dev") with the manufacturer settings from `README.md` applied. A debug build has its own team code, which the laptops are given for these tests.

1. **The long run.** The phone does a simulated run for 1 hour with the screen off, on mobile data with Wi-Fi off. Throughout it, 8 viewer windows on one laptop stay live. Afterwards the log has no gaps. This is done once. It is where app-killing by the phone's manufacturer, Doze, and a mobile carrier dropping idle sockets are caught before the track. It uses about 60 MB of phone data.
2. **The short run.** A second simulated run of about 10 minutes, on any network, carries items 3 to 5. It is kept apart from the long run because item 5 puts a gap in the log.
3. **The other system.** 8 viewer windows on one laptop of the system the long run did not use stay live through the short run. Between the two runs, macOS and Windows have each held 8 viewers.
4. **Each status-line state once.** On one laptop every state of the desktop spec's status line is produced by hand: no team code (before the code is typed), hub unreachable (a wrong host), phone not found (before Start, or after Stop), joining and live (Start), phone lost (the phone's network cut), board unreachable ("stop answering" on the phone's Setup screen). Version mismatch is the exception: only an automated test produces it.
5. **Kill and resume.** During the short run the app's process is killed once from a computer with `adb`. "Force stop" in Android's settings does not count: Android restarts nothing after it. The run must resume by itself into the same log file, with the gap showing as missing rows, and every viewer must drop out of live and come back live with nobody touching a laptop. The state in between is phone not found, not phone lost: the hub closes a viewer's socket when the killed process's sockets close.
6. **The board on a stand.** With the car on a stand and its wheels lifted, and the wheel turned:
   - The phone, with the real board picked, connects: the Vehicle row reads "Connected", Setup shows the firmware version, and a viewer shows sane values. The phone screen itself shows no telemetry.
   - One macOS laptop and one Windows laptop each start a direct link: the board appears in the list, and after it is picked the dashboard shows sane values at a steady rate. The board is then switched off and on, and each laptop reconnects by itself.
7. **The self-hosted hub, once.** A stock hub is started on a rented server as the hub spec's "Host, port and the self-hosted hub" describes. The phone (a simulated run is enough) and two laptops are moved to it by typing the host; both laptops join and show live values; all three are moved back to the public hub and join again. Then the server is destroyed. This is not repeated.
8. **Board speed settings.** The wheel diameter, gear ratio and motor pole count set on the board match the mechanics team's measured figures. Until they do, speed and distance in every log and on every dashboard are off by whatever the board's estimates are off by. Neither app changes when they are corrected.

Not on the checklist, because they are met earlier: the emulator requirement in the Android spec's "The simulated board and the debug build", and the two pnpm settings Distribution wrote from memory, which the first packaged build checked (desktop spec 2.12).

Tickets: [Testing without the car](issues/16-testing-without-the-car.md), [Self-run hub fallback](issues/17-self-run-hub-fallback.md), [Board speed settings and firmware version](issues/14-board-speed-settings-and-firmware-version.md), [Android headless operation constraints](issues/02-android-headless-operation-constraints.md). Decided while writing this page: the long run is 1 hour, not 2, done once and on mobile data; the second system's 8 windows and the status-line states moved to a short run; kill and resume was added; and the direct link is tried on a macOS and a Windows laptop, not on one.

## 7. Acceptance session

One real track session with the car. The migration is done when all of these hold in that session:

- The phone completes a run with the screen off, and its log of record has no gaps.
- At least two laptops, one macOS and one Windows, watch through the hub for the whole run.
- Every dashboard feature works on a viewer: gauges, ADC, map, trip meter, CSV export.
- The direct link works on one laptop.

A test on a desk does not count, however complete: the old app's failures only showed up in the car. The session is booked only after the desk checklist has passed.

Tickets: [Repo layout and cutover](issues/12-repo-layout-and-cutover.md).

## 8. Out of scope

- **An iOS app.** A later effort that follows the Android spec.
- **Control, configuration or firmware commands from the dashboard.** It stays read-only.
- **Forwarding anything a viewer sends to the board.** Not for now; it may be reconsidered later.
- **New dashboard features** beyond today's gauges, ADC, map, trip meter and CSV export.
- **Any custom relay or server code.** The hub is used as it is, public or self-hosted.
- **VESC Tool attaching through the hub.** Debugging connects VESC Tool to the board directly.
- **Updating the team's ESP32 board mimic** for the new polling. It stays as it is, outside this repo.
- **Any workflow on ordinary pushes,** and any root-level tooling.

Each part spec ends with its own list.

Tickets: the [map](map.md)'s Out of scope, [What a viewer may send to the board](issues/06-what-a-viewer-may-send-to-the-board.md), [How several pit laptops watch at once](issues/04-how-several-pit-laptops-watch-at-once.md).
