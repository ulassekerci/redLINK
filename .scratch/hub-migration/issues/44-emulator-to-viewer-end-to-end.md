# 44: Emulator to viewer, end to end

**What to build:** nothing new. This is the first time the two apps meet: a person runs the Android debug build on a stock Android Studio emulator image, starts a run on the simulated board against the public hub, and watches a viewer on the same laptop go live. It meets the Android spec's emulator requirement and is how a viewer is tested with no phone from here on. Anything it turns up is fixed here or filed as a new ticket.

Spec: [Android app spec](../spec-android.md) 2.12 ("Emulator requirement"); [front page](../spec.md) section 6, which says this requirement is met before the desk checklist.

**Blocked by:** 30 (Viewer joins the hub and goes live), 40 (The stream to viewers).

**Status:** ready-for-human

- [ ] The debug build installs on a stock Android Studio emulator image and all grants except Bluetooth are given
- [ ] A run on the simulated board starts, and the Hub row reads `Connected`
- [ ] The desktop app, given the debug build's team code, goes from phone not found to joining to live with no click
- [ ] Gauges, ADC and the bottom section show the simulated lap; the fault warning appears during the lap's fault
- [ ] With a route playing on the emulator, the map shows the marker moving
- [ ] The phone's Viewers row reads `1 viewer`, and counts a second window when one is opened
- [ ] `Stop answering` makes the viewer show board unreachable, dimmed, and switching it off brings it back to live
- [ ] Stop on the phone sends the viewer to phone not found; Start brings it back to live by itself
- [ ] The emulator's log file has rows for the whole run with GPS cells filled
- [ ] Every defect found is fixed or filed as a ticket in this directory
