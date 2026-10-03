# 27: Direct link on the new protocol

**What to build:** a person testing the vehicle presses "Doğrudan bağlan", sees a list of boards the app draws itself, picks one, and the gauges show live values polled from the board. This is the first real data path in the desktop app, and it moves the dashboard onto the new sample shape: the board's own speed and distance from the setup reply, with the wheel, gear and pole constants gone.

There is no hub client yet, so the parts of the spec about leaving and rejoining the hub belong to ticket 31. The waiting screen belongs to ticket 30; until then the action only needs to be reachable from the middle section and from settings.

Spec: [desktop app spec](../spec-desktop.md) 2.3 (samples, stores, derived values), 2.5, 2.9 (gauges, bottom section, fault warning, dimming), 3.1 (direct-link rows), 3.2 (device list, direct-link row), 3.3 and the `direct-link` suite in section 4.

**Blocked by:** 25 (Protocol vectors and the TypeScript protocol module), 26 (Electron shell around the dashboard).

**Status:** resolved

- [x] The action starts a scan filtered by the Nordic UART service only; the `redBLE` name filter is gone
- [x] Main passes the devices found so far to the renderer, which shows the device list with its Turkish strings; a device with no name is listed by its ID; nothing is picked automatically, even with one entry
- [x] Picking an entry connects to it; `Vazgeç` ends the scan
- [x] The polling loop runs the hub spec's cycle: setup values, then decoded ADC, every 50 ms, giving up on a command after 250 ms; it never sends the firmware version request
- [x] The loop talks to a transport object; Web Bluetooth is one implementation
- [x] Frames are decoded by the protocol module's buffering decoder; the old one-notification-one-frame parser, the old values parser, the `Mock` CRC bypass and the Bluetooth test file are removed
- [x] The vehicle store holds the latest board sample, ADC sample and GPS fix under the spec's field names; a field missing from a short reply is absent, not zero
- [x] The derived values (km/h, power, motor voltage, net energy) come from the new fields; the ERPM constants file is removed
- [x] Gauges and bottom section show what 2.9 says, with the battery percentage still from the Aspilsan curve
- [x] A non-zero fault code shows the warning with the firmware 6.06 fault name without its prefix, or the number when unknown
- [x] When the board stops answering for 2 s or the link drops, the gauges keep their last values dimmed, the status line reads `Araç yanıt vermiyor, yeniden deneniyor`, and the app reconnects to the same device every 2 s
- [x] `Araca bağlanılıyor` and `Bluetooth kullanılamıyor` (for 5 s) appear as 3.1 says
- [x] On a direct link the middle section shows `Doğrudan bağlantı` and `Bağlantıyı kes`, which closes the link
- [x] The map says `Konum verisi yok`
- [x] The macOS Bluetooth usage description is set
- [x] The `direct-link` suite drives the loop against a fake transport and a fake clock, covers the cases the spec lists, and passes

## Comments

The polling loop is `desktop/src/renderer/services/direct-link/polling.ts`, a `startPolling(transport, clock, events)` function; `web-bluetooth.ts` beside it is the Web Bluetooth transport and the scan. The `direct-link` suite is `direct-link.test.ts` there, using the setup and ADC replies from `protocol/vectors.json`. Samples are made by `boardSample` and `adcSample` in the new `src/protocol/samples.ts`, so main's hub client can use the same shape in ticket 30; they carry their arrival time as `received_at` (Unix ms). The vehicle store is `{ board, adc, gps }`, each the latest sample or null. The direct link's state is `store/directLink.ts` (`off`, `choosing`, `connecting`, `answering`, `not_answering`). Main's side of the device list is `src/main/bluetooth.ts`; the preload API gained `onBluetoothDevices`, `pickBluetoothDevice` and `cancelBluetoothScan`. Telling main a direct link started or ended is left to ticket 31, which needs it.

How the loop reads the spec:

- **A cycle** starts 50 ms after the previous one started, or at once when it ran over (a reply given up on at 250 ms).
- **Reconnecting.** After a drop or a failed connect, the next attempt is 2 s later. After 2 s of silence on a link that is still up, the link is closed and made again at once, since the 2 s have already passed; a board that stays silent therefore gets a fresh link every 2 s. The first version kept polling a silent link, as the bridge does; review pointed out that 2.5 says the app reconnects in both cases.
- **A connect that hangs** is not cut short: the next attempt waits for it to settle. Chromium's own GATT connect timeout ends it.
- **A first connect that fails** moves the status line from `Araca bağlanılıyor` to `Araç yanıt vermiyor, yeniden deneniyor`, as any later drop does. 3.1 doesn't list this case.
- **`Bluetooth kullanılamıyor`** shows when `navigator.bluetooth` is missing or the scan ends with an error the person didn't cause by pressing `Vazgeç`. Whether Electron rejects the scan with the radio off, or just lists nothing, is not known. If it lists nothing, the list stays on `Araç aranıyor` until `Vazgeç`. This goes on the desk checklist.

Also changed:

- **`backgroundThrottling: false`** on the window. The loop runs on renderer timers, which Chromium slows to once a second in a hidden or minimised window. That would starve the 50 ms cycle and trip the 2 s silence check.
- **The fault names** are firmware 6.06's `mc_fault_code` (checked against `datatypes.h` at `release_6_06`). That list ends with `ENCODER_FAULT` and `LV_OUTPUT_FAULT`, which the old enum lacked. They live in `utils/faults.ts`.
- **The usage description** is in a new `desktop/electron-builder.yml` under `mac.extendInfo`, for ticket 32 to build on. Nothing reads that file until electron-builder is added. In `pnpm dev`, Electron's own `Info.plist` already carries the key.
- **`utils/csv.ts` and the trip store** were moved onto the new fields only so they keep working. CSV rows now follow board samples and use the new field names; ticket 28 sets the real columns and order. `utils/crc.ts` went with the legacy Bluetooth code, as ticket 25 asked.

Found when running it: macOS kills the dev app on its first Bluetooth access when the app was started from a process that has no Bluetooth usage description. macOS checks the "responsible" process, here the CLI that ran `pnpm dev`, not Electron. The crash report says `namespace TCC`. Starting `pnpm dev` from a terminal app that has the key, or running the packaged app, avoids it.

Checked by hand on macOS in the running app, with the stores set through DevTools rather than a real board: the scan starts from `Doğrudan bağlan`; the device list shows `Araç aranıyor`, then the names, with an ID for a nameless device; the direct-link middle section shows `Araca bağlanılıyor`, then `Araç yanıt vermiyor, yeniden deneniyor`, the trip meter, `Doğrudan bağlantı` and `Bağlantıyı kes`; the gauges and bottom section dim; 10 m/s reads 36 km/h; fault 5 shows `OVER_TEMP_FET`; the map says `Konum verisi yok` on a direct link even with a fix in the store; `Bağlantıyı kes` returns to `Doğrudan bağlan`. Not checked: a real board, Bluetooth off, and Windows.
