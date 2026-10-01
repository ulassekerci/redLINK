# How hub credentials are set and shared

Type: grilling
Status: open
Blocked by: 04

Map: [Hub migration](../map.md)

## Question

The hub ID and password are the only access control and travel in clear text. How are they chosen, stored and handed to the pit crew?

Decide: who generates them and where they are entered; how the lobby ID and each viewer's token-bearing ID are derived from them (the hub upper-cases IDs, strips spaces and rejects `:`); how a laptop is configured (typed, file, QR); where the hub host and port setting lives so a self-run stock hub can be swapped in.
