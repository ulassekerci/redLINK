# What the phone screen shows

Type: prototype
Status: open

Map: [Hub migration](../map.md)

## Question

The app must work headless, but it also displays some information. What does the screen show when someone does look at it?

Prototype the screen(s): connection state for the board and the hub, viewer count, logging state, the handful of live values worth showing, and the controls for starting a run and exporting logs.

Also carry these from [What the phone log contains](08-what-the-phone-log-contains.md): the app lists past runs (start time, duration, size) with the total size of all logs; one run or several selected runs can be shared through the share sheet; runs are deleted only by a person, from this list. Live values shown on the screen come from `COMM_GET_VALUES_SETUP`, so speed is the board's own figure.
