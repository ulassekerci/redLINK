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
