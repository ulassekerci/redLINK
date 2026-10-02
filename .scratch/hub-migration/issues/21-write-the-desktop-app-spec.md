# Write the desktop app spec

Type: task
Status: resolved
Blocked by: 19

Map: [Hub migration](../map.md)

## Question

Write `.scratch/hub-migration/spec-desktop.md`: the Electron app as viewer and on a direct link.

Cover: the main process hub client and what it hands the renderer; the connection states and the status line; joining on launch; the direct link and how it excludes the hub; the shared protocol module; the trip meter and CSV export; team code entry and the settings file; no single-instance lock; packaging details that are the app's own (bundle ID, Bluetooth permission text); the update line; the desktop automated tests. Read `web/` for what the existing dashboard does, since it is the feature bar.

Sources: [How desktop VESC Tool behaves as a hub client](03-how-desktop-vesc-tool-behaves-as-a-hub-client.md) (packet dispatch only), [How hub credentials are set and shared](10-how-hub-credentials-are-set-and-shared.md), [Desktop app architecture](11-desktop-app-architecture.md), [Distribution](15-distribution.md), [Testing without the car](16-testing-without-the-car.md), [Self-run hub fallback](17-self-run-hub-fallback.md).

Follow [Where the specs live and how they are split](18-where-the-specs-live-and-how-they-are-split.md) for the path, the restate-not-link rule, the section layout and what to do on finding a contradiction. Use the vocabulary in `CONTEXT.md`.

## Answer

[`spec-desktop.md`](../spec-desktop.md) exists and no section is left open. Written on 2026-10-02. No contradiction between tickets was found, and nothing in the hub spec or the Android spec was changed.

- **Filled in without asking,** each small and marked in the spec where it sits:
  - The direct link's scan is given up after 10 s, and the app stays on the hub until a board has been picked, so a failed scan changes nothing. [Desktop app architecture](11-desktop-app-architecture.md) said only that starting a direct link leaves the hub.
  - The battery percentage stays the Aspilsan cell-curve figure the dashboard shows today, not the board's battery level from the setup reply.
  - The fault warning shows the fault's name without the `FAULT_CODE_` prefix, as the phone does; today's dashboard shows the prefix.
  - The CSV export leaves out `elapsed_s` (a viewer does not know when the run started) and otherwise has the log of record's columns in order; ADC and GPS cells repeat the latest sample.
  - The trip is kept across a switch between the hub and a direct link.
  - The update line is shown on the waiting screen as well as in settings, because the pit crew does not open settings after first launch.
  - The settings file is `settings.json`; a change saved in one window reaches other windows on restart. Host and port can be changed at any time, unlike on the phone.
  - The Inter font is bundled instead of loaded from `rsms.me`, so a direct link with no internet looks the same. Map tiles still need internet.
  - Space does nothing while a text field has the focus.
  - Every Turkish string except today's dashboard strings, the version mismatch line and the Bluetooth permission text: the status line, the waiting screen, settings and the direct link.
  - Five named test suites run with `pnpm test` on Vitest: `protocol-vectors`, `team-code`, `hub-client`, `direct-link`, `trip-and-csv`. The last is beyond what [Testing without the car](16-testing-without-the-car.md) required.
- **Not established,** and named in the spec: whether Electron's Web Bluetooth holds 20 Hz against the board on both systems, and whether reconnecting to the same device without a new scan works on Windows.
- **For [Write the spec front page](22-write-the-spec-front-page.md):** the tag workflow's desktop test command is `pnpm test` in `desktop/`; the desktop spec puts the first-open steps for the unsigned app in `README.md` and cites the front page's desk checklist for everything not automated.
- **For [Check the specs against the decisions](23-check-the-specs-against-the-decisions.md):** the desktop spec names the board "araç" in Turkish strings; the direct link's "board not answering" reuses the hub spec's polling timeouts but is not one of its eight viewer states.

No change to `CONTEXT.md` and no ADR.
