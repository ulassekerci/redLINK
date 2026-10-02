# Write the Android app spec

Type: task
Status: open
Blocked by: 19

Map: [Hub migration](../map.md)

## Question

Write `.scratch/hub-migration/spec-android.md`: the phone app as bridge and logger.

Cover: the foreground service, grants and what a run is; reconnect behaviour; the bridge's side of the lobby and registrations (citing the hub spec for the wire format, not restating it); the log of record; the main, Logs and Setup screens; team code generation; the simulated board and the debug build; the update line; the stack; the Android automated tests.

Sources: [Android headless operation constraints](02-android-headless-operation-constraints.md), [Android runtime and stack](07-android-runtime-and-stack.md), [What the phone log contains](08-what-the-phone-log-contains.md), [What the phone screen shows](09-what-the-phone-screen-shows.md), [How hub credentials are set and shared](10-how-hub-credentials-are-set-and-shared.md), [Board speed settings and firmware version](14-board-speed-settings-and-firmware-version.md), [Distribution](15-distribution.md), [Testing without the car](16-testing-without-the-car.md), [Self-run hub fallback](17-self-run-hub-fallback.md), and the assets in `../assets/09-phone-screen/`.

Follow [Where the specs live and how they are split](18-where-the-specs-live-and-how-they-are-split.md) for the path, the restate-not-link rule, the section layout and what to do on finding a contradiction. Use the vocabulary in `CONTEXT.md`.
