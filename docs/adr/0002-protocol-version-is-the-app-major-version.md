# The protocol version is the app's major version, and a mismatch refuses both ways

The phone app and the desktop app are released together under one version number, but they are updated by hand on different machines, so a phone and a laptop can meet on the hub running different builds. We decided that the protocol version byte in the status message is the app's major version, and that a viewer shows nothing from the stream when its own major differs from the bridge's, whichever side is newer. Within one major, the messages change only by addition: a viewer ignores message types it does not know and extra bytes at the end of a message it does.

## Considered options

- **A protocol counter of its own, with the viewer refusing only a newer protocol.** This was the first design. Rejected: it leaves open what a newer viewer does with an older bridge, and answering that leads to the next option.
- **A newer viewer reads every older protocol.** Rejected: the desktop app would carry and test a parser for each past protocol for good. With a handful of laptops that install an older release in minutes, that code repays nothing.

## Consequences

- An incompatible change to the messages is a new major release of both apps, even when nothing else changed.
- Additive changes ship in a minor release. The rule that unknown types and trailing bytes are ignored must hold from the first release, or it cannot be relied on later.
- A laptop that is a major ahead of the phone cannot watch. The way out is to install the older desktop release, which stays on the Releases page, or to update the phone. The viewer states the two versions and recommends neither.
- The direct link is unaffected: it talks to the board, not the bridge, and carries no protocol version.
