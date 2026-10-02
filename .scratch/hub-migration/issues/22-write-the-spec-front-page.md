# Write the spec front page

Type: task
Status: open

Map: [Hub migration](../map.md)

## Question

Write `.scratch/hub-migration/spec.md`: the entry point an implementer reads first, linking the three part specs and holding everything that belongs to no single part.

Cover, each under its own heading: what the system is, in a paragraph; the repo layout and the cutover steps in order (the `legacy-stack` tag, the rename, the deletions, pnpm, creating `protocol/` from the hub spec); the release workflow and the version scheme; the desk checklist; the acceptance session; what is out of scope, from the map.

Sources: the map, [Repo layout and cutover](12-repo-layout-and-cutover.md), [Board speed settings and firmware version](14-board-speed-settings-and-firmware-version.md) (the checklist item), [Distribution](15-distribution.md), [Testing without the car](16-testing-without-the-car.md), [Self-run hub fallback](17-self-run-hub-fallback.md) (the checklist item).

Follow [Where the specs live and how they are split](18-where-the-specs-live-and-how-they-are-split.md) for the path, the restate-not-link rule, the section layout and what to do on finding a contradiction. Use the vocabulary in `CONTEXT.md`.
