# Write the spec front page

Type: task
Status: resolved

Map: [Hub migration](../map.md)

## Question

Write `.scratch/hub-migration/spec.md`: the entry point an implementer reads first, linking the three part specs and holding everything that belongs to no single part.

Cover, each under its own heading: what the system is, in a paragraph; the repo layout and the cutover steps in order (the `legacy-stack` tag, the rename, the deletions, pnpm, creating `protocol/` from the hub spec); the release workflow and the version scheme; the desk checklist; the acceptance session; what is out of scope, from the map.

Sources: the map, [Repo layout and cutover](12-repo-layout-and-cutover.md), [Board speed settings and firmware version](14-board-speed-settings-and-firmware-version.md) (the checklist item), [Distribution](15-distribution.md), [Testing without the car](16-testing-without-the-car.md), [Self-run hub fallback](17-self-run-hub-fallback.md) (the checklist item).

Follow [Where the specs live and how they are split](18-where-the-specs-live-and-how-they-are-split.md) for the path, the restate-not-link rule, the section layout and what to do on finding a contradiction. Use the vocabulary in `CONTEXT.md`.

## Answer

[`spec.md`](../spec.md) is written with nothing left open. Written on 2026-10-02. Nothing was built, moved or tagged.

Its sections: the system, repo layout, cutover (five steps in order), release workflow, version scheme, desk checklist, acceptance session, out of scope. It restates the decisions of its source tickets in final form and points to the part specs for what they already hold (packaging, the version code, the update lines, the protocol version rule) instead of repeating it.

The part specs hand several "not established" facts to the desk checklist that no decided item covered. These were put to the user and decided in this session:

- **The long run is 1 hour, not 2,** done exactly once, on mobile data with Wi-Fi off, with 8 viewer windows on one laptop. Mobile data is what finds out whether a carrier drops the idle lobby socket. The run was shortened because the time and the phone data were judged worth saving; the cost is less evidence against manufacturer app-killing.
- **A short run of about 10 minutes** carries the rest: 8 windows on the other system, each status-line state by hand, and the new kill-and-resume item.
- **Kill and resume is a new item:** the app's process is killed once with `adb` during the short run, and the run must resume into the same log file with viewers returning by themselves. It is kept out of the long run because it puts a gap in the log.
- **The direct link is tried on one macOS and one Windows laptop** with the car on a stand, including a reconnect after the board is switched off and on. The desktop spec leaves Web Bluetooth's rate on both systems and reconnecting on Windows to the checklist. The acceptance session still asks for the direct link on one laptop.

Filled in without asking, as wording:

- For the phone, "shows sane values" on the stand means the Vehicle row reads "Connected", Setup shows the firmware version and a viewer shows the values: the phone screen shows no telemetry.
- The checklist phone runs the debug build, since only it has the simulated board, so the laptops are given that build's team code for the desk tests.
- The old React Native app is uninstalled from the phone before the first install, stated under the cutover.

Changed elsewhere: [`spec-android.md`](../spec-android.md) called the long run "the 2-hour screen-off item"; it now says 1-hour.

Amends [Testing without the car](16-testing-without-the-car.md): the desk checklist as above.

No change to `CONTEXT.md` and no ADR.
