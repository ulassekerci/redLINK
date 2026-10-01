# What travels on the stream

Type: grilling
Status: open
Blocked by: 01, 03

Map: [Hub migration](../map.md)

## Question

The phone owns polling and desktops listen. What exactly does the phone poll and forward, and how does it avoid streaming into a slot nobody is attached to?

Decide: which commands the phone polls and at what rate (today: `COMM_GET_VALUES` and decoded ADC); the layout of the phone-GPS packet carried as `COMM_CUSTOM_APP_DATA`; how the phone learns a viewer is present so the hub does not build a stale backlog; how a desktop tells "connected and live" from "attached but silent".
