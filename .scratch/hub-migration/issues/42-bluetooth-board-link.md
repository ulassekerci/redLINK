# 42: Bluetooth board link

**What to build:** the phone talks to the real board. A person picks the board once on Setup from a scan, and from then on Start connects to it without scanning. The bridge reads the board's firmware version on each connect and shows it on Setup, polls as it does with the simulated board, and keeps retrying until Stop when the link drops.

This is the second implementation of the board link's seam from ticket 35. It cannot be exercised by automated tests or an emulator; the desk checklist's "board on a stand" item is where it is proven, so build it carefully against the spec.

Spec: [Android app spec](../spec-android.md) 2.5, 2.14 (stack: Nordic Android-BLE-Library 2.x), 3.1 (Vehicle row) and 3.3 (board entry); [hub usage spec](../spec-hub.md) 2.3 for the firmware version command.

**Blocked by:** 35 (A run on the simulated board).

**Status:** ready-for-agent

- [ ] `Pick vehicle` on Setup scans for devices offering the Nordic UART service, with no name filter, and stores the picked board by address
- [ ] Setup shows the board's Bluetooth name with its address under it and `Change vehicle`
- [ ] Start connects to the stored address without scanning, requests MTU 517, writes to the RX characteristic and listens for notifications on the TX characteristic
- [ ] Bytes go through the protocol module's buffering decoder, so a frame spanning several notifications is decoded once
- [ ] After each connect, before polling, the firmware version is requested once with a wait of up to 250 ms; the major and minor are shown on Setup as `Firmware 6.06`; no reply leaves it unknown and polling starts anyway
- [ ] The firmware version reply is not logged and not copied to any registration; no version is refused or warned about
- [ ] A run can start with the board off or out of range
- [ ] When the link drops the bridge retries until Stop: a direct connect at once, and after a few failures Android's `autoConnect`; Bluetooth switched off on the phone is treated the same
- [ ] While the link is down the log stays open and GPS keeps being logged and streamed
- [ ] The Vehicle row reads `Connected` in green, `Connected · fault <NAME>` in red on a non-zero fault code, and `Reconnecting` in red when unreachable
- [ ] Fault names are the firmware 6.06 names without the `FAULT_CODE_` prefix; an unknown code is shown as its number
- [ ] `Change vehicle` is disabled during a run
- [ ] With a real board picked the Bluetooth grant blocks Start as the other five do
