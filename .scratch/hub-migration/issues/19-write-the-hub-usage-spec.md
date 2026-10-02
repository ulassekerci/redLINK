# Write the hub usage spec

Type: task
Status: open

Map: [Hub migration](../map.md)

## Question

Write `.scratch/hub-migration/spec-hub.md`: how the bridge and the viewers use the hub, and the wire format both apps implement.

Cover: the VESC framing and each `COMM_CUSTOM_APP_DATA` message (GPS, lobby request, heartbeat, status) byte by byte; polling and what is copied to a registration; the lobby, joining, retry pacing and stale registrations; the team code and how IDs and the password derive from it; the protocol version rule; host and port and the self-hosted hub; the list of vectors `protocol/vectors.json` must contain, by name, with no hex. Mark which sections move to `protocol/README.md` at the start of implementation.

Sources: [Hub limits for multiple registrations](01-hub-limits-for-multiple-registrations.md), [How several pit laptops watch at once](04-how-several-pit-laptops-watch-at-once.md), [What travels on the stream](05-what-travels-on-the-stream.md), [What the phone log contains](08-what-the-phone-log-contains.md) (the change to `COMM_GET_VALUES_SETUP`), [How hub credentials are set and shared](10-how-hub-credentials-are-set-and-shared.md), [Stale re-registration on the public hub](13-stale-re-registration-on-the-public-hub.md), [Board speed settings and firmware version](14-board-speed-settings-and-firmware-version.md), [Distribution](15-distribution.md) (mismatched versions), [Self-run hub fallback](17-self-run-hub-fallback.md), `tcp-hub.md`, and both ADRs in `docs/adr/`.

Follow [Where the specs live and how they are split](18-where-the-specs-live-and-how-they-are-split.md) for the path, the restate-not-link rule, the section layout and what to do on finding a contradiction. Use the vocabulary in `CONTEXT.md`.
