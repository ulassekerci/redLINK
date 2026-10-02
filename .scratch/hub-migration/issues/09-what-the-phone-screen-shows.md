# What the phone screen shows

Type: prototype
Status: resolved

Map: [Hub migration](../map.md)

## Question

The app must work headless, but it also displays some information. What does the screen show when someone does look at it?

Prototype the screen(s): connection state for the board and the hub, viewer count, logging state, the handful of live values worth showing, and the controls for starting a run and exporting logs.

Also carry these from [What the phone log contains](08-what-the-phone-log-contains.md): the app lists past runs (start time, duration, size) with the total size of all logs; one run or several selected runs can be shared through the share sheet; runs are deleted only by a person, from this list. Live values shown on the screen come from `COMM_GET_VALUES_SETUP`, so speed is the board's own figure.

## Answer

The app has one main screen of status rows with a Start/Stop button, and two screens behind it, Logs and Setup. Decided on 2026-10-02 by reacting to a clickable prototype, [assets/09-phone-screen/prototype.html](../assets/09-phone-screen/prototype.html); variant D is the one chosen. Nothing here was run on a phone.

- **Main screen:** the redLINK wordmark with the run's duration beside it, then one row each for Vehicle, Hub, Viewers, GPS and Log, each with a coloured dot and a short state. The GPS row appears only during a run. Below are two plain rows, Logs and Setup, with no subtitles. Start/Stop is one wide button pinned to the bottom.
- **Wording:** the screen says "Vehicle" for the board; this is a label only, and the domain term stays Board. States read "Connected", "Not connected" and "Reconnecting" rather than "answering" or a poll rate. A board fault turns the Vehicle row red and names the fault. Past runs are called "Logs" on screen.
- **No live values:** no speed, voltage or other telemetry is shown. Nobody reads the phone while driving.
- **Start and Stop:** Start is disabled until all six grants are in place and a board is picked; a notice on the main screen says what is missing and opens Setup. Stop asks first: title "Stop redLINK?", text "Bridging and logging end. The log is saved.", buttons "Keep running" and "Save & stop".
- **Logs screen:** the number of logs and their total size, the log in progress (not shareable), then past logs with start time, duration and size. One or several can be selected, then shared through the share sheet or deleted after a confirmation.
- **Setup screen:** the six grants as rows with their state and a Grant button, the picked board with "Change", and the hub's host and port. The board and hub cannot be changed during a run. Manufacturer settings the app cannot read stay a written checklist outside the app.
- **Restart notice:** after a phone restart ended a run, the main screen says so once, dismissible.
- **Notification:** title "redLINK" with the run's duration, one line such as "Vehicle connected · 3 viewers", and a Stop action.
- **Look:** dark only, pure black background, redLINK red `#E11D48` (from the old app's splash image) for the wordmark and Start. Stop is a quiet dark button. Stock Material 3 components were acceptable; the Material 3 Expressive look was rejected.

Rejected: a large speed readout for the driver (variant B), the board-to-viewers chain diagram (variant C), and logs listed on the main screen (variant A).

Handed on: how the hub's credentials are entered on the Setup screen goes to [How hub credentials are set and shared](10-how-hub-credentials-are-set-and-shared.md).

## Comments

2026-10-02: prototype at [assets/09-phone-screen/prototype.html](../assets/09-phone-screen/prototype.html), three variants (A one page, B cockpit, C chain) with a panel that fakes the board, hub, viewers and grants. Variants D (merged) and E (D in Material 3 web components) were added in later rounds.

Amended 2026-10-02 by [Testing without the car](16-testing-without-the-car.md): on the simulated board the Vehicle row reads "Simulated"; in debug builds only, Setup offers the simulated board beside the real one and a "stop answering" control.
