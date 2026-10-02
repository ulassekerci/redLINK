# 48: Desk checklist

**What to build:** nothing new. The front page's desk checklist is passed in full, on a real phone of the race phone's model running the debug build against the public hub. It is where the facts the part specs list as not established are found out without the car on a track. Passing it is what allows the acceptance session to be booked; it is not the acceptance session.

Before the long run, the manufacturer steps for the reference phone are worked out on the phone and written into `README.md`.

Spec: [front page](../spec.md) section 6; [Android app spec](../spec-android.md) 2.14 ("Manufacturer checklist", "Not established"); [desktop app spec](../spec-desktop.md) 2.12 ("Not established").

**Blocked by:** 28 (Trip meter and CSV export), 31 (Hub and direct link are exclusive), 38 (Logs screen), 40 (The stream to viewers), 41 (Hub reconnection and hub settings on the phone), 42 (Bluetooth board link), 43 (Automatic restart), 44 (Emulator to viewer, end to end), 45 (Update line in both apps), 47 (Release keystore and first release candidate).

**Status:** ready-for-human

- [ ] `README.md` carries the manufacturer steps for the reference phone, and they are applied
- [ ] 1. The long run: 1 hour, screen off, mobile data with Wi-Fi off, 8 viewer windows on one laptop live throughout, and a log with no gaps
- [ ] 2. The short run of about 10 minutes, carrying items 3 to 5
- [ ] 3. The other system: 8 viewer windows on one laptop of the system the long run did not use stay live
- [ ] 4. Each status-line state is produced once by hand, version mismatch excepted
- [ ] 5. Kill and resume: the process is killed with `adb`, the run resumes into the same file, and every viewer returns to live by itself
- [ ] 6. The board on a stand: the phone connects and shows the firmware version; a macOS and a Windows laptop each start a direct link, show sane values at a steady rate, and reconnect after the board is switched off and on
- [ ] 7. The self-hosted hub, once: started on a rented server, phone and two laptops moved to it and back, then the server destroyed
- [ ] 8. The board's wheel diameter, gear ratio and pole count match the mechanics team's measured figures
- [ ] Each fact listed as not established in the Android and desktop specs has an answer written into the spec
- [ ] Every defect found is fixed or filed as a ticket in this directory, and the affected item is passed again
