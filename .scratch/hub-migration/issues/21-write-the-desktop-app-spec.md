# Write the desktop app spec

Type: task
Status: open
Blocked by: 19

Map: [Hub migration](../map.md)

## Question

Write `.scratch/hub-migration/spec-desktop.md`: the Electron app as viewer and on a direct link.

Cover: the main process hub client and what it hands the renderer; the connection states and the status line; joining on launch; the direct link and how it excludes the hub; the shared protocol module; the trip meter and CSV export; team code entry and the settings file; no single-instance lock; packaging details that are the app's own (bundle ID, Bluetooth permission text); the update line; the desktop automated tests. Read `web/` for what the existing dashboard does, since it is the feature bar.

Sources: [How desktop VESC Tool behaves as a hub client](03-how-desktop-vesc-tool-behaves-as-a-hub-client.md) (packet dispatch only), [How hub credentials are set and shared](10-how-hub-credentials-are-set-and-shared.md), [Desktop app architecture](11-desktop-app-architecture.md), [Distribution](15-distribution.md), [Testing without the car](16-testing-without-the-car.md), [Self-run hub fallback](17-self-run-hub-fallback.md).

Follow [Where the specs live and how they are split](18-where-the-specs-live-and-how-they-are-split.md) for the path, the restate-not-link rule, the section layout and what to do on finding a contradiction. Use the vocabulary in `CONTEXT.md`.
