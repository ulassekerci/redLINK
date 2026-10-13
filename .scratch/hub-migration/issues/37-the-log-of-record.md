# 37: The log of record

**What to build:** every run leaves one CSV file in the phone's shared `Documents/redLINK/` folder, readable from the Files app and over USB. It has one row per poll cycle with the board's values and the latest GPS fix, is flushed every second so a kill loses about a second, and is never deleted by the app. The Log row says it is recording and how big the file is; if the file cannot be written the run goes on and the phone says so.

Spec: [Android app spec](../spec-android.md) 2.8, 3.1 (Log row), 3.4 (the "not logging" notification) and `LogWriterTest` in section 4.

**Blocked by:** 36 (Location during a run).

**Status:** resolved

- [x] A run writes `redLINK_<local start time>.csv` through MediaStore with no storage permission; a simulated run's file has the `SIM_` prefix
- [x] The first line is the column names of 2.8, in order; the layout is comma-separated with decimal points and no comment lines
- [x] There is one row per poll cycle in which at least one reply arrived; a missing reply leaves its cells empty
- [x] While the board is unreachable there is one row per GPS fix with the board cells empty
- [x] The latest fix is repeated on every row; a field the fix lacks is an empty cell; the GPS cells are empty until the first fix
- [x] `time_utc`, `elapsed_s` and `power_w` have the forms 2.8 gives; board values are written with the decimals the wire carries
- [x] The file is flushed once a second
- [x] The writer can append to an existing run's file without writing the column names again
- [x] When the file cannot be written the run and polling continue, the Log row reads `Not writing` in red, the notification of 3.4 is shown and removed when writing works again, and the app retries every second
- [x] The Log row reads `Recording, 1.25 MB` during a run and `Idle` outside one
- [x] Nothing is deleted automatically
- [x] `LogWriterTest` runs the writer on the simulated board with a fake clock and a fixed fix, compares the output byte for byte with an expected file, covers the three further cases the spec lists, and passes

## Comments

2026-10-13, on resolving: a debug build ran on the simulated board on the `redlink_api36` emulator. `SIM_redLINK_<time>.csv` appeared in `Documents/redLINK/` with no storage permission, grew every second on the underlying storage, and was complete after `Save & stop`, with one line of column names. The Log row read `Recording, 0.08 MB`. While the run went on, the shell's `/sdcard` view of the file showed a stale size; it caught up when the file was closed at Stop. The write-failure path (`Not writing`, the notification, the retry) was exercised only in `LogWriterTest`, not on a device.

`time_utc` is the wall clock at the moment the cycle ended (or the fix arrived), the same moment `elapsed_s` gives, not the moment the row reaches the file, which can be up to a second later.

The writer appends to a file that already holds rows, but `MediaStoreLogFile` only makes new files: reopening a run's file by its URI is left to [Automatic restart](43-automatic-restart.md).
