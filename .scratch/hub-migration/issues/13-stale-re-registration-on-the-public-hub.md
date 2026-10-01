# Stale re-registration on the public hub

Type: task
Status: resolved

Map: [Hub migration](../map.md)

## Question

When the phone's network drops silently, the hub keeps its registrations. Can the phone then register the same lobby ID again from a new socket, and does that new socket receive what viewers send?

Test against the public hub (`veschub.vedder.se:65101`) and record:

- Register an ID, then cut the socket without closing it (drop the packets or pull the network), and register the same ID from a new socket. Does a `VESCTOOL` login reach the new socket, the old one, or neither? The hub limits research read `tcphub.cpp:111-126` as possibly leaving the new socket unregistered.
- How long the hub keeps a silently dead registration answering `PONG`, with and without a client writing into it.
- When several `VESCTOOL` logins hit one registration within a few tens of milliseconds, each sending one packet right after the login line, how many packets arrive.

If a stale lobby cannot be replaced reliably, say so plainly: the lobby design in [How several pit laptops watch at once](04-how-several-pit-laptops-watch-at-once.md) then needs revisiting.

## Answer

Yes: a stale lobby can be replaced reliably. Registering the same ID from a new socket takes over at once, and the new socket receives what viewers send. The lobby design stands. Tested on 2026-10-02 against `veschub.vedder.se:65101` with throwaway IDs, from a Mac on a home network, not from a phone on a mobile network.

How a silent drop was produced: after registering, the socket's IP TTL was set to 1, so everything it sent afterwards died at the first router and the hub saw a peer that had gone quiet with no FIN or RST. A check confirmed bytes written after that never reached an attached client. Packets from the hub still reached the Mac, which a real dead phone would not see; the hub cannot tell the difference.

- **Re-registration:** in six cases, each run twice, the new socket answered `PONG` within 0.3 s, received the packet a `VESCTOOL` login sent, and its reply reached that client. The cases: old socket idle or silent; no viewer, a viewer that had written and left, or a viewer still attached; same or changed password. The hub closed the old socket and any viewer attached to it. With a changed password the old password was refused.
- **The race in `tcphub.cpp:111-126` is real but needs a large backlog.** With 1 MiB written by a viewer into the silent old registration, the new registration answered `PONG` at first, then vanished 986 s later while its socket stayed open; the hub gave no sign, and a login was reset. Registering again repaired it, and the orphaned socket was left open, so the bridge must close it itself. With a realistic backlog (30 packets of 20 bytes) the new registration was still registered and delivering in both directions 1,312 s later, checked every 60 s. Heartbeats and lobby requests never build such a backlog.
- **How long a silent registration answers `PONG`:**
  - With a viewer writing 20 bytes a second into it: the viewer's socket was closed at 993 s and `PING` turned to `NULL` at about 1,000 s (16.7 minutes).
  - With one packet written once and the viewer gone: the same, `NULL` at about 1,000 s.
  - With nothing ever written to it: `PONG` until 2 h 12 min, `NULL` at 2 h 13 min (7,980 s, checked every 60 s). This matches the hub's TCP keep-alive (`tcphub.cpp:86`) at Linux defaults, 7,875 s.
- **Several logins at once:** with 2, 4 or 8 `VESCTOOL` logins inside about 30 ms, each sending one packet 50 to 500 ms after its login line, exactly one packet arrived per trial (72 trials). Which viewer won varied. With realistic viewers (connect, login line, 50 ms, packet, close) started a fixed time apart: 0 or 20 ms apart, one of four arrived; 50 ms apart, two to four of four; 100 ms or more apart, all four in every trial. Waiting 150 ms before the packet widened this: 100 ms apart still lost three of four, 200 ms apart lost none. Connect time to the hub was 74 ms.
- **Bytes in the same write as the login line are lost,** in every trial. A packet sent as a separate write with no pause arrived 1 time in 10; with a 10 ms pause or more, 10 of 10.
- **Someone else's connection can swallow ours.** A connection opened just after ours that sent nothing for 3 s caused the hub to discard everything we sent for those 3 s; one silent for 6 s got our connection reset at 5.3 s. This is the nested handshake described in [Hub limits for multiple registrations](01-hub-limits-for-multiple-registrations.md), now seen live. A pause after the login line therefore guarantees nothing: a viewer cannot know when its login took effect.

What this means:

- The rule from [Android runtime and stack](07-android-runtime-and-stack.md) holds: on a hub connection drop the bridge re-registers the lobby and confirms it with `PING`. It must close its own old sockets. Stale viewer registrations linger on the hub for about 17 minutes and harm nothing, because viewers return with fresh tokens.
- The amended "phone lost" rule in [What travels on the stream](05-what-travels-on-the-stream.md) is right: `PING` on a viewer's old registration says `PONG` for at least 16 minutes after the phone is gone.
- A lobby request can be lost without any collision. The existing retry (no `PONG` after a couple of seconds, repeat) is what makes joining reliable, not the timing of the request.

Handed on: how the viewer sends its lobby request (separate write after the login line, how long to stay, the retry spacing) goes to [Desktop app architecture](11-desktop-app-architecture.md).

Not tested: a phone on a mobile network, where the carrier may reset a dead connection on the phone's behalf; the backlog size at which the race starts, between 600 bytes and 1 MiB.

Scripts and logs: [assets/13-stale-re-registration](../assets/13-stale-re-registration/).
