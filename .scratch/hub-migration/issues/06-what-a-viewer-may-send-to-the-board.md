# What a viewer may send to the board

Type: grilling
Status: closed (out of scope)
Blocked by: 03, 04

Map: [Hub migration](../map.md)

## Question

The dashboard is read-only, yet desktop VESC Tool should be able to attach for debugging, and anyone bridged can send any VESC command. What does the phone forward from a viewer to the board?

Decide whether the phone forwards everything, filters by command ID, or distinguishes a debugging attachment from ordinary viewers, and what happens to the phone's own polling and log while a debugging session is active.

## Comments

Closed 2026-10-01 without a session of its own. [How several pit laptops watch at once](04-how-several-pit-laptops-watch-at-once.md) dropped desktop VESC Tool attaching through the hub and decided the phone forwards nothing from a viewer to the board for now, which leaves nothing to decide here. May return as a fresh effort if viewer commands are wanted later. The command classification in [How desktop VESC Tool behaves as a hub client](03-how-desktop-vesc-tool-behaves-as-a-hub-client.md) is the starting point if it does.
