# redLINK

Telemetry for a Shell Eco-marathon vehicle: data leaves the car's motor controller, travels through a phone in the car, and reaches the pit crew's laptops.

## Language

**Board**:
The VESC motor controller in the vehicle, the origin of all motor and battery telemetry. The phone screen labels it "Vehicle"; everywhere else it is the board.
_Avoid_: Vehicle, VESC (when the hub role is meant)

**Hub**:
The VESC TCP Hub, a third-party relay that joins one registration to one client and copies bytes between them.
_Avoid_: Server, relay, socket server

**Self-hosted hub**:
A stock hub the team starts on its own server and points the phone and laptops at when the public hub is unusable.
_Avoid_: Self-run hub, custom hub, fallback hub, backup server

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

**Direct link**:
The desktop app connected straight to the board over Bluetooth, with no bridge or hub, used while testing the vehicle. An app on a direct link is not a viewer.
_Avoid_: BLE mode, source, local

**Device**:
Anything a Bluetooth scan finds offering the board's serial service, such as the car's board, a spare VESC or another team's. The person picks one from the device list, and it is the board from then on.
_Avoid_: Peripheral, board (before it is picked)

**Simulated board**:
A stand-in for the board inside the phone app's debug build, which answers the bridge's polls with generated values. A run on it is a simulated run.
_Avoid_: Mock, fake board, test device, dummy

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

**Acceptance session**:
One real track session with the car, run on the phone app and desktop app, that decides whether the migration to the hub is done. A test on a desk is not one.
_Avoid_: Parity, field test
