# 43: Automatic restart

**What to build:** a run survives the app being killed. If Android kills the service or the app crashes, the run resumes by itself into the same log file and the same lobby, with the gap showing only as missing rows, and every viewer comes back without anyone touching a laptop. A phone restart does end the run, and the app says so the next time it is opened. Three restarts in a minute end the run with a notification.

How Android brings the service back is left to implementation; the behaviour below is the requirement. The check is the desk checklist's kill-and-resume item, done from a computer with `adb`. "Force stop" in Android's settings does not count.

Spec: [Android app spec](../spec-android.md) 2.3, 2.14 ("Not established"), 3.1 (the restart notice) and 3.4 (restart limit notification).

**Blocked by:** 37 (The log of record), 39 (Viewers join the bridge).

**Status:** ready-for-agent

- [ ] On Start the app stores that a run is active with its start time, its log file and the phone's boot count; Stop clears it
- [ ] After a kill or a crash the service comes back and resumes the same run: same log file with no second line of column names, same lobby ID, `elapsed_s` still counted from the original Start
- [ ] If the app is opened and finds a run stored as active on the same boot with no service running, it resumes the run the same way
- [ ] After 3 automatic restarts within one minute the app stops resuming, ends the run, clears the stored state and posts `redLINK stopped` with the text of 3.4
- [ ] The app does not start at boot
- [ ] Opened after a phone restart, the app clears the stored run and shows `The last run ended when the phone restarted.` with `Dismiss`, once; Start then begins a new run with a new file
- [ ] Killing the app's process with `adb` during a simulated run on an emulator shows the run resuming into the same file
