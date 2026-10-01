# Stale re-registration on the public hub

Type: task
Status: open

Map: [Hub migration](../map.md)

## Question

When the phone's network drops silently, the hub keeps its registrations. Can the phone then register the same lobby ID again from a new socket, and does that new socket receive what viewers send?

Test against the public hub (`veschub.vedder.se:65101`) and record:

- Register an ID, then cut the socket without closing it (drop the packets or pull the network), and register the same ID from a new socket. Does a `VESCTOOL` login reach the new socket, the old one, or neither? The hub limits research read `tcphub.cpp:111-126` as possibly leaving the new socket unregistered.
- How long the hub keeps a silently dead registration answering `PONG`, with and without a client writing into it.
- When several `VESCTOOL` logins hit one registration within a few tens of milliseconds, each sending one packet right after the login line, how many packets arrive.

If a stale lobby cannot be replaced reliably, say so plainly: the lobby design in [How several pit laptops watch at once](04-how-several-pit-laptops-watch-at-once.md) then needs revisiting.
