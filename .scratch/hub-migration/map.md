# Hub migration

Label: wayfinder:map

## Destination

A locked set of architecture decisions plus one spec per part (Android app, hub usage, Electron app), ready to hand to implementation. The specs describe a system where an Android app bridges the board to the VESC TCP Hub and logs every run, and the pit crew's laptops watch through an Electron app, with today's read-only dashboard as the feature bar. No code is built inside this map.

## Notes

- Domain: Shell Eco-marathon vehicle telemetry. Read `CONTEXT.md` for vocabulary and `tcp-hub.md` for the hub protocol before any ticket.
- Skills: `grilling` and `domain-modeling` for grilling tickets, `research` for research tickets, `prototype` for prototype tickets.
- Why the change: drop the self-hosted relay and drop the unreliable React Native app.
- Standing decisions from charting (not tickets):
  - Android first. The app is written natively and must run as a headless logger and bridge with the screen off; it also displays some information.
  - Target the public hub (`veschub.vedder.se:65101`), host and port configurable so a self-run stock hub is a drop-in fallback. No custom server code.
  - The stream stays pure VESC framing; phone GPS travels as `COMM_CUSTOM_APP_DATA`. Compatibility with VESC Tool (desktop or mobile) is not a goal: only our own apps attach to the phone's registrations. (Changed 2026-10-01; desktop VESC Tool attaching for debugging was dropped.)
  - The desktop app can also connect straight to the board over Bluetooth with no phone, for testing the vehicle. Pure VESC framing is kept so both paths share one parser.
  - The phone owns polling at a fixed rate; desktops listen.
  - The phone log is the log of record: a CSV per run including GPS, exported through the share sheet. The desktop keeps its CSV export as a convenience copy.
  - Several laptops watching at once is a hard requirement. Assume 3-8 laptops with no shared LAN.
  - Electron targets macOS and Windows; its renderer is the existing React dashboard with the data layer swapped.
  - New apps live in this repo and replace `mobile/`, `server/` and `web/`; the old stack stays until parity.

## Decisions so far

<!-- one line per resolved ticket -->

- [Hub limits for multiple registrations](issues/01-hub-limits-for-multiple-registrations.md): one phone can hold several registrations with no cap or idle timeout in the hub code, but unattached registrations buffer without bound and nothing is published about the public hub's capacity.
- [How desktop VESC Tool behaves as a hub client](issues/03-how-desktop-vesc-tool-behaves-as-a-hub-client.md): it dispatches packets by command ID alone, so the phone's unsolicited stream is harmless and custom app data is ignored, but it needs a firmware version reply within about 2 s and sends writes (including an Esc-key motor stop) that the bridge must filter with an allow-list.
- [How several pit laptops watch at once](issues/04-how-several-pit-laptops-watch-at-once.md): one registration per viewer, handed out through a lobby using a per-launch token and confirmed by `PING`; viewers send a 1 Hz heartbeat, the phone never refuses a viewer, and 8 is the tested number.
- [Android headless operation constraints](issues/02-android-headless-operation-constraints.md): one foreground service typed `connectedDevice|location`, started from a visible activity with a partial wake lock, is the documented shape and needs no background location permission; survival under Doze rests on AOSP source, and manufacturer killing on the actual phone is untested.
- [What travels on the stream](issues/05-what-travels-on-the-stream.md): the phone polls `COMM_GET_VALUES` and decoded ADC at 20 Hz and copies replies unchanged; GPS, lobby request, heartbeat and status are `COMM_CUSTOM_APP_DATA` messages told apart by a type byte, and a 1 Hz status message lets a viewer tell live from board unreachable from phone lost.
- [Android runtime and stack](issues/07-android-runtime-and-stack.md): a run is one foreground service from Start to Stop that resumes after a kill or crash but not a reboot, with six grants blocking Start; Kotlin, Compose and the Nordic BLE library on Android 12+. It also amended the viewer's "phone lost" rule, because a silent network drop leaves stale registrations on the hub.

## Not yet specified

- Writing the three specs themselves, once the decisions they rest on are closed. Likely one ticket per part.
- Distribution: how the APK reaches the phone, and how the Electron app is packaged, signed and updated for the pit crew.
- Testing without the car: some stand-in for the board and for the hub so both apps can be exercised on a desk.
- Self-run hub fallback: what triggers switching to it and who runs it on race day. Depends on what the hub limits research finds.
- Trip semantics: whether a trip is a phone concept, a per-laptop concept, or both, once the log and desktop architecture are decided.

## Out of scope

- iOS app: a later effort that follows the Android spec.
- Control, configuration or firmware commands from the dashboard: it stays read-only.
- New dashboard features beyond today's (gauges, ADC, map, trip meter, CSV).
- Any custom relay or server code.
- Desktop VESC Tool attaching through the hub: dropped while resolving [How several pit laptops watch at once](issues/04-how-several-pit-laptops-watch-at-once.md). Its 2 s connect deadline and Esc motor stop made a debug registration costly; debugging connects VESC Tool to the board directly.
- Forwarding viewer commands to the board: not for now, may be reconsidered later. Closed [What a viewer may send to the board](issues/06-what-a-viewer-may-send-to-the-board.md).
