# 38: Logs screen

**What to build:** a person can get logs off the phone and tidy up. The Logs screen lists every past log, newest first, with its start time, duration and size. One or several can be selected and then shared as CSV files through the share sheet, or deleted after a confirmation. During a run the log in progress is shown but cannot be touched.

Spec: [Android app spec](../spec-android.md) 2.11 (Logs), 2.8 ("After a reinstall"), 3.2 and 3.5 (Delete logs).

**Blocked by:** 37 (The log of record).

**Status:** ready-for-agent

- [ ] The screen is titled `Logs` and shows the count and total size, `5 logs · 49.7 MB in Documents/redLINK`
- [ ] Past logs are listed newest first with `2026-09-26 14:22` and `41:00 · 10.2 MB`; a log's duration is the `elapsed_s` of its last row
- [ ] A simulated run's entry is marked `SIM`
- [ ] During a run the log in progress is listed as `Current log` with `12:44 · in progress, cannot be shared yet`, and cannot be selected, shared or deleted
- [ ] With no logs the screen reads `No logs yet`
- [ ] `Select all` / `Select none`, `Share n` and `Delete n` behave as 3.2 says and are disabled with nothing selected
- [ ] Share opens the share sheet with the selected files as CSV
- [ ] Delete asks first with the dialog of 3.5, in its singular and plural forms, and removes the files from the folder
- [ ] Files the app does not own (from before a reinstall) do not appear and are left alone
