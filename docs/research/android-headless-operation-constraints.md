# Android headless operation constraints

Research for the hub migration ticket "Android headless operation constraints". Researched 2026-10-01.

Question: what does current Android require for an app to hold a BLE connection to the board, about 9 outbound TCP sockets to the hub and GPS updates for hours with the screen off, while writing the log of record?

Each claim carries its source. **Doc** means an official page states it. **Source** means it was read from AOSP code on `refs/heads/main` (behaviour of stock Android today, not a documented contract, and manufacturers can change it). **Inference** means it is my conclusion from the cited facts.

## Summary

One foreground service declared `connectedDevice|location`, started from a visible activity after the Bluetooth and location runtime permissions are granted, is the documented shape for this workload. `ACCESS_BACKGROUND_LOCATION` is not needed. On stock Android the foreground service process state keeps network access and partial wake locks working through Doze, so a partial wake lock is needed and a battery-optimisation exemption is not required by AOSP, but the exemption is the main documented defence against manufacturer process killing. For BLE, Google documents only the platform `BluetoothGatt` API and recommends no library; Nordic's Android-BLE-Library is the maintained wrapper that adds the operation queue the platform lacks.

## 1. Foreground service types

### What to declare

- **Doc.** Apps targeting Android 14 (API 34) or higher must declare at least one type per foreground service. A missing type raises `MissingForegroundServiceTypeException` on `startForeground()`; a missing type permission or unmet runtime prerequisite raises `SecurityException`. https://developer.android.com/about/versions/14/changes/fgs-types-required
- **Doc.** Types can be combined on one service. Passing a type to `startForeground()` that is not in the manifest throws `IllegalArgumentException`. `startForeground()` may be called again later with a different combination of the declared types. https://developer.android.com/develop/background-work/services/fgs/launch

| | `connectedDevice` | `location` |
|---|---|---|
| Manifest `android:foregroundServiceType` | `connectedDevice` | `location` |
| Manifest permission | `FOREGROUND_SERVICE` plus `FOREGROUND_SERVICE_CONNECTED_DEVICE` | `FOREGROUND_SERVICE` plus `FOREGROUND_SERVICE_LOCATION` |
| `startForeground()` constant | `FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE` | `FOREGROUND_SERVICE_TYPE_LOCATION` |
| Runtime prerequisite | At least one of: a declared `CHANGE_NETWORK_STATE`, `CHANGE_WIFI_STATE`, `CHANGE_WIFI_MULTICAST_STATE`, `NFC` or `TRANSMIT_IR`; or a granted `BLUETOOTH_CONNECT`, `BLUETOOTH_ADVERTISE`, `BLUETOOTH_SCAN` or `UWB_RANGING`; or a `UsbManager.requestPermission()` call | Location services enabled by the user, and `ACCESS_COARSE_LOCATION` or `ACCESS_FINE_LOCATION` granted |
| Documented purpose | "Interactions with external devices that require a Bluetooth, NFC, IR, USB, or network connection." | "Long-running use cases that require location access, such as navigation and location sharing." |

Source for the table: https://developer.android.com/develop/background-work/services/fgs/service-types

- **Doc.** Permissions must be requested and granted before `startForeground()` is called. https://developer.android.com/about/versions/14/changes/fgs-types-required
- **Inference.** The TCP sockets to the hub need no type of their own: the `connectedDevice` description covers a "network connection", and `dataSync` (the other candidate) is capped at 6 hours per 24 hours on Android 15 (see below), so it should be avoided.

### Bluetooth permissions

- **Doc.** Targeting Android 12 or higher: `BLUETOOTH_SCAN` to look for devices, `BLUETOOTH_CONNECT` to talk to them. Both are runtime permissions, shown to the user as "Nearby devices". Legacy `BLUETOOTH` and `BLUETOOTH_ADMIN` are kept with `android:maxSdkVersion="30"`. `BLUETOOTH_SCAN` can carry `android:usesPermissionFlags="neverForLocation"` when scan results are not used to derive location. https://developer.android.com/develop/connectivity/bluetooth/bt-permissions

### Notification

- **Doc.** A foreground service must show a notification with priority `PRIORITY_LOW` or higher, and the notification ID must not be 0. https://developer.android.com/develop/background-work/services/fgs/launch
- **Doc.** `POST_NOTIFICATIONS` (Android 13+) is not needed to launch a foreground service. If the user denies it, the service still runs and is listed in the Task Manager but not in the notification drawer. https://developer.android.com/develop/ui/views/notifications/notification-permission
- **Doc.** Since Android 14 the user can dismiss a foreground service notification even with `setOngoing(true)`, except while the phone is locked or through "Clear all". https://developer.android.com/about/versions/14/behavior-changes-all
- **Doc.** Since Android 13 the user can press Stop in the Task Manager. The whole app is removed from memory and no callback is delivered; on next start `ApplicationExitInfo` reports `REASON_USER_REQUESTED`. https://developer.android.com/develop/background-work/services/fgs/handle-user-stopping

### Android 15 and 16

- **Doc.** Android 15 (target 35): `dataSync` and the new `mediaProcessing` type are limited to 6 hours in 24 hours, after which `Service.onTimeout(int, int)` is called. `BOOT_COMPLETED` receivers may not launch `dataSync`, `camera`, `mediaPlayback`, `phoneCall`, `mediaProjection` or `microphone` services. Neither restriction names `connectedDevice` or `location`. https://developer.android.com/about/versions/15/behavior-changes-15
- **Doc.** Android 15 (all apps): a network request started "outside of a valid process lifecycle" fails with `UnknownHostException` or another socket `IOException`; the page names a foreground service as a way to keep such work valid. https://developer.android.com/about/versions/15/behavior-changes-all
- **Doc.** Android 16 (all apps): jobs running alongside a foreground service now count against the job runtime quota. This affects `JobScheduler`/WorkManager work, not the service itself. Bond loss is now handled by the system: the link is disconnected, the bond kept and a system dialog shown. https://developer.android.com/about/versions/16/behavior-changes-all
- **Doc.** Android 16 (target 36): new `ACTION_KEY_MISSING` and `ACTION_ENCRYPTION_CHANGE` Bluetooth intents; local network permission is opt-in at this stage and concerns local network addresses. No change to `connectedDevice` or `location` service types is listed. https://developer.android.com/about/versions/16/behavior-changes-16
- **Inference.** The hub is a public internet host, so the Android 16 local network permission does not apply to the public hub. A self-run hub on a LAN address would fall under it once enforced.

### Google Play

- **Doc.** For apps targeting Android 14+, each foreground service type must be declared in Play Console (App content) with a description, the user impact if the task is deferred, and a video. Listed use cases include external device interaction over Bluetooth or network for `connectedDevice`, and navigation or location sharing for `location`. https://support.google.com/googleplay/android-developer/answer/13392821
- **Inference.** This applies only if the app is distributed through Play. The map leaves distribution open and mentions an APK.

## 2. Doze, App Standby, wake locks and the battery-optimisation exemption

### What the docs state

- **Doc.** In Doze the system suspends network access, ignores wake locks, defers alarms and jobs and stops Wi-Fi scans. https://developer.android.com/training/monitoring-device-state/doze-standby
- **Doc.** Deep Doze needs screen off, on battery and stationary; light Doze needs only screen off and on battery, and restricts network access and jobs outside maintenance windows. The device "immediately leaves Doze if motion is detected". https://source.android.com/docs/core/power/platform_mgmt
- **Doc.** An app is not idle for App Standby while it "has a process in the foreground (activity or foreground service)". The same page warns not to start a foreground service merely to avoid being idle. https://developer.android.com/training/monitoring-device-state/doze-standby
- **Doc.** The power limits table lists network access as "No restrictions" for an app process running a foreground service. https://developer.android.com/topic/performance/power/power-details
- **Doc.** An app on the battery-optimisation exemption list "can use the network and hold partial wake locks during Doze and App Standby". Apps may fire `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` only for acceptable use cases; "Peripheral device companion app" whose core function is a persistent connection to the peripheral to give it internet access is listed as acceptable, and Play policy prohibits the request unless core function is adversely affected. https://developer.android.com/training/monitoring-device-state/doze-standby
- **Doc.** The wake lock guidance says to use a wake lock from a foreground service only when it would hurt the user experience for the device to suspend while the service runs with the screen off, and to hold it inside a foreground service so the user sees a notification. https://developer.android.com/develop/background-work/background-tasks/awake and https://developer.android.com/develop/background-work/background-tasks/awake/wakelock/best-practices
- **Doc.** If the user puts the app in the "restricted" battery state, on AOSP it cannot launch foreground services and existing ones are removed from the foreground. "The precise restrictions imposed are determined by the device manufacturer." https://developer.android.com/topic/performance/background-optimization

### What AOSP code does

- **Source.** Network in Doze: `NetworkPolicyManager.isProcStateAllowedWhileIdleOrPowerSaveMode()` returns true when `procState <= FOREGROUND_THRESHOLD_STATE`, defined as `PROCESS_STATE_BOUND_FOREGROUND_SERVICE`, and is documented in code as "considered foreground and as such will be allowed to access network when the device is idle or in battery saver mode". `NetworkPolicyManagerService.updateRulesForAllowlistedPowerSaveUL()` sets the Doze firewall chain to allow for such uids. https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/net/NetworkPolicyManager.java and https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/services/core/java/com/android/server/net/NetworkPolicyManagerService.java
- **Source.** Wake locks in Doze: `PowerManagerService.setWakeLockDisabledStateLocked()` disables a partial wake lock in device idle only when the uid is not allowlisted and its `procState > PROCESS_STATE_BOUND_FOREGROUND_SERVICE`. https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/services/core/java/com/android/server/power/PowerManagerService.java
- **Source.** `PROCESS_STATE_FOREGROUND_SERVICE` is declared before `PROCESS_STATE_BOUND_FOREGROUND_SERVICE` in `ActivityManager`, i.e. a running foreground service is within both thresholds. (Ordering read from declaration order; numeric values not checked.) https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/app/ActivityManager.java
- **Source.** `DeviceIdleController` leaves idle on motion (`handleMotionDetectedLocked()` leads to `becomeActiveLocked()`), except under Quick Doze, where motion is ignored. https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/apex/jobscheduler/service/java/com/android/server/DeviceIdleController.java

### Consequences for the three resources

- **TCP sockets. Inference** from the source and the power-details table: on stock Android an app with a running foreground service keeps network access in light and deep Doze, so open sockets to the hub are not cut by Doze. No official page states this for sockets specifically.
- **CPU. Inference** from the wake lock guidance: a foreground service alone does not stop the CPU suspending with the screen off. Polling the board at a fixed rate, relaying and writing the log need a `PARTIAL_WAKE_LOCK` held for the session; on stock Android that lock is honoured in Doze because of the foreground service state.
- **Location. Doc.** Background location limits (a few updates per hour) do not apply while the app has a foreground service: it is then treated as a foreground app. https://developer.android.com/about/versions/oreo/background-location-limits
- **Location under Battery Saver. Source.** `PowerManager` defines Battery Saver location modes including `LOCATION_MODE_GPS_DISABLED_WHEN_SCREEN_OFF`, `LOCATION_MODE_ALL_DISABLED_WHEN_SCREEN_OFF` and `LOCATION_MODE_THROTTLE_REQUESTS_WHEN_SCREEN_OFF`; `LOCATION_MODE_FOREGROUND_ONLY` keeps fixes for foreground apps. Which mode a device uses is device configuration. https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/os/PowerManager.java **Inference:** Battery Saver, not Doze, is the stock mechanism that can stop GPS with the screen off, so it should be off during a run.
- **BLE.** The Doze page does not list Bluetooth among its restrictions. **Doc:** "There is no restriction on connecting to a device while the app is in the background, although the connection is closed if your process is killed", and a `connectedDevice` foreground service is the documented way to keep a connection alive "as long as possible". https://developer.android.com/develop/connectivity/bluetooth/ble/background
- **Exemption. Inference:** on AOSP the exemption is redundant for network and wake locks while the foreground service runs. It is still worth requesting because it is one of the documented exemptions that lets the app start a foreground service from the background (useful for restart after a kill) and because it is the first mitigation on manufacturer skins (section 4). Doc for the background-start exemption: https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start

## 3. Background location permission

- **Doc.** Foreground location is access while an activity is visible or while "running a foreground service" with the `location` type; the app "retains access when it's placed in the background, such as when the user presses the Home button on their device or turns their device's display off". Background location (`ACCESS_BACKGROUND_LOCATION`) is any access outside those situations. https://developer.android.com/develop/sensors-and-location/location/permissions
- **Doc.** A `location` foreground service cannot be created while the app is in the background unless `ACCESS_BACKGROUND_LOCATION` is granted. With only a while-in-use grant, creating it from the background throws `SecurityException`, and `checkSelfPermission()` does not reveal the problem because it returns `PERMISSION_GRANTED` in the background too. The service must be started "while your app has a visible activity". https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start
- **Doc.** Exemptions from the while-in-use rule include a service started by the user interacting with a notification or an app widget. https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start
- **Inference.** Starting the run from the app's screen with `ACCESS_FINE_LOCATION` granted "while using the app" is enough for GPS through hours of screen-off. The cost: if the process dies mid-run, the service cannot be restarted from the background with location (a sticky restart or a boot receiver would throw); the user must reopen the app or tap a notification. Requesting `ACCESS_BACKGROUND_LOCATION` would remove that limit and, on Play, triggers the background location declaration (https://support.google.com/googleplay/android-developer/answer/9799150).
- **Doc.** If the user grants only approximate location, the app gets about 3 square kilometres of accuracy regardless of what it declared. https://developer.android.com/develop/sensors-and-location/location/permissions

## 4. Manufacturer-specific process killing

Official Android documentation does not catalogue this. It only acknowledges that restriction details "are determined by the device manufacturer" (https://developer.android.com/topic/performance/background-optimization).

Everything below is from dontkillmyapp.com, which is a community site, **not an official source**.

- Ranking of worst offenders at the time of reading: Huawei, Xiaomi, OnePlus and Samsung at the top; then Meizu, Asus, Oppo, Vivo, Realme, Motorola and others; AOSP-like devices (Nokia, HTC) at the bottom. https://dontkillmyapp.com/
- Samsung: the site reports that from Android 11 a default-on restriction stops apps holding wake locks in foreground services unless excluded from battery optimisation, and that "Put unused apps to sleep" restricts apps unused for 3 days. User-side fix: exclude the app from battery optimisation and add it to "Never sleeping apps". The site lists no developer-side solution. https://dontkillmyapp.com/samsung
- Xiaomi: the site reports non-standard background limits. User-side fix: enable Autostart, set the app's battery saver to "No restrictions", and lock the app in recents. https://dontkillmyapp.com/xiaomi
- General mitigations the site lists are all user settings: disable battery optimisation for the app, remove background restrictions, vendor battery-saver whitelists. https://dontkillmyapp.com/general

**Inference.** The mitigations available to this project are: request the battery-optimisation exemption in-app (officially acceptable use case in section 2), document per-device settings for the one phone used in the car, and prefer a phone near the bottom of that ranking. Because the team controls the single phone, device choice and a one-time setup checklist are cheaper than code.

## 5. BLE API surface, MTU and write throughput

### Recommended API

- **Doc.** Google's BLE guides use the platform API only: `BluetoothDevice.connectGatt()`, `BluetoothGatt`, `BluetoothGattCallback`, hosted in a `Service`. No third-party library is recommended. https://developer.android.com/develop/connectivity/bluetooth/ble/connect-gatt-server
- **Doc.** Jetpack `androidx.bluetooth` exists but its latest release is `1.0.0-alpha02` (29 November 2023). https://developer.android.com/jetpack/androidx/releases/bluetooth **Inference:** not suitable as the production surface.
- **Doc.** For long-lived background connections Google names three options: a `connectedDevice` foreground service; `CompanionDeviceService` with `REQUEST_COMPANION_RUN_IN_BACKGROUND` and `startObservingDevicePresence()`; or WorkManager for short work. `autoConnect = true` makes the GATT client reconnect automatically when the peripheral becomes available again. https://developer.android.com/develop/connectivity/bluetooth/ble/background
- **Source.** `connectGatt` Javadoc: "Generally, the first ever connection to a device should be direct (autoConnect set to false) and subsequent connections to known devices should be invoked with the autoConnect parameter set to true." https://android.googlesource.com/platform/packages/modules/Bluetooth/+/refs/heads/main/framework/java/android/bluetooth/BluetoothGatt.java
- **Source.** `BluetoothGatt` allows one outstanding operation per connection: reads and writes take an `mDeviceBusy` flag and a second call returns `false` or `ERROR_GATT_WRITE_REQUEST_BUSY` until the callback for the first arrives. The platform provides no queue. Same file as above.
- **Nordic.** Android-BLE-Library (`no.nordicsemi.android:ble:2.11.0`, `minSdk 18`, `targetSdk 36`) wraps `BluetoothGatt` and adds: connection with automatic retries, an operation queue, splitting and merging of long packets, MTU and connection priority requests, operation timeouts. Its `useAutoConnect(true)` makes the first connection direct and later ones use `autoConnect`; Nordic notes it "should only be used with bonded devices" or devices with a static address. The README says a Kotlin BLE Library 2.0 will eventually replace it but is "not recommended for production use" yet. https://github.com/NordicSemiconductor/Android-BLE-Library and https://github.com/NordicSemiconductor/Android-BLE-Library/blob/main/ble/src/main/java/no/nordicsemi/android/ble/ConnectRequest.java
- **Inference.** There is no official recommendation of a library over the platform API. The platform API is the only Google-documented surface; Nordic's library is the maintained option that supplies the queue, retry and split logic the app would otherwise write itself.

### MTU

- **Doc.** From Android 14 the stack requests an ATT MTU of 517 when the first GATT client calls `requestMtu()` and ignores later requests on that link; the result is `min(517, remoteMtu)`. Google's sizing rule is `min(SUPPORTED_MTU, 517) - 5` bytes per write. https://developer.android.com/about/versions/14/behavior-changes-all
- **Source.** `requestMtu` Javadoc: a write without response is truncated to the MTU size. A characteristic value longer than `GATT_MAX_ATTR_LEN` (512) throws `IllegalArgumentException`. https://android.googlesource.com/platform/packages/modules/Bluetooth/+/refs/heads/main/framework/java/android/bluetooth/BluetoothGatt.java
- **Nordic.** The default splitter cuts data into "at-most MTU-3 bytes long packets". https://github.com/NordicSemiconductor/Android-BLE-Library/blob/main/ble/src/main/java/no/nordicsemi/android/ble/WriteRequest.java
- **Inference.** Without an MTU request the usable payload is 20 bytes (the Bluetooth default ATT MTU of 23 minus 3), so an 80-byte VESC frame takes 4 writes or 4 notifications. With any negotiated MTU of 85 or more a whole frame fits in one. The app must call `requestMtu()` once after connecting and size writes from `onMtuChanged`; the frame parser must still reassemble across notifications because the board's side decides how it chunks.

### Throughput

- **Source.** Default connection parameters per priority in AOSP (units of 1.25 ms): balanced 30 to 50 ms, high 11.25 to 15 ms, low power 100 to 125 ms. https://android.googlesource.com/platform/packages/modules/Bluetooth/+/refs/heads/main/android/app/res/values/config.xml
- **Source.** `CONNECTION_PRIORITY_HIGH` is documented as only for transferring "large amounts of data over LE quickly", after which the app should return to balanced. https://android.googlesource.com/platform/packages/modules/Bluetooth/+/refs/heads/main/framework/java/android/bluetooth/BluetoothGatt.java
- **Inference.** A 5 to 10 Hz poll means one request and one reply every 100 to 200 ms. At the balanced interval (at most 50 ms) there are at least two connection events per poll period, so the workload fits without `CONNECTION_PRIORITY_HIGH` provided the MTU is raised or frames are split across few packets. Using write-without-response for polls avoids waiting for an acknowledgement per write, but the one-operation-at-a-time rule still applies.

## Not established

- No official page states in words that open TCP sockets, BLE notifications or location callbacks keep working in Doze under a foreground service. The conclusion rests on AOSP source plus the power-details table, and manufacturers may change that code.
- Whether an incoming BLE notification or location fix wakes a suspended CPU on its own, and so whether the partial wake lock is strictly required for receive-only paths. The docs only give the decision rule for wake locks.
- Whether cellular carriers or NAT drop idle TCP connections to the hub with the screen off, and what keepalive interval is needed. Not an Android platform question; the viewer heartbeat at 1 Hz covers attached registrations only.
- Which Battery Saver location mode a given phone ships with.
- Whether deep Doze ever engages in a moving car. The docs say motion exits Doze; Quick Doze (entered with Battery Saver) ignores motion in AOSP source. Not tested.
- The MTU and characteristic properties the board's BLE module actually supports (write with or without response, notification chunk size). This is VESC firmware and module behaviour, outside the sources allowed for this ticket.
- Actual achievable throughput on the target phone; the figures above are default connection intervals, not measurements.
- The release date and maintenance cadence of Android-BLE-Library 2.11.0 beyond what its README and build file show.
- Manufacturer killing behaviour is documented only by a community site; no official vendor documentation was consulted, and the behaviour of the specific phone to be used in the car is unknown.
- Whether `CompanionDeviceManager` association would add protection on manufacturer skins. Google documents it as an option for background BLE; its effect on vendor task killers is not documented.
