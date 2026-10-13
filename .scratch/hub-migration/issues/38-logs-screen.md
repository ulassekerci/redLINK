# 38: Logs screen

**What to build:** a person can get logs off the phone and tidy up. The Logs screen lists every past log, newest first, with its start time, duration and size. One or several can be selected and then shared as CSV files through the share sheet, or deleted after a confirmation. During a run the log in progress is shown but cannot be touched.

Spec: [Android app spec](../spec-android.md) 2.11 (Logs), 2.8 ("After a reinstall"), 3.2 and 3.5 (Delete logs).

**Blocked by:** 37 (The log of record).

**Status:** resolved

- [x] The screen is titled `Logs` and shows the count and total size, `5 logs · 49.7 MB in Documents/redLINK`
- [x] Past logs are listed newest first with `2026-09-26 14:22` and `41:00 · 10.2 MB`; a log's duration is the `elapsed_s` of its last row
- [x] A simulated run's entry is marked `SIM`
- [x] During a run the log in progress is listed as `Current log` with `12:44 · in progress, cannot be shared yet`, and cannot be selected, shared or deleted
- [x] With no logs the screen reads `No logs yet`
- [x] `Select all` / `Select none`, `Share n` and `Delete n` behave as 3.2 says and are disabled with nothing selected
- [x] Share opens the share sheet with the selected files as CSV
- [x] Delete asks first with the dialog of 3.5, in its singular and plural forms, and removes the files from the folder
- [x] Files the app does not own (from before a reinstall) do not appear and are left alone

## Comments

2026-10-13, from [The log of record](37-the-log-of-record.md): MediaStore's size for the log in progress is stale until the run's file is closed, and so is the size other apps see over `/sdcard`. The size of the current log should come from the run (`LogState.Writing`), not from MediaStore.

2026-10-13, on resolving: a debug build ran on the `redlink_api36` emulator. The Logs screen listed the debug app's earlier log as `SIM 2026-10-13 21:43` with `0:43 · 0.2 MB`. A second file in the folder, whose MediaStore owner was empty from an earlier install, was not listed and was left in place: the "after a reinstall" rule, seen on a real file. During a simulated run the screen showed `Current log` with its duration counting and no checkbox, and counted it in the summary. After `Save & stop` the run's log became the newest past log with its duration. `Share 1` opened the share sheet with the file under its own name. `Delete 2 logs?` and `Delete 1 log?` showed; deleting one removed it from `Documents/redLINK/` and left the others. `No logs yet` was not seen on the device, since that would have meant deleting the earlier log.

The summary counts the log in progress, with the run's own size for it, so `No logs yet` shows only when there is no log at all. During a first run the screen shows `1 log · …` and the current log, with no empty-list line.

A log's duration and size are read from the file itself (its last 4 KB, and its size), not from MediaStore's size column, which lags behind a file being written. Selection is kept only while the screen is open. The list is read again when the service has written a run's last rows and closed its file (`RunService.logsClosed`), not when the run ends, so a run's log shows its full duration, even a run shorter than a second.
