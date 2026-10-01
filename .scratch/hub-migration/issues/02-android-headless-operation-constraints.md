# Android headless operation constraints

Type: research
Status: resolved

Map: [Hub migration](../map.md)

## Question

What does current Android require for an app to hold a BLE connection, several TCP sockets and GPS updates for hours with the screen off?

Establish from official Android documentation:

- Which foreground service types are needed (connected device, location) and what each demands on Android 14 and later (manifest, runtime permissions, user-visible notification).
- How Doze and app standby affect BLE notifications, open TCP sockets and location updates inside a foreground service; whether a battery-optimisation exemption or wake lock is needed.
- Background location permission rules when the service starts from the foreground.
- Known manufacturer-specific process killing and the documented mitigations.
- The officially recommended BLE API surface for a long-lived central connection (platform `BluetoothGatt` versus a maintained library), including MTU and write-throughput limits relevant to relaying VESC packets.

## Answer

One foreground service typed `connectedDevice|location`, started from a visible activity and holding a partial wake lock, is the documented shape for this workload; background location permission is not needed. Read from developer.android.com, AOSP source and the Nordic Android-BLE-Library repo on 2026-10-01. **Nothing was run on a phone.**

- **Foreground service types:** targeting Android 14+ needs `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_CONNECTED_DEVICE` and `FOREGROUND_SERVICE_LOCATION`, with both types in the manifest and in `startForeground()`. Before `startForeground()` the app must hold `BLUETOOTH_CONNECT`/`BLUETOOTH_SCAN` and a coarse or fine location permission, and location services must be on. A notification is mandatory, but `POST_NOTIFICATIONS` is not; the user can dismiss it or stop the app from Task Manager with no callback. Android 15/16 add nothing for these two types. `dataSync` is capped at 6 h per 24 h, so avoid it.
- **Doze and standby:** the docs say Doze suspends network and ignores wake locks, and that an app with a foreground service is not idle and has unrestricted network. AOSP source exempts foreground-service process state from both the Doze firewall and wake-lock disabling. The service alone does not keep the CPU awake (inferred from the wake-lock guidance), so hold a `PARTIAL_WAKE_LOCK`. The battery-optimisation exemption is redundant on AOSP but an officially acceptable request for a peripheral companion app. Battery Saver, not Doze, can switch GPS off with the screen off.
- **Background location:** a `location` foreground service started while an activity is visible counts as foreground location and keeps access with the screen off. `ACCESS_BACKGROUND_LOCATION` is needed only to start or restart the service from the background, which otherwise throws `SecurityException`.
- **Manufacturer killing:** official docs say only that restrictions are manufacturer-determined. dontkillmyapp.com (not official) ranks Huawei, Xiaomi, OnePlus and Samsung worst. Its mitigations are user settings: battery-optimisation exemption, autostart, "never sleeping" lists, locking the app in recents.
- **BLE API:** Google documents only platform `BluetoothGatt` and recommends no library; `androidx.bluetooth` is still alpha. The platform allows one operation at a time with no queue. Nordic Android-BLE-Library 2.11.0 adds the queue, retries, splitting and autoConnect handling. On Android 14+ the first `requestMtu()` negotiates `min(517, remote)`. The default payload is 20 bytes, so an 80-byte frame needs an MTU of 85 or more to fit in one packet. The balanced connection interval is 30-50 ms, enough for 5-10 Hz (inferred).
- **Not established:**
  - No official sentence confirms that sockets, BLE notifications or location survive Doze under a foreground service; this rests on AOSP source.
  - Whether BLE or GPS events wake a suspended CPU without a wake lock.
  - Carrier or NAT idle timeouts on the hub sockets.
  - The board's actual MTU and characteristic properties.
  - Measured throughput on the target phone.
  - How the specific phone's manufacturer skin behaves.

Full findings: branch `research/android-headless-operation-constraints`, file `docs/research/android-headless-operation-constraints.md`.
