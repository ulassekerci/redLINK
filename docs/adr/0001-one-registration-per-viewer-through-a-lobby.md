# One registration per viewer, handed out through a lobby

The hub joins one registration to one client and drops the previous client when a new one logs in, but the whole pit crew must watch at once from laptops that share no network. We decided that the bridge holds one registration per viewer: a viewer sends a random token to the lobby, the bridge opens a registration whose ID includes that token, and the viewer attaches there once the hub's `PING` confirms it exists. This keeps the stock public hub and needs no per-laptop setup beyond the shared credentials.

## Considered options

- **One laptop attaches and re-broadcasts.** Rejected: with no shared network the other laptops need an internet-reachable relay, which is the custom server this migration removes, and that one laptop becomes a single point of failure.
- **A hub patched to fan out.** Rejected: it is custom server code and rules out the public hub.
- **A fixed pool of registrations with a seat number per laptop.** Rejected: every laptop would need configuring, and two laptops on the same seat kick each other indefinitely.
- **A fixed pool handed out by the lobby.** Rejected: a laptop waking from sleep reattaches to its old registration and kicks whoever was given it since. With an ID per viewer, a stale laptop finds its registration gone and returns to the lobby.

## Consequences

- VESC Tool has no precedent for this; it is our own protocol on top of the hub, and desktop VESC Tool cannot attach through it.
- Viewers must send a heartbeat (about once a second). The hub does not tell the bridge when a viewer attaches or leaves, and a registration with no viewer attached buffers on the hub without bound, so the bridge closes a registration after about 10 s of silence.
- The bridge's uplink grows with the number of viewers, since it writes the stream once per registration. The bridge never refuses a viewer; 8 is the tested number.
- The lobby is the one place viewers can displace each other. A viewer stays there only long enough to send its token, and a lost request is retried silently. A viewer that is watching cannot be kicked, because no other laptop knows its ID.
