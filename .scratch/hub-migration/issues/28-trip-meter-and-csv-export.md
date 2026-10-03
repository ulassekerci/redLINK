# 28: Trip meter and CSV export

**What to build:** each laptop keeps its own trip in memory and can save what it saw. Space starts a new trip; the rows show distance, time, average speed and consumption since then, computed from the board's own absolute distance and energy figures. Cmd+S or Ctrl+S saves a CSV of the current trip with the log of record's columns.

Spec: [desktop app spec](../spec-desktop.md) 2.10, 3.2 (trip meter rows) and the `trip-and-csv` suite in section 4; [Android app spec](../spec-android.md) 2.8 for the column list.

**Blocked by:** 27 (Direct link on the new protocol).

**Status:** resolved

- [x] A trip is a baseline of absolute distance, energy used and energy charged plus a start time; the rows are the current values minus the baseline
- [x] Space sets the baseline to the current values and the start time to now, and does nothing while a text field has the focus
- [x] Before the first Space the baseline is zero and the time row reads `00:00:00`
- [x] When a value falls below its baseline (the board was power-cycled) that baseline moves to the new value, and no row goes negative
- [x] The trip is kept across phone lost, rejoining and a switch between the hub and a direct link, and is lost when the app quits
- [x] One CSV row is held in memory per board sample; a new trip clears them
- [x] The columns are the log of record's names, units and order, with the differences 2.10 lists: arrival time as `time_utc`, no `elapsed_s`, latest ADC and GPS values repeated, `power_w` computed on the laptop
- [x] GPS cells are empty when there is no fix
- [x] Cmd+S on macOS and Ctrl+S on Windows open the system's save dialog through main with the default name `redLINK_<local time>.csv`; with no rows nothing happens; saving does not clear the rows
- [x] The `trip-and-csv` suite covers the cases the spec lists and passes
