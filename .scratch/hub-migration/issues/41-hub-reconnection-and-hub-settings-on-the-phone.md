# 41: Hub reconnection and hub settings on the phone

**What to build:** the bridge survives the phone's network coming and going, and the phone can be pointed at another hub. When the hub connection drops the bridge registers the lobby again by itself, with a growing wait and at once when the network returns, and the Hub row counts down to the next attempt. Setup lets a person edit the hub's host and port and make a new team code, both locked during a run.

Spec: [Android app spec](../spec-android.md) 2.6 (network change, "never touches the log or the board link"), 2.9 ("New code"), 2.10, 3.1 (Hub row), 3.3 (Hub, team code) and 3.5 (New code); [hub usage spec](../spec-hub.md) 2.8 ("When the hub connection drops", "Stale registrations") and 2.10.

**Blocked by:** 39 (Viewers join the bridge).

**Status:** resolved

- [x] When the hub connection drops the bridge re-registers the lobby only, confirming with `PING`: first attempt at once, then waits of 250 ms doubling to a 10 s cap, each randomised by 20% either way
- [x] A network callback for a newly available default network triggers an attempt at once
- [x] Lost viewer registrations are not re-opened; the bridge closes its own old sockets itself
- [x] The Hub row reads `Reconnecting in 4 s` in red with the wait left, and `No network` in red while Android reports no network at all
- [x] A hub connection that drops never touches the log or the board link
- [x] Setup shows the hub as `host:port` with `Edit`, pre-filled with the public hub's
- [x] When host or port differ from the public hub's, the Hub row shows `host:port` as a second line in every condition; otherwise the main screen does not show them
- [x] `New code` asks first with the dialog of 3.5 and then replaces the team code
- [x] `Edit` and `New code` are disabled during a run
- [x] The team code is unaffected by a change of host
- [x] `BridgeLobbyTest` gains the case that after the hub connection drops only the lobby is registered again, and passes

## Comments

Implemented in the `Bridge`, which now holds the lobby in a loop, and in Setup and the main screen. Run on the Android 16 emulator image: with the port set to 65102, where nothing answers, a simulated run showed `veschub.vedder.se:65102` under the Hub row and counted down waits growing to about 10 s; airplane mode showed `No network`, and turning it off started the waits again from the shortest. Back on the public hub, the run went `Connected`; through 5 s of airplane mode the Log row kept recording and the Vehicle row stayed `Simulated`, and afterwards a `PING` of the lobby from another machine answered `PONG`. The lobby is registered again on the new network whether or not the old socket noticed anything; the emulator's shell cannot list the app's sockets, so that the old one was closed is shown by the tests only. `Change vehicle`, `Edit` and `New code` were disabled during the run, and `New code` asked first and replaced the code only on `New code`.

- **Automated tests:** `BridgeLobbyTest` gains cases for only the lobby being registered again after a drop, with the viewer's socket closed and its token free to ask again; a new network while the lobby is held registering it again at once; a new network during registration changing nothing; the waits of 250 ms doubling to 10 s; the 20% spread; a new network trying at once and starting the waits again; a drop starting the waits again; and the state naming the time of the next attempt. `MainScreenTest` gains the countdown, `No network` and the second line. `HubAddressTest` is new, for a typed host and port.
- **A drop closes every viewer registration** opened through the lost lobby, and the bridge closes their sockets itself. The hub spec says lost registrations are not re-opened and old sockets are the bridge's to close; closing those that may still work too keeps one rule, and their viewers notice the silence and come back through the lobby within a few seconds.
- **A drop** is the lobby's connection ending, or a new default network while the lobby is held. The lobby is never written to, so a connection left dead on the old network would otherwise go unnoticed for hours; registering the same ID again replaces it on the hub. A network that arrives while the lobby is being registered is the one it is registered on, and the network that is the default when the run starts is not a new one.
- **Waits:** the cap applies before the spread, so a wait at the cap is 8 to 12 s. A new network resets the doubling, as a drop does. The Hub row rounds the wait left up to whole seconds and never reads less than `1 s`.
- **`No network`** comes from a `DefaultNetwork` watch the service owns for the run: false from a lost default network with no successor until the next one arrives.
- **The hub edit dialog** is titled `Hub`, with fields `Host` and `Port` and buttons `Cancel` and `Save`; the spec gives no strings for it. An empty host, or a port that is not one, is the public hub's, as on the desktop app. The host and port are kept in the app's private settings beside the team code, which a change of host leaves alone.
- **The emulator's team code** is now `M2FU-N3PV`, made by the `New code` check above.
