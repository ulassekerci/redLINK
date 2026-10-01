# Android headless operation constraints

Type: research
Status: open

Map: [Hub migration](../map.md)

## Question

What does current Android require for an app to hold a BLE connection, several TCP sockets and GPS updates for hours with the screen off?

Establish from official Android documentation:

- Which foreground service types are needed (connected device, location) and what each demands on Android 14 and later (manifest, runtime permissions, user-visible notification).
- How Doze and app standby affect BLE notifications, open TCP sockets and location updates inside a foreground service; whether a battery-optimisation exemption or wake lock is needed.
- Background location permission rules when the service starts from the foreground.
- Known manufacturer-specific process killing and the documented mitigations.
- The officially recommended BLE API surface for a long-lived central connection (platform `BluetoothGatt` versus a maintained library), including MTU and write-throughput limits relevant to relaying VESC packets.
