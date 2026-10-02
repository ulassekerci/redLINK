# 41: Hub reconnection and hub settings on the phone

**What to build:** the bridge survives the phone's network coming and going, and the phone can be pointed at another hub. When the hub connection drops the bridge registers the lobby again by itself, with a growing wait and at once when the network returns, and the Hub row counts down to the next attempt. Setup lets a person edit the hub's host and port and make a new team code, both locked during a run.

Spec: [Android app spec](../spec-android.md) 2.6 (network change, "never touches the log or the board link"), 2.9 ("New code"), 2.10, 3.1 (Hub row), 3.3 (Hub, team code) and 3.5 (New code); [hub usage spec](../spec-hub.md) 2.8 ("When the hub connection drops", "Stale registrations") and 2.10.

**Blocked by:** 39 (Viewers join the bridge).

**Status:** ready-for-agent

- [ ] When the hub connection drops the bridge re-registers the lobby only, confirming with `PING`: first attempt at once, then waits of 250 ms doubling to a 10 s cap, each randomised by 20% either way
- [ ] A network callback for a newly available default network triggers an attempt at once
- [ ] Lost viewer registrations are not re-opened; the bridge closes its own old sockets itself
- [ ] The Hub row reads `Reconnecting in 4 s` in red with the wait left, and `No network` in red while Android reports no network at all
- [ ] A hub connection that drops never touches the log or the board link
- [ ] Setup shows the hub as `host:port` with `Edit`, pre-filled with the public hub's
- [ ] When host or port differ from the public hub's, the Hub row shows `host:port` as a second line in every condition; otherwise the main screen does not show them
- [ ] `New code` asks first with the dialog of 3.5 and then replaces the team code
- [ ] `Edit` and `New code` are disabled during a run
- [ ] The team code is unaffected by a change of host
- [ ] `BridgeLobbyTest` gains the case that after the hub connection drops only the lobby is registered again, and passes
