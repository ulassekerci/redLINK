# How several pit laptops watch at once

Type: grilling
Status: resolved
Blocked by: 01

Map: [Hub migration](../map.md)

## Question

The hub bridges one client to one registration, but the whole pit crew (assume 3-8 laptops, no shared LAN) must watch at once. What mechanism gives every laptop the stream?

Candidates to weigh: the phone holds one hub registration per viewer slot; one desktop attaches and re-broadcasts; something else the hub research turns up. Decide the mechanism, how many viewers it supports, and how a laptop ends up on a free slot.

## Answer

The phone holds one registration per viewer, handed out through a lobby. Decided by grilling on 2026-10-01, on top of [Hub limits for multiple registrations](01-hub-limits-for-multiple-registrations.md).

- **Mechanism:** the phone opens one registration per viewer and writes the same stream to each. Re-broadcast from one laptop and a patched hub were rejected: both need custom server code or a shared LAN.
- **Joining:** a laptop needs only the shared credentials, no seat number. It makes a random token at launch, attaches to the lobby, sends one packet carrying the token and disconnects at once. The phone opens a registration whose ID includes that token. The laptop asks the hub with `PING` whether its own ID is registered, and attaches once the answer is `PONG`. With no `PONG` after a couple of seconds it repeats the lobby step.
- **Collisions:** a viewer that is watching is never kicked, because no other laptop knows its ID. Two laptops can displace each other only in the lobby, within about one network round trip; the lost request is retried silently.
- **Staying:** each viewer sends a heartbeat about once a second. After about 10 s of silence the phone closes that registration, which also keeps the phone from streaming into an empty one. A laptop that finds its registration gone returns to the lobby.
- **Capacity:** the phone never refuses a viewer. The design is tested with 8 and promises nothing above that.
- **Board link down:** registrations follow the phone's hub connection, not the board's. If Bluetooth to the board drops, viewers stay attached and are told the board is unreachable.
- **Viewer to board:** the phone forwards nothing from a viewer to the board for now.
- **VESC Tool:** attaching desktop VESC Tool through the hub was dropped (see the map's Out of scope). Pure VESC framing is kept so the desktop app can also connect straight to the board over Bluetooth.

Handed on: the packet forms for the lobby request, heartbeat and "board unreachable" signal go to [What travels on the stream](05-what-travels-on-the-stream.md); how the lobby and viewer IDs are derived goes to [How hub credentials are set and shared](10-how-hub-credentials-are-set-and-shared.md).
