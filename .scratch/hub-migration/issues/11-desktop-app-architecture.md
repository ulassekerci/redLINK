# Desktop app architecture

Type: grilling
Status: open
Blocked by: 04, 05, 13

Map: [Hub migration](../map.md)

## Question

The Electron renderer is the existing React dashboard with its data layer swapped. How is the hub client placed in the Electron app?

Decide: what runs in the main process versus the renderer; the shape of the data handed to the existing stores; what replaces the current source picker; how the direct Bluetooth connection to the board (no phone, used while testing the vehicle) is provided in Electron and how it polls there; which parts of `web/` are dropped (Socket.IO); where the trip meter and CSV export live now that each laptop has its own copy of the stream; how connection failures are shown.

Also carry this requirement from [Android runtime and stack](07-android-runtime-and-stack.md): a viewer waits a random 1 to 2 s between lobby attempts, so that 8 laptops returning together after the phone drops do not displace each other round after round.

And these facts from [Stale re-registration on the public hub](13-stale-re-registration-on-the-public-hub.md): the hub discards bytes sent in the same write as the login line; another party's slow connection can make it discard a viewer's bytes for up to 5 s or reset the viewer; of several lobby requests within about 100 ms only one arrives. Decide how the viewer sends its lobby request given that it cannot know when its login took effect.

And these from [What the phone log contains](08-what-the-phone-log-contains.md): the stream now carries `COMM_GET_VALUES_SETUP` (47) replies instead of `COMM_GET_VALUES` (4), so the viewer parses that reply, takes speed and distance from it, and drops the wheel, gear and pole constants in `web/src/utils/erpm.ts`; the direct Bluetooth path polls the same command. The log of record knows nothing about trips, so the trip meter is the viewer's alone.

And these from [How hub credentials are set and shared](10-how-hub-credentials-are-set-and-shared.md): the app asks for the team code on first launch and keeps it, with the hub's host and port, in its settings; it rejects a code whose check character is wrong; the waiting screen shows the code in use; the viewer builds the lobby ID, its own ID and the password from the code and an 8-character token made fresh for each lobby visit.
