# redLINK

Telemetry for a Shell Eco-marathon vehicle: data leaves the car's motor controller, travels through a phone in the car, and reaches the pit crew's laptops.

## Language

**Board**:
The VESC motor controller in the vehicle, the origin of all motor and battery telemetry.
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

**Viewer**:
A desktop app instance attached to the hub to watch the board's stream.
_Avoid_: Client, dashboard (for the instance), listener

**Log of record**:
The telemetry file the phone writes during a run, authoritative over any copy a viewer saves.
_Avoid_: Export, CSV (as a name for the concept)
