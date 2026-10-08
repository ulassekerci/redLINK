# 35: A run on the simulated board

**What to build:** a run exists. In a debug build a person picks "Simulated board" on Setup, presses Start, and the app runs as it will in the car: one foreground service polls the board at 20 Hz with the screen off, the main screen shows the run's duration and the Vehicle row, the notification counts along, and Stop asks before ending it. The board here is the simulated one, which plays a fixed generated lap, so the whole thing runs on an emulator.

Everything above the board link talks to one seam; the Bluetooth implementation is ticket 42. Location, the log, the hub and automatic restart are later tickets.

Spec: [Android app spec](../spec-android.md) 2.1, 2.2, 2.5 ("Polling", "Faults", "The seam"), 2.12, 3.1 (Vehicle row, duration, button), 3.3 (board entry, debug switch), 3.4 (the run notification) and 3.5 (Stop); [hub usage spec](../spec-hub.md) 2.6 for the polling cycle and timeouts.

**Blocked by:** 34 (Main and Setup screens with grants).

**Status:** resolved

- [x] A run is one lifetime of a single foreground service typed `connectedDevice|location`, started from the visible activity when Start is pressed, holding a partial wake lock
- [x] Closing the activity or swiping the app out of recents changes nothing about the run
- [x] Outside a run the app holds no board link and no wake lock
- [x] The board link is one interface (send a request, receive bytes, connection state)
- [x] The simulated board is in the debug source set, answers the three board commands, and is absent from a release build
- [x] Its values are the fixed lap 2.12 describes, a function of time since the run started and nothing else
- [x] In debug builds the board picker on Setup offers `Simulated board`; with it picked the Bluetooth grant is not asked for and does not block Start
- [x] The poll cycle is setup values then decoded ADC every 50 ms, giving up on a command after 250 ms; after 2 s with no reply the board is unreachable and polling continues
- [x] The Vehicle row reads `Simulated`, `Connected · fault <NAME>` in red during the lap's fault, and `Reconnecting` when unreachable
- [x] The `Stop answering` switch on Setup works during a run and makes the board unreachable
- [x] The duration counts beside the wordmark as `12:44`, and `1:02:44` from one hour
- [x] The notification has the title, counting duration and `Stop` action of 3.4; tapping `Stop` opens the main screen with the dialog showing and does not stop the run by itself
- [x] The button reads `Stop` during a run; the dialog has the strings of 3.5, and `Save & stop` ends the run
- [x] The board cannot be changed during a run
- [x] A run on an emulator keeps polling with the screen off

## Comments

Implemented as `RunService`, the board link's seam and its poller in the package `org.metucet.redlink.board`, and the simulated board with its lap in the debug source set. Run on the Android 16 emulator image, where each line above was seen: the Vehicle row in its three states, the fault at 15 s into a lap, the notification's `Stop` opening the dialog with the activity open and with it closed, and `Save & stop` leaving no service, no wake lock and no notification.

- **Automated tests** cover the poller, through a board link the test answers, and the simulated board, through the seam: the cycle and its two timeouts, and that the lap's values agree with each other and repeat. The Vehicle row's text, the duration's format and the fault names are tested as plain functions. The service, the notification and the screens are not, as the Android spec's section 4 says.
- **The service's type.** Android refuses the `connectedDevice` type to an app without the Bluetooth grant, and a run on the simulated board must not need it. The service asks for `location`, and for `connectedDevice` as well when the grant is held. The spec's 2.2 says so now.
- **Screen off.** With the activity closed, the screen off and the emulator forced into deep idle, the simulated board went on receiving 40 requests a second for a minute. Swiping the app out of recents left the process, the service and the wake lock as they were.
- **Absent from release.** The release APK has no `SimulatedBoard` or lap class in it. Each build type has its own file saying what is simulated; the release one says nothing is. The picker's entry, the `Stop answering` switch and the `Not needed` text are in the shared code, behind that check, so their strings are in a release build though nothing there can show them.
- **The Bluetooth row** reads `Not needed` with the simulated board picked and the grant missing, and counts towards `Grants (n of 6)`. The spec had no string for it and did not say how the row counts; `5 of 6` beside an enabled Start was the other reading. The spec's 2.4 and 3.3 say this now.
- **Setup's board entry** is headed `Vehicle`, as in the prototype. The picker is a dialog titled `Pick vehicle` with a `Cancel` button, and the simulated board is shown as `Simulated board` with no address. The spec's 3.3 has these strings now.
- **The lap** is 120 seconds: 20 s of acceleration to 8 m/s, 60 s of cruising, 30 s of coasting to a stop, 10 s standing. The fault is `OVER_TEMP_FET`, over the last 5 seconds of the acceleration. Distance, charge and energy are worked out from formulas, not by stepping, so a reply depends on the time alone.
- **Polling** runs for the whole run whether or not the board answers. A reply that comes late still counts as the board answering. One that came before a request went out is not taken as that request's reply. A fault does not outlast the board's silence: when the board answers again there is none until a setup reply names one.
- **Duration** under one hour reads `0:07` and `12:44`. The notification's is Android's own counter, which reads `00:07`.

Known and left:

- **`Stop answering`** is not remembered: it is off whenever the app's process starts.
- **The notification's count** starts from the phone's wall clock, the only kind Android's counter takes, and the screen's from the monotonic clock. A clock change during a run makes the two differ.
- **A stopped service.** If Android kills the app during a run, nothing brings the run back. That is ticket 43.

Handed on, not decided here:

- **The picker in a release build** lists nothing, since scanning is ticket 42. A release build can not start a run until then.
- **The firmware version** request is answered by the simulated board but nothing sends it yet. Ticket 42 owns the request and where its result is shown. Its text says "after each connect", which the simulated board never does, so it has to say when the simulated board is asked; the start of the run is the likely answer.
- **The replies themselves** go no further than the poller, which only reads the fault code. The log and the bridge are the tickets that pass them on.
- **The Hub, Viewers and Log rows** keep their "no run" text during a run, and the GPS row is not there. Their tickets fill them in.
- **The notification's text** (`Vehicle connected · 3 viewers`) is ticket 39's. Until then the notification has no text.
