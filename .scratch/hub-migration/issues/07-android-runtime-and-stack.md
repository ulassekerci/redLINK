# Android runtime and stack

Type: grilling
Status: open
Blocked by: 02

Map: [Hub migration](../map.md)

## Question

How is the Android app built so it runs as a headless logger and bridge with the screen off?

Decide: the service model and what the user must grant at first launch; reconnect behaviour when BLE or the hub connection drops mid-run; what starts and stops a run; UI toolkit, minimum Android version and BLE library.
