# Write the hub usage spec

Type: task
Status: resolved

Map: [Hub migration](../map.md)

## Question

Write `.scratch/hub-migration/spec-hub.md`: how the bridge and the viewers use the hub, and the wire format both apps implement.

Cover: the VESC framing and each `COMM_CUSTOM_APP_DATA` message (GPS, lobby request, heartbeat, status) byte by byte; polling and what is copied to a registration; the lobby, joining, retry pacing and stale registrations; the team code and how IDs and the password derive from it; the protocol version rule; host and port and the self-hosted hub; the list of vectors `protocol/vectors.json` must contain, by name, with no hex. Mark which sections move to `protocol/README.md` at the start of implementation.

Sources: [Hub limits for multiple registrations](01-hub-limits-for-multiple-registrations.md), [How several pit laptops watch at once](04-how-several-pit-laptops-watch-at-once.md), [What travels on the stream](05-what-travels-on-the-stream.md), [What the phone log contains](08-what-the-phone-log-contains.md) (the change to `COMM_GET_VALUES_SETUP`), [How hub credentials are set and shared](10-how-hub-credentials-are-set-and-shared.md), [Stale re-registration on the public hub](13-stale-re-registration-on-the-public-hub.md), [Board speed settings and firmware version](14-board-speed-settings-and-firmware-version.md), [Distribution](15-distribution.md) (mismatched versions), [Self-run hub fallback](17-self-run-hub-fallback.md), `tcp-hub.md`, and both ADRs in `docs/adr/`.

Follow [Where the specs live and how they are split](18-where-the-specs-live-and-how-they-are-split.md) for the path, the restate-not-link rule, the section layout and what to do on finding a contradiction. Use the vocabulary in `CONTEXT.md`.

## Answer

[`spec-hub.md`](../spec-hub.md) exists and no section is left open. Written on 2026-10-02.

- **Marked to move into `protocol/README.md`:** 2.2 Framing, 2.3 Board commands, 2.4 Our messages and 4.1 Vectors. Everything else stays in the hub spec.
- **Decided with the user while writing:**
  - A viewer in version mismatch stays attached, keeps its heartbeat and keeps reading status; it leaves the state only through phone lost and a new lobby visit. Detaching and retrying every 5 s was rejected.
  - `protocol/vectors.json` gets a second group of team-code cases (validity, normalised code, lobby ID, viewer ID, password) beside the frames, so the Kotlin and TypeScript check-character code cannot drift. This extends [Repo layout and cutover](12-repo-layout-and-cutover.md), which described the file as hex frames only.
  - A local build takes its version from the latest version tag in git with `-dev` added, falling back to `0.0.0-dev` only with no tag. The fixed `0.0.0-dev` from [Distribution](15-distribution.md) gave every local build protocol version 0, a version mismatch against any released build of the other app. Amends [Distribution](15-distribution.md).
- **Settled from the tickets without asking:** the token is fresh for every lobby visit. [How several pit laptops watch at once](04-how-several-pit-laptops-watch-at-once.md) said "at launch"; four later tickets say per visit.
- **Corrected:** the example code in [How hub credentials are set and shared](10-how-hub-credentials-are-set-and-shared.md), `K7QM3XPD`, fails its own check character. The spec uses `K7QM-3XPC`.
- **Recalculated:** a poll cycle is 97 bytes with `COMM_GET_VALUES_SETUP`, about 2 kB/s per viewer and 57 MB per hour with 8 viewers. The decoded ADC reply layout was taken from `web/src/services/bluetooth/commands/get-adc.ts`.
- **What the other specs cite instead of restating:** the eight viewer state names and their conditions (section 3); the polling cycle and its 250 ms and 2 s timeouts (2.6), which the direct link reuses; the bridge's lobby re-registration pacing (2.8); the viewer's lobby visit and waiting pace (2.9); the team code's shape, check character and ID derivation (2.7).
- **Left to the other specs:** who generates and types the team code, the phone's lock on host and port during a run, where each app shows a non-default host, and every Turkish string (Android and desktop); the self-hosted hub desk test (front page). Section 4.2 lists the protocol behaviour each part spec's test suite must cover.
