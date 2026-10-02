# 37: The log of record

**What to build:** every run leaves one CSV file in the phone's shared `Documents/redLINK/` folder, readable from the Files app and over USB. It has one row per poll cycle with the board's values and the latest GPS fix, is flushed every second so a kill loses about a second, and is never deleted by the app. The Log row says it is recording and how big the file is; if the file cannot be written the run goes on and the phone says so.

Spec: [Android app spec](../spec-android.md) 2.8, 3.1 (Log row), 3.4 (the "not logging" notification) and `LogWriterTest` in section 4.

**Blocked by:** 36 (Location during a run).

**Status:** ready-for-agent

- [ ] A run writes `redLINK_<local start time>.csv` through MediaStore with no storage permission; a simulated run's file has the `SIM_` prefix
- [ ] The first line is the column names of 2.8, in order; the layout is comma-separated with decimal points and no comment lines
- [ ] There is one row per poll cycle in which at least one reply arrived; a missing reply leaves its cells empty
- [ ] While the board is unreachable there is one row per GPS fix with the board cells empty
- [ ] The latest fix is repeated on every row; a field the fix lacks is an empty cell; the GPS cells are empty until the first fix
- [ ] `time_utc`, `elapsed_s` and `power_w` have the forms 2.8 gives; board values are written with the decimals the wire carries
- [ ] The file is flushed once a second
- [ ] The writer can append to an existing run's file without writing the column names again
- [ ] When the file cannot be written the run and polling continue, the Log row reads `Not writing` in red, the notification of 3.4 is shown and removed when writing works again, and the app retries every second
- [ ] The Log row reads `Recording, 1.25 MB` during a run and `Idle` outside one
- [ ] Nothing is deleted automatically
- [ ] `LogWriterTest` runs the writer on the simulated board with a fake clock and a fixed fix, compares the output byte for byte with an expected file, covers the three further cases the spec lists, and passes
