# 30: Viewer joins the hub and goes live

**What to build:** a laptop with a team code watches the car by itself. On launch the app visits the lobby, gets its own registration, attaches, sends its heartbeat and shows the stream on the dashboard, with no click. While it is not live it shows the waiting screen and says why on the status line; when the phone or the board drops out it says so, dims, and comes back by itself.

The hub client lives in the main process and hands the renderer parsed samples and one connection state. Leaving the hub for a direct link, and rejoining after a settings change, are ticket 31.

Spec: [desktop app spec](../spec-desktop.md) 2.2, 2.3, 2.9 (map, middle section, dimming), 3.1, 3.2 and the `hub-client` suite in section 4; [hub usage spec](../spec-hub.md) 2.1, 2.5, 2.9 and section 3.

**Blocked by:** 27 (Direct link on the new protocol), 29 (Settings screen and team code).

**Status:** ready-for-agent

- [ ] The hub client is in main, on Node's sockets, and takes its socket factory, clock and random source from outside
- [ ] With a team code stored it starts its first lobby visit on launch and keeps trying for as long as the app is open; with no code it does nothing
- [ ] A lobby visit follows the hub spec's six steps, with the lobby request as its own write, repeated every 100 ms for about 500 ms
- [ ] Waits between visits are a random 1 to 2 s, every 5 s after a minute with no `PONG` from the lobby, and fast again on a `PONG`
- [ ] Attached, it sends a heartbeat once a second as its own write, and nothing else is ever written to the hub
- [ ] After 3 s without status it closes its socket and visits the lobby with a fresh token, without a `PING` of its old ID
- [ ] The connection state is one of the hub spec's eight states, with both major versions when it is version mismatch; the renderer can subscribe to it and read it once on start
- [ ] Board samples, ADC samples and GPS fixes reach the renderer stamped with their arrival time in main, and fill the same store the direct link fills
- [ ] In version mismatch the client stays attached, keeps its heartbeat and hands on no samples
- [ ] Unknown command IDs, unknown message types and trailing bytes are ignored
- [ ] The status line shows the Turkish text of 3.1 for each state, and nothing when live
- [ ] The waiting screen shows the logo, the status line, `Takım kodu: XXXX-XXXX`, and `host:port` only when it is not the public hub's; `Doğrudan bağlan` is not on it
- [ ] Once live the middle section is the trip meter; in board unreachable and phone lost the gauges and bottom section keep their last values dimmed
- [ ] The map shows the latest GPS fix as a marker, and `Konum verisi yok` before the first
- [ ] The `hub-client` suite drives the client with scripted bytes, a fake clock and a fixed random source, covers every case the spec lists except the two in ticket 31, and passes
- [ ] No automated test opens a socket to a hub
