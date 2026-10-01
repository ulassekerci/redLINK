# How hub credentials are set and shared

Type: grilling
Status: open
Blocked by: 04, 13

Map: [Hub migration](../map.md)

## Question

The hub ID and password are the only access control and travel in clear text. How are they chosen, stored and handed to the pit crew?

Decide: who generates them and where they are entered; the length and alphabet of the viewer's token (carried as ASCII in the lobby request, see [What travels on the stream](05-what-travels-on-the-stream.md)); how the lobby ID and each viewer's token-bearing ID are derived from them (the hub upper-cases IDs, strips spaces and rejects `:`); how a laptop is configured (typed, file, QR); where the hub host and port setting lives so a self-run stock hub can be swapped in.
