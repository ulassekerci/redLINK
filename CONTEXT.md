# redLINK

Telemetry for a Shell Eco-marathon vehicle: data leaves the car's motor controller, travels through a phone in the car, and reaches the pit crew's laptops.

## Language

**Board**:
The VESC motor controller in the vehicle, the origin of all motor and battery telemetry. The phone screen labels it "Vehicle"; everywhere else it is the board.
_Avoid_: Device, vehicle, VESC (when the hub role is meant)

**Hub**:
The VESC TCP Hub, a third-party relay that joins one registration to one client and copies bytes between them.
_Avoid_: Server, relay, socket server

**Bridge**:
The phone app in its role of carrying bytes between the board and the hub.
_Avoid_: Source, uploader

**Registration**:
A bridge's presence on the hub under one ID and password, which one viewer at a time can attach to.
_Avoid_: Session, channel, room

**Lobby**:
The one registration every viewer attaches to first, where it asks the bridge for a registration of its own.
_Avoid_: Slot, seat, pool

**Team code**:
The one secret the phone generates and the pit crew types into each laptop, which lets a viewer find and attach to the bridge on the hub.
_Avoid_: Password, hub ID, credentials, key

**Viewer**:
A desktop app instance attached to the hub to watch the board's stream.
_Avoid_: Client, dashboard (for the instance), listener

**Stream**:
Everything the bridge writes to a viewer's registration: the board's replies plus the bridge's own messages.
_Avoid_: Feed, telemetry (for the bytes on the hub)

**Heartbeat**:
The message a viewer sends about once a second to tell the bridge it is still watching.
_Avoid_: Keep-alive, ping (the hub's `PING` is a different thing)

**Status**:
The message the bridge sends each viewer about once a second, saying the bridge is alive and whether the board is answering.
_Avoid_: Health, state packet

**Run**:
The span from someone pressing Start on the phone to someone pressing Stop or the phone restarting, during which the bridge works and the log of record is written.
_Avoid_: Session, trip (a trip is a distance measurement, not a span of bridging)

**Log of record**:
The telemetry file the phone writes during a run, authoritative over any copy a viewer saves.
_Avoid_: Export, CSV (as a name for the concept)
