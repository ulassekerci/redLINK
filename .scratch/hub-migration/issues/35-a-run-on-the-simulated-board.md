# 35: A run on the simulated board

**What to build:** a run exists. In a debug build a person picks "Simulated board" on Setup, presses Start, and the app runs as it will in the car: one foreground service polls the board at 20 Hz with the screen off, the main screen shows the run's duration and the Vehicle row, the notification counts along, and Stop asks before ending it. The board here is the simulated one, which plays a fixed generated lap, so the whole thing runs on an emulator.

Everything above the board link talks to one seam; the Bluetooth implementation is ticket 42. Location, the log, the hub and automatic restart are later tickets.

Spec: [Android app spec](../spec-android.md) 2.1, 2.2, 2.5 ("Polling", "Faults", "The seam"), 2.12, 3.1 (Vehicle row, duration, button), 3.3 (board entry, debug switch), 3.4 (the run notification) and 3.5 (Stop); [hub usage spec](../spec-hub.md) 2.6 for the polling cycle and timeouts.

**Blocked by:** 34 (Main and Setup screens with grants).

**Status:** ready-for-agent

- [ ] A run is one lifetime of a single foreground service typed `connectedDevice|location`, started from the visible activity when Start is pressed, holding a partial wake lock
- [ ] Closing the activity or swiping the app out of recents changes nothing about the run
- [ ] Outside a run the app holds no board link and no wake lock
- [ ] The board link is one interface (send a request, receive bytes, connection state)
- [ ] The simulated board is in the debug source set, answers the three board commands, and is absent from a release build
- [ ] Its values are the fixed lap 2.12 describes, a function of time since the run started and nothing else
- [ ] In debug builds the board picker on Setup offers `Simulated board`; with it picked the Bluetooth grant is not asked for and does not block Start
- [ ] The poll cycle is setup values then decoded ADC every 50 ms, giving up on a command after 250 ms; after 2 s with no reply the board is unreachable and polling continues
- [ ] The Vehicle row reads `Simulated`, `Connected · fault <NAME>` in red during the lap's fault, and `Reconnecting` when unreachable
- [ ] The `Stop answering` switch on Setup works during a run and makes the board unreachable
- [ ] The duration counts beside the wordmark as `12:44`, and `1:02:44` from one hour
- [ ] The notification has the title, counting duration and `Stop` action of 3.4; tapping `Stop` opens the main screen with the dialog showing and does not stop the run by itself
- [ ] The button reads `Stop` during a run; the dialog has the strings of 3.5, and `Save & stop` ends the run
- [ ] The board cannot be changed during a run
- [ ] A run on an emulator keeps polling with the screen off
