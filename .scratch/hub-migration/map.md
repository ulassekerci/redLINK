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
  - The stream stays pure VESC framing; phone GPS travels as `COMM_CUSTOM_APP_DATA`. Desktop VESC Tool can attach for debugging. Stock VESC Tool mobile is not required to substitute for the app.
  - The phone owns polling at a fixed rate; desktops listen.
  - The phone log is the log of record: a CSV per session including GPS, exported through the share sheet. The desktop keeps its CSV export as a convenience copy.
  - Several laptops watching at once is a hard requirement. Assume 3-8 laptops with no shared LAN.
  - Electron targets macOS and Windows; its renderer is the existing React dashboard with the data layer swapped.
  - New apps live in this repo and replace `mobile/`, `server/` and `web/`; the old stack stays until parity.

## Decisions so far

<!-- one line per resolved ticket -->

- [Hub limits for multiple registrations](issues/01-hub-limits-for-multiple-registrations.md): one phone can hold several registrations with no cap or idle timeout in the hub code, but unattached registrations buffer without bound and nothing is published about the public hub's capacity.
- [How desktop VESC Tool behaves as a hub client](issues/03-how-desktop-vesc-tool-behaves-as-a-hub-client.md): it probes the firmware version with a 2 s deadline, reads configs and scans CAN once, then stays silent; it acts on unsolicited packets by command ID, ignores `COMM_CUSTOM_APP_DATA`, and its Esc key sends a motor stop, so a bridge needs an allow-list (read from code, not tested).

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
