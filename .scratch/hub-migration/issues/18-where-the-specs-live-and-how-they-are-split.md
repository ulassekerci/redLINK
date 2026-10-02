# Where the specs live and how they are split

Type: grilling
Status: resolved

Map: [Hub migration](../map.md)

## Question

Every decision ticket is resolved; what remains is writing the three specs the destination names (Android app, hub usage, Electron app). Where do they live, and what goes in which?

Decide: the file paths (the tracker convention is one `.scratch/<feature-slug>/spec.md` per feature, and there are three parts); how the hub usage spec relates to `protocol/README.md` and `protocol/vectors.json` from [Repo layout and cutover](12-repo-layout-and-cutover.md), so the wire format is described in one place only; where cross-cutting requirements go (the named test suites and desk checklist from [Testing without the car](16-testing-without-the-car.md), the release workflow from [Distribution](15-distribution.md), the cutover steps); whether a spec restates decisions or links to the tickets; in what order the three are written and whether each is its own session.

## Answer

Four files in `.scratch/hub-migration/`, each written in its own session and complete without the tickets, followed by one check that reads them together. Decided by grilling on 2026-10-02. Nothing here was written.

- **Paths:** `spec.md` is the front page; `spec-hub.md`, `spec-android.md` and `spec-desktop.md` sit beside it. The three parts are one feature with one `issues/` directory for implementation tickets. Three feature directories and a permanent `docs/specs/` were rejected: the parts ship and are tested together.
- **Wire format in one place:** until implementation starts, `spec-hub.md` is the only description of the framing and of our `COMM_CUSTOM_APP_DATA` messages. The first implementation step moves those sections into `protocol/README.md` and leaves a link behind. `protocol/` is not created inside this map. What stays in the hub spec after the move is behaviour: lobby, joining, retry, heartbeat, stale registrations, team code, self-hosted hub.
- **Vectors:** the hub spec lists the vectors `protocol/vectors.json` must contain by name (one per message type, plus edge cases such as a bad CRC and an unknown type byte) and gives no hex. Computing frames is implementation.
- **Cross-cutting requirements:** the front page holds the cutover steps, the release workflow, the version scheme, the desk checklist and the acceptance session, each under its own heading. A part spec holds only its own named test suites. The reading order is the front page, then the part.
- **Restate, not link:** a spec states every decision in its final form, with amendments already applied. Each section ends with links to its tickets, for the reasoning and the rejected alternatives only. Where a spec and a ticket disagree, the spec wins; the tickets become history.
- **Order:** the hub spec blocks the Android and desktop specs, which cite its message names and states. The front page is blocked by nothing. The Android and desktop specs can be written in parallel.
- **Sessions:** one ticket and one session per file, type `task`, driven by the agent alone. This is the map carrying execution for documents only, which its destination already names as the output.
- **Contradictions found while writing:** a later ticket's explicit "Amends" line settles a disagreement without asking. Anything else that changes behaviour stops the section: it is marked open in the spec and a grilling ticket is opened on the map. The writing session never picks silently.
- **Final check:** a fifth ticket, blocked by the four, reads the specs together against the map's Decisions so far: every decision appears in exactly one spec, and the vocabulary matches `CONTEXT.md`. Closing it reaches the destination.
- **Part spec layout,** shared by the three so parallel sessions match:
  1. Purpose
  2. Behaviour, by area, each area ending with its ticket links
  3. States and messages shown to the user, with the Turkish strings verbatim
  4. Required automated tests
  5. Out of scope

  Prose is English; user-facing strings stay Turkish.

No change to `CONTEXT.md` and no ADR: nothing here is hard to reverse.

Amended 2026-10-02 by [Write the Android app spec](20-write-the-android-app-spec.md): the phone app's strings are English, as its prototype had them. "User-facing strings stay Turkish" applies to the desktop app only.
