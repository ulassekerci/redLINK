# 27: Direct link on the new protocol

**What to build:** a person testing the vehicle presses "Doğrudan bağlan", sees a list of boards the app draws itself, picks one, and the gauges show live values polled from the board. This is the first real data path in the desktop app, and it moves the dashboard onto the new sample shape: the board's own speed and distance from the setup reply, with the wheel, gear and pole constants gone.

There is no hub client yet, so the parts of the spec about leaving and rejoining the hub belong to ticket 31. The waiting screen belongs to ticket 30; until then the action only needs to be reachable from the middle section and from settings.

Spec: [desktop app spec](../spec-desktop.md) 2.3 (samples, stores, derived values), 2.5, 2.9 (gauges, bottom section, fault warning, dimming), 3.1 (direct-link rows), 3.2 (device list, direct-link row), 3.3 and the `direct-link` suite in section 4.

**Blocked by:** 25 (Protocol vectors and the TypeScript protocol module), 26 (Electron shell around the dashboard).

**Status:** ready-for-agent

- [ ] The action starts a scan filtered by the Nordic UART service only; the `redBLE` name filter is gone
- [ ] Main passes the devices found so far to the renderer, which shows the device list with its Turkish strings; a device with no name is listed by its ID; nothing is picked automatically, even with one entry
- [ ] Picking an entry connects to it; `Vazgeç` ends the scan
- [ ] The polling loop runs the hub spec's cycle: setup values, then decoded ADC, every 50 ms, giving up on a command after 250 ms; it never sends the firmware version request
- [ ] The loop talks to a transport object; Web Bluetooth is one implementation
- [ ] Frames are decoded by the protocol module's buffering decoder; the old one-notification-one-frame parser, the old values parser, the `Mock` CRC bypass and the Bluetooth test file are removed
- [ ] The vehicle store holds the latest board sample, ADC sample and GPS fix under the spec's field names; a field missing from a short reply is absent, not zero
- [ ] The derived values (km/h, power, motor voltage, net energy) come from the new fields; the ERPM constants file is removed
- [ ] Gauges and bottom section show what 2.9 says, with the battery percentage still from the Aspilsan curve
- [ ] A non-zero fault code shows the warning with the firmware 6.06 fault name without its prefix, or the number when unknown
- [ ] When the board stops answering for 2 s or the link drops, the gauges keep their last values dimmed, the status line reads `Araç yanıt vermiyor, yeniden deneniyor`, and the app reconnects to the same device every 2 s
- [ ] `Araca bağlanılıyor` and `Bluetooth kullanılamıyor` (for 5 s) appear as 3.1 says
- [ ] On a direct link the middle section shows `Doğrudan bağlantı` and `Bağlantıyı kes`, which closes the link
- [ ] The map says `Konum verisi yok`
- [ ] The macOS Bluetooth usage description is set
- [ ] The `direct-link` suite drives the loop against a fake transport and a fake clock, covers the cases the spec lists, and passes
