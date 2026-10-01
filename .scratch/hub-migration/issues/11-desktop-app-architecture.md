# Desktop app architecture

Type: grilling
Status: open
Blocked by: 04, 05

Map: [Hub migration](../map.md)

## Question

The Electron renderer is the existing React dashboard with its data layer swapped. How is the hub client placed in the Electron app?

Decide: what runs in the main process versus the renderer; the shape of the data handed to the existing stores; what replaces the current source picker; which parts of `web/` are dropped (Web Bluetooth, Socket.IO); where the trip meter and CSV export live now that each laptop has its own copy of the stream; how connection failures are shown.
