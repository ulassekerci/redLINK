# 40: The stream to viewers

**What to build:** viewers see the car. Every reply the board gives to a poll is copied unchanged to each viewer that is watching, and every GPS fix is sent as a GPS message, so a joined desktop app shows moving gauges and a marker on the map. One slow viewer loses its own oldest samples and holds nobody else back.

Spec: [hub usage spec](../spec-hub.md) 2.4 (GPS, tolerance) and 2.6 ("What is copied", "GPS", "Slow viewer", "Cost"); [Android app spec](../spec-android.md) 2.6, 2.7 and `BridgeLobbyTest` in section 4.

**Blocked by:** 36 (Location during a run), 39 (Viewers join the bridge).

**Status:** resolved

- [x] Each setup and ADC reply frame is written unchanged to every active registration; nothing is added to a board frame
- [x] The firmware version reply is never copied
- [x] One GPS message is sent per fix to every active registration, with 0 for a field the fix lacks; nothing is sent while there is no fix
- [x] GPS keeps flowing while the board is unreachable
- [x] Replies and GPS follow the active-registration rule: nothing before the first heartbeat, and nothing after 3 s without one until the next arrives
- [x] Each registration has a small bounded queue; when it is full the oldest samples are dropped for that viewer only, and polling and the other viewers are unaffected
- [x] Nothing a viewer sends reaches the board
- [x] `BridgeLobbyTest` gains cases for the above (copied unchanged to active registrations only, the firmware reply held back, a full queue dropping its oldest for one viewer) and passes
- [ ] A desktop app joined to a simulated run shows the lap on its gauges

## Comments

Implemented in the `Bridge`, which now takes the board's replies and the phone's fixes as two flows beside the board state. The `BoardPoller` publishes every frame the board sends as `replies`, its payload, as it comes; the bridge picks what it copies. Run on the Android 16 emulator image against the public hub, with the desktop app's hub code run headless from a script as the viewer and fixes fed with `adb emu geo fix`: it went live about 1.2 s after Start and received 20 setup replies, 20 ADC replies and one GPS message a second, with speed and distance climbing along the lap. `Stop answering` moved it to board unreachable with GPS still arriving once a second, and back to live; Stop moved it to phone lost, then phone not found. The Electron window itself was not opened, so the gauges box is left for ticket 44, which runs the same path with the window; the gauges' path from a sample was checked in ticket 30.

- **Automated tests:** `BridgeLobbyTest` gains cases for replies copied unchanged to active registrations only, the firmware version reply and unknown frames held back, replies stopping at 3 s without a heartbeat, a GPS message per fix with 0 for what it lacks, GPS while the board is unreachable, a fix the message cannot carry, a stalled viewer losing its oldest while another gets everything, and nothing from before a silence written once a viewer is back. `BoardPollerTest` gains a case for `replies`.
- **What is copied:** setup and ADC replies only, chosen by command ID, so a late reply the poller passed over is still copied and one too short to read is copied as it came. A copied frame is the payload framed again, which gives back the board's bytes because the decoder takes only a frame that frames so.
- **The queue** is 64 frames per registration, shared with status and GPS, about 1.5 s of stream. When it is full the oldest frame goes, whatever its kind, so a status can go too; the next comes within a second.
- **A registration that stops being active** loses what was waiting for it, so a write stuck on a slow connection cannot be followed by seconds-old frames once the viewer is back.
- **A fix the GPS message cannot carry** in a field other than accuracy, such as a speed over 655 m/s, is not sent, as the protocol's "an encoder refuses the message" says; the log still has it.
- **Nothing a viewer sends reaches the board** by construction: the bridge is given the board's output only, and has no way to send to it.

