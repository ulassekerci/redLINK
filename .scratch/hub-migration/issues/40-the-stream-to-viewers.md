# 40: The stream to viewers

**What to build:** viewers see the car. Every reply the board gives to a poll is copied unchanged to each viewer that is watching, and every GPS fix is sent as a GPS message, so a joined desktop app shows moving gauges and a marker on the map. One slow viewer loses its own oldest samples and holds nobody else back.

Spec: [hub usage spec](../spec-hub.md) 2.4 (GPS, tolerance) and 2.6 ("What is copied", "GPS", "Slow viewer", "Cost"); [Android app spec](../spec-android.md) 2.6, 2.7 and `BridgeLobbyTest` in section 4.

**Blocked by:** 36 (Location during a run), 39 (Viewers join the bridge).

**Status:** ready-for-agent

- [ ] Each setup and ADC reply frame is written unchanged to every active registration; nothing is added to a board frame
- [ ] The firmware version reply is never copied
- [ ] One GPS message is sent per fix to every active registration, with 0 for a field the fix lacks; nothing is sent while there is no fix
- [ ] GPS keeps flowing while the board is unreachable
- [ ] Replies and GPS follow the active-registration rule: nothing before the first heartbeat, and nothing after 3 s without one until the next arrives
- [ ] Each registration has a small bounded queue; when it is full the oldest samples are dropped for that viewer only, and polling and the other viewers are unaffected
- [ ] Nothing a viewer sends reaches the board
- [ ] `BridgeLobbyTest` gains cases for the above (copied unchanged to active registrations only, the firmware reply held back, a full queue dropping its oldest for one viewer) and passes
- [ ] A desktop app joined to a simulated run shows the lap on its gauges
