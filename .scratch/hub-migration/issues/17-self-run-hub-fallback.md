# Self-run hub fallback

Type: grilling
Status: resolved

Map: [Hub migration](../map.md)

## Question

Host and port are configurable so a self-run stock hub is a drop-in fallback for the public one. [Hub limits for multiple registrations](01-hub-limits-for-multiple-registrations.md) found nothing published about the public hub's capacity, and [Stale re-registration on the public hub](13-stale-re-registration-on-the-public-hub.md) found that strangers' connections can delay ours. When is the fallback used, and who runs it?

Decide: whether a self-run hub is set up ahead of race day or only after the public hub fails; where it runs and who keeps it up; what observed failure triggers the switch; how the phone and 3-8 laptops are all moved to the new host and port in the pit, given the team code carries neither.

## Answer

The fallback is a self-hosted hub that nobody keeps running: it is proven once at a desk, and after that it is started only when the public hub fails. Decided by grilling on 2026-10-02. The reason for proving it at all is that the public hub comes with no service promise ([Hub limits for multiple registrations](01-hub-limits-for-multiple-registrations.md)). A hub outage costs live viewing only; the log of record is written on the phone either way.

- **Readiness:** no standing hub and no written recipe or script. A standing hub was rejected because the migration exists to stop running a server.
- **Where and who:** a small rented Linux server with a public address, on port 65101 so only the host differs, started by the software side of the team (Ulaş). A laptop in the pit cannot host it: the phone is on mobile data and the laptops share no LAN. On race day it is set up from scratch.
- **Start command:** `vesc_tool --tcpHub 65101 --offscreen`. In hub mode VESC Tool starts without a window, and `--offscreen` removes the need for a display (`main.cpp:1580-1591`, read from the source). Whether the released Linux build starts on a bare server without extra libraries is not established; the desk test finds out.
- **Desk test:** done once, before the acceptance session, then the server is destroyed. The phone (a simulated run is enough) and two laptops are moved to the self-hosted hub by typing the host; both laptops join and show live values; all three are moved back to the public hub and join again. It is one item on the desk checklist from [Testing without the car](16-testing-without-the-car.md), so it gates the acceptance session. It is not repeated.
- **Trigger:** a person on the software side decides, arranges the server and tells the pit crew to switch. There is no automatic failover (the phone and a laptop could land on different hubs, each looking healthy) and no written rule for when to switch: the pit crew does not read the specs.
- **Moving devices:** host and port are typed by hand on the phone's Setup screen and in each laptop's settings, as [How hub credentials are set and shared](10-how-hub-credentials-are-set-and-shared.md) left it. A stored backup hub was rejected because a rented server's address is not known ahead of time; the phone announcing the new host was rejected because viewers cannot reach the phone when the hub is down. The team code does not change.
- **Phone lock:** host and port cannot be changed during a run, like "New code". Switching means Stop, edit, Start, which splits the log of record into two files.
- **Display:** both apps show the host and port only when they differ from the public hub: on the phone in the Hub row, on the laptop on the waiting screen beside the team code. A laptop left on the old host otherwise looks the same as the phone not running.

"Self-hosted hub" was added to `CONTEXT.md`.
