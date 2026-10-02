# 39: Viewers join the bridge

**What to build:** during a run the phone can be found and joined on the hub. It generates its team code on first launch and shows it on Setup. When a run starts it registers the lobby; each viewer that asks there gets a registration of its own, and from that viewer's first heartbeat the bridge sends it a status message once a second. The main screen says whether the hub is connected and how many viewers are watching. A desktop app given the code goes live, or to board unreachable, with its gauges at zero.

The board's replies and GPS are not sent yet: that is ticket 40. This ticket covers a hub connection that stays up; re-registering after a drop, the host and port setting, and "New code" are ticket 41.

Spec: [Android app spec](../spec-android.md) 2.6, 2.9 (generation and display), 3.1 (Hub and Viewers rows), 3.3 (team code), 3.4 (notification text) and `BridgeLobbyTest` in section 4; [hub usage spec](../spec-hub.md) 2.1, 2.4 (lobby request, heartbeat, status), 2.5, 2.6 ("Status", "Active registration", "Sockets"), 2.7, 2.8 and 4.2.

**Blocked by:** 35 (A run on the simulated board).

**Status:** ready-for-agent

- [ ] The team code is generated on first launch with its check character, kept in the app's private settings, and shown on Setup as `XXXX-XXXX`
- [ ] The lobby ID, viewer IDs and password are derived from the code as the hub spec says
- [ ] When a run starts the bridge registers the lobby on the public hub and confirms it with `PING`, registering again on `NULL`; outside a run it holds no registration
- [ ] A lobby request with a well-formed token opens a new socket, registers the viewer's ID and confirms it with `PING`; a repeated request for the same token is a no-op; a malformed request is ignored
- [ ] The lobby is never written to, and the bridge never refuses a viewer
- [ ] A registration is active from its first heartbeat; nothing is written to it before that, writing stops after 3 s without one, and it is closed after 10 s
- [ ] Status goes to every active registration once a second, at once on a board state change and at once on a registration's first heartbeat; its version byte is the app's major version
- [ ] Every frame from a viewer other than a lobby request on the lobby or a heartbeat on its registration is discarded
- [ ] Every hub socket has TCP keep-alive on and is owned by the service; Stop closes them all
- [ ] Registrations stay while the board is unreachable
- [ ] The Hub row reads `Connected` in green with the lobby registered; the Viewers row reads `0 (hub down)`, `0 viewers`, `1 viewer` or `3 viewers`, counting registrations with a heartbeat in the last 3 s
- [ ] The notification's text is the Vehicle row's text in lower case, then the Viewers row's text
- [ ] The hub logic sits behind a socket seam and a clock seam; `BridgeLobbyTest` drives it with fakes, covers the hub spec's list for the bridge, and passes
- [ ] No automated test opens a socket to a hub
