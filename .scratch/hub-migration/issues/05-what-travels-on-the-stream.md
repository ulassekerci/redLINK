# What travels on the stream

Type: grilling
Status: claimed
Blocked by: 01, 03

Map: [Hub migration](../map.md)

## Question

The phone owns polling and desktops listen. What exactly does the phone poll and forward, and how does it avoid streaming into a slot nobody is attached to?

Decide: which commands the phone polls and at what rate (today: `COMM_GET_VALUES` and decoded ADC); the layout of the phone-GPS packet carried as `COMM_CUSTOM_APP_DATA`; the form of the lobby request (carrying the viewer's token) and of the roughly 1 Hz viewer heartbeat decided in [How several pit laptops watch at once](04-how-several-pit-laptops-watch-at-once.md); how the phone tells a viewer that the board is unreachable; how a desktop tells "connected and live" from "attached but silent".
