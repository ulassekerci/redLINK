# Check the specs against the decisions

Type: task
Status: resolved
Blocked by: 19, 20, 21, 22

Map: [Hub migration](../map.md)

## Question

Read the four specs together against the map's Decisions so far.

Check: every decision, in its final amended form, appears in exactly one spec; the part specs cite the hub spec for the wire format and do not restate it; message names, state names and Turkish strings are identical wherever they recur; the vocabulary matches `CONTEXT.md`; no section is left marked open. Fix what is a slip; anything that needs a decision becomes a grilling ticket. Closing this ticket reaches the map's destination.

Follow [Where the specs live and how they are split](18-where-the-specs-live-and-how-they-are-split.md) for the path, the restate-not-link rule, the section layout and what to do on finding a contradiction. Use the vocabulary in `CONTEXT.md`.

## Answer

The four specs agree with the decisions in their final amended form. Five slips were fixed, nothing needed a decision, and no grilling ticket was opened. Checked on 2026-10-02 by reading the four specs against every ticket's answer and both ADRs. Nothing was built or run.

- **Fixed:**
  - [`spec-desktop.md`](../spec-desktop.md) section 4 still said one laptop tries the direct link on the stand. It now says a macOS and a Windows laptop, as the front page's desk checklist does since [Write the spec front page](22-write-the-spec-front-page.md).
  - [`spec-android.md`](../spec-android.md) 2.6 said Stop sends viewers to phone lost and then phone not found. By the hub spec, the hub closes a viewer's socket when the bridge closes its registration, and the viewer's next lobby visit finds no lobby: the state is phone not found. Phone lost is only reached when status stops without the socket closing, which is a silent network drop.
  - [`spec.md`](../spec.md) desk checklist item 5 (kill and resume) had the same slip: a killed process closes its sockets, so viewers pass through phone not found, not phone lost. The requirement itself is unchanged: they come back live by themselves.
  - "A field the GPS fix lacks is sent as 0" is part of the wire format but was only in the Android spec. It is now stated under the GPS message in [`spec-hub.md`](../spec-hub.md) 2.4, and the Android spec cites it. The desktop spec's CSV export now says such a field reads 0 where the log of record has an empty cell.
  - [`spec-hub.md`](../spec-hub.md) 2.9 said starting a direct link makes the desktop app leave the hub. It now says the app leaves when a board is picked, as the desktop spec has it.
- **Every decision has a home.** Each ticket's answer, with its amendments, was traced to a spec section. Where a decision shows in more than one file (the local-build version rule, the typed-code rules, why the code is not in a keystore, fault names without their prefix), the copies say the same thing and the part specs point to the owning section.
- **Wire format:** the part specs describe no frame or payload layout. They do repeat some hub-spec timings in passing (the 50 ms cycle, the firmware request's 250 ms wait, the lobby visit's timings in the test lists); each matches the hub spec and was left.
- **Names and strings:** the four message names and the eight viewer state names are identical in all files. The Turkish strings that recur inside the desktop spec match, and the two from [Distribution](15-distribution.md) match that ticket. The direct link's "board not answering" is deliberately not one of the eight states.
- **Numbers:** the setup reply's 70 bytes, the 97-byte poll cycle, the 57 MB per hour and the example code's check character were recomputed and hold.
- **Vocabulary:** no avoided term from `CONTEXT.md` is used for its concept. "Client", "password" and "device" appear only for the hub's own notions and for Bluetooth scan results.
- **No section is marked open,** and every link in the four files resolves.

Left as they are, for the record:

- The map's line for [Android headless operation constraints](02-android-headless-operation-constraints.md) says no background location permission is needed. [Android runtime and stack](07-android-runtime-and-stack.md) later made "all the time" location a grant, for automatic restart, and the Android spec follows the later ticket.
- The Android spec says the prototype supplied the strings it does not list as new. `Change vehicle`, `Pick vehicle` and `No logs yet` are not in the prototype either. The spec wins, so nothing changes.

No change to `CONTEXT.md` and no ADR. With this ticket closed the map has no open ticket and nothing in Not yet specified: the destination is reached.
