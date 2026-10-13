# 39: Viewers join the bridge

**What to build:** during a run the phone can be found and joined on the hub. It generates its team code on first launch and shows it on Setup. When a run starts it registers the lobby; each viewer that asks there gets a registration of its own, and from that viewer's first heartbeat the bridge sends it a status message once a second. The main screen says whether the hub is connected and how many viewers are watching. A desktop app given the code goes live, or to board unreachable, with its gauges at zero.

The board's replies and GPS are not sent yet: that is ticket 40. This ticket covers a hub connection that stays up; re-registering after a drop, the host and port setting, and "New code" are ticket 41.

Spec: [Android app spec](../spec-android.md) 2.6, 2.9 (generation and display), 3.1 (Hub and Viewers rows), 3.3 (team code), 3.4 (notification text) and `BridgeLobbyTest` in section 4; [hub usage spec](../spec-hub.md) 2.1, 2.4 (lobby request, heartbeat, status), 2.5, 2.6 ("Status", "Active registration", "Sockets"), 2.7, 2.8 and 4.2.

**Blocked by:** 35 (A run on the simulated board).

**Status:** resolved

- [x] The team code is generated on first launch with its check character, kept in the app's private settings, and shown on Setup as `XXXX-XXXX`
- [x] The lobby ID, viewer IDs and password are derived from the code as the hub spec says
- [x] When a run starts the bridge registers the lobby on the public hub and confirms it with `PING`, registering again on `NULL`; outside a run it holds no registration
- [x] A lobby request with a well-formed token opens a new socket, registers the viewer's ID and confirms it with `PING`; a repeated request for the same token is a no-op; a malformed request is ignored
- [x] The lobby is never written to, and the bridge never refuses a viewer
- [x] A registration is active from its first heartbeat; nothing is written to it before that, writing stops after 3 s without one, and it is closed after 10 s
- [x] Status goes to every active registration once a second, at once on a board state change and at once on a registration's first heartbeat; its version byte is the app's major version
- [x] Every frame from a viewer other than a lobby request on the lobby or a heartbeat on its registration is discarded
- [x] Every hub socket has TCP keep-alive on and is owned by the service; Stop closes them all
- [x] Registrations stay while the board is unreachable
- [x] The Hub row reads `Connected` in green with the lobby registered; the Viewers row reads `0 (hub down)`, `0 viewers`, `1 viewer` or `3 viewers`, counting registrations with a heartbeat in the last 3 s
- [x] The notification's text is the Vehicle row's text in lower case, then the Viewers row's text
- [x] The hub logic sits behind a socket seam and a clock seam; `BridgeLobbyTest` drives it with fakes, covers the hub spec's list for the bridge, and passes
- [x] No automated test opens a socket to a hub

## Comments

Implemented as the `Bridge` in the package `org.metucet.redlink.hub`, behind the socket seam `HubSockets` with `TcpHubSockets` as its one implementation, and a clock passed in as a function. `RunService` runs the bridge for the run's lifetime on a dispatcher that runs one coroutine at a time. Run on the Android 16 emulator image against the public hub, with the desktop app's own hub client as the viewer: it went live in about 1.3 s and the phone showed `1 viewer`; `Stop answering` moved it to board unreachable and back; `Stop` moved it to phone lost, then phone not found. The notification read `Vehicle simulated · 1 viewer`, and `Vehicle reconnecting · 1 viewer` while the board was silent. Setup showed the code as `NVV6-3PQ5`.

- **Automated tests:** `BridgeLobbyTest` drives the bridge through a fake hub that registers, replaces, loses logins and answers `PING` as the hub spec says, on virtual time. The Hub and Viewers rows, the notification's text and the major version are tested as plain functions.
- **The Hub row while registering** reads `Connecting` in amber. The spec had no string for it; its 3.1 has one now.
- **The notification** lower-cases only the first letter of the Vehicle row's text, so `Vehicle connected · fault OVER_TEMP_FET` keeps the fault's name as the Vehicle row shows it. The spec's 3.4 says so now.
- **Confirming a registration:** the `PING` follows the login line by 100 ms, as the viewer's first heartbeat does. After a `NULL` the bridge waits 250 ms before logging in again, so it does not log in back to back while the hub discards what it is sent. A registration's 10 s without a heartbeat are counted from the opening of the connection the hub confirmed.
- **Status at once** goes to a registration on its first heartbeat and also whenever it becomes active again after 3 s without one. Frames already waiting for a registration are not written once it stops being active.
- **When `PING` cannot reach the hub** the bridge gives up on that registration: a viewer's is dropped, and the lobby is lost.
- **The host name** is looked up so that Stop does not wait for it. A host that cannot be found is the same as one that cannot be reached.

Known and left:

- **A lost lobby** is not registered again, and the Hub row reads `Not connected` in red until the run ends. Ticket 41 re-registers it and gives the row its `Reconnecting in 4 s` and `No network` texts.
- **Status only:** the board's replies and GPS are not on the stream yet, so a live viewer's gauges read zero. That is ticket 40.
- **The team code as a type:** it travels as a plain string without its dash, as `protocol/TeamCode.kt` already had it.
