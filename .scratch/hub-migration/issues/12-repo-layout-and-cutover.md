# Repo layout and cutover

Type: grilling
Status: resolved

Map: [Hub migration](../map.md)

## Question

The new apps live in this repo and replace `mobile/`, `server/` and `web/`, with the old stack kept until parity. What is the layout and when does the old stack go?

Decide: directory names for the Android and Electron apps; whether the Electron app is built in place from `web/` or copied; where the protocol description shared by Kotlin and TypeScript lives; what "parity" means concretely as the trigger for deleting the old stack.

## Answer

Two top-level directories, `android/` and `desktop/`, beside a shared `protocol/` directory; the old stack goes at the start of implementation, not at parity. Decided by grilling on 2026-10-02. Nothing here was built or moved.

The question assumed the old stack was still in use. It is not: the relay is already shut down and nobody runs the old apps. That removed the reason to keep them until parity.

- **Directory names:** `android/` and `desktop/`. They name the platform, not the technology, and leave room for an `ios/` sibling.
- **Convert in place:** `web/` is renamed to `desktop/` with `git mv` and the changes from [Desktop app architecture](11-desktop-app-architecture.md) are made there, so file history and `git blame` carry over. Copying was considered only to keep the old dashboard working against the relay, which no longer exists.
- **Removal:** the commit before implementation starts is tagged `legacy-stack`. The first implementation commit deletes `mobile/` and `server/` and updates `README.md`. The React Native app's VESC parsing code stays reachable through the tag as a reference for the Kotlin implementation.
- **Shared protocol description:** a top-level `protocol/` directory holding `README.md` (the prose description of the framing and of our `COMM_CUSTOM_APP_DATA` messages) and `vectors.json` (hex frames with their expected decoded fields). The Kotlin and the TypeScript test suites both read the vectors, so the two implementations cannot drift silently. A generated schema was rejected as more tooling than the small message set repays; prose alone was rejected because nothing would catch drift. `tcp-hub.md` moves into `protocol/`. The TypeScript protocol module itself stays inside `desktop/`, as already decided.
- **No root tooling:** no root `package.json` and no workspaces. `android/` is a Gradle project and `desktop/` is a standalone npm package; they share only `protocol/`, read as files.
- **Acceptance session, replacing "parity":** it no longer triggers a deletion; it is the bar the specs point to for the migration being done. One real track session with the car in which all of these hold. A desk test does not count, because the old app's failures only showed up in the car.
  - The phone completes a run with the screen off and its log of record has no gaps.
  - At least two laptops, one macOS and one Windows, watch through the hub for the whole run.
  - Every dashboard feature works on a viewer: gauges, ADC, map, trip meter, CSV export.
  - The direct link works on one laptop.

The rename, the deletions and the move of `tcp-hub.md` are the first steps of implementation, not of this map. The map's standing note about keeping the old stack until parity was replaced.

Added to `CONTEXT.md`: **Acceptance session**.
