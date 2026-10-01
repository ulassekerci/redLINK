# How desktop VESC Tool behaves as a hub client

Type: research
Status: resolved

Map: [Hub migration](../map.md)

## Question

When desktop VESC Tool attaches through the hub, what does it send, and how does it treat packets it did not ask for?

Read the vedderb/vesc_tool source and establish:

- Which commands desktop VESC Tool sends on connect and periodically while idle (firmware version probe, config reads, alive packets).
- How it handles unsolicited replies, e.g. `COMM_GET_VALUES` replies to requests it never made, arriving at 5-10 Hz.
- How it handles `COMM_CUSTOM_APP_DATA` (36) packets it receives: ignored, logged, or surfaced to the user?
- Which of its commands write to the board (config writes, motor control, firmware upload), so a bridge could tell read-only traffic from writes by command ID.
- Confirm the layout of `COMM_GET_DECODED_ADC` (32) and whether a newer command supersedes it.

## Answer

Desktop VESC Tool is an event-driven client: it probes the firmware version, reads configs once, scans CAN, then goes silent, and it acts on any packet that arrives whether it asked for it or not. Read from vedderb/vesc_tool at `dc53c658` and vedderb/bldc at `4fd8279e`. **Everything below is a reading of code; nothing was run against a hub or a board.**

- **On connect:**
  - It writes `VESCTOOL:<id>:<pass>\n` (`vescinterface.cpp:3116-3122`), then `COMM_FW_VERSION` (0) from about 80 ms after TCP connect.
  - It disconnects with an error dialog if no reply arrives within about 2 s (25 retries x 80 ms, `vescinterface.cpp:3245-3263`). Any FW version reply satisfies the probe, solicited or not (`commands.cpp:109-168`).
  - It always sends `COMM_PING_CAN` (62) once after the FW version is in (`mainwindow.cpp:810-814`, `2126-2132`). The board then pings CAN IDs 0-254 at 10 ms each (`bldc/comm/commands.c:2355-2370`, `bldc/comm/comm_can.c:648-670`).
  - After the ping reply it sends `COMM_FW_VERSION` again, directly and wrapped in `COMM_FORWARD_CAN` (34) per CAN ID found (`mainwindow.cpp:2142-2172`).
  - It requests `COMM_GET_MCCONF` (14) and `COMM_GET_APPCONF` (17) until each deserialises once per VESC Tool process, about one request per 1.2 s (`mainwindow.cpp:864-882`, `commands.cpp:1395-1405`). A bridge that drops these makes it retry for as long as it is connected.
  - Only if the board advertises custom configs or QML UIs, it also reads 92, 93, 117 and 118 (`vescinterface.cpp:3888-4160`).
- **Idle:** nothing periodic. Every periodic sender is a toolbar toggle that is off by default (`mainwindow.ui:793-803`, `943-953`):
  - RT data: 128, 4 and 47 at a 50 Hz setting (`mainwindow.cpp:486-493`).
  - App data: 32, 33 and 31 at 50 Hz (`mainwindow.cpp:495-502`).
  - IMU: 65 at 50 Hz; BMS: 96 at 10 Hz (`mainwindow.cpp:504-516`).
  - `COMM_ALIVE` (30): every 200 ms, off by default, but control buttons and keyboard control switch it on (`mainwindow.cpp:849-857`, `638-655`).
  - Each getter waits for its reply or 1 s before sending again (`commands.cpp:1256-1267`), so the real rate is bounded by round-trip time.
- **Unsolicited replies:** there is no request/reply matching; packets are dispatched by command ID alone (`commands.cpp:103-1168`).
  - Unrequested `COMM_GET_VALUES` updates the status-bar gauges (`mainwindow.cpp:1074-1079`) and the RT Data page plots (`pages/pagertdata.cpp:310-345`), and adds a row to the RT log if one is open (`vescinterface.cpp:415-470`).
  - Unrequested `COMM_GET_VALUES_SETUP` is stored as the latest setup values (`vescinterface.cpp:405-408`).
  - Each reply also clears that command's in-flight gate (`commands.cpp:191`).
  - Unknown IDs are dropped silently (`commands.cpp:1165-1166`).
  - So 5-10 Hz from the phone is harmless, and the desktop shows live data without streaming being enabled.
- **`COMM_CUSTOM_APP_DATA` (36):** ignored silently.
  - The handler only emits a signal (`commands.cpp:437-439`), and nothing in the desktop GUI listens to it.
  - The only listeners are the CLI `bridgeAppData` mode (`main.cpp:1109-1114`) and example QML scripts the user loads by hand (`res/qml/Examples/BalanceUi.qml:149`).
  - If the bridge forwards a viewer's ID 36 to the board, the firmware passes it to app-data handlers and LispBM (`bldc/comm/commands.c:771-778`), so the bridge should consume it.
- **Writes by command ID** (senders at `commands.cpp:1170-2346`, firmware handlers at `bldc/comm/commands.c:231-1720`):
  - Read-only: 0, 4, 14, 15, 17, 18, 31, 32, 33, 47, 50, 51, 65, 79, 90, 91, 92, 93, 94, 96, 111, 115, 117, 118, 122, 127, 128, 130, 134, 137, 140, 141, 150, 157. Also 62, which changes no state but generates CAN traffic.
  - Wrapper: 34 `COMM_FORWARD_CAN`; classify by the inner ID at payload byte 2 (`commands.cpp:2399-2402`).
  - Motor control and keep-alive: 5-12, 30, 35, 63, 84, 159.
  - Config and persistent state: 13, 16, 48, 49, 86, 87, 88, 89, 95, 110, 129, 149, 158.
  - Firmware, bootloader, reboot: 1, 2, 3, 29, 59, 60, 61, 73, 74, 81, 82, 102-109, 156.
  - Terminal, scripting, files: 20, 64, 120, 121, 131, 132, 133, 138, 139, 142, 143, 144, 152.
  - Detection and sampling (drives the motor): 19, 24-28, 57, 58.
  - Peripherals: 36, 37-46, 52-56, 66-72, 80, 83, 85, 97-101, 112, 116, 119, 123-126, 136.
  - Hazard: pressing Esc anywhere in desktop VESC Tool sends `COMM_SET_CURRENT` 0 plus `COMM_MOTOR_ESTOP` for 5000 ms (`mainwindow.cpp:601-605`, `1122-1131`). Forwarded, that cuts throttle input for 5 s.
  - Use an allow-list. The minimum for a clean attach with live data is 0, 4, 14, 17, 47, 50, 51, 62, 128, plus 34 wrapping those.
- **`COMM_GET_DECODED_ADC` (32):** confirmed, and nothing supersedes it.
  - Request is `[32]`. The reply is 17 bytes: the ID, then four big-endian `i32` values scaled by 1e6: ADC1 decoded level, ADC1 voltage, ADC2 decoded level, ADC2 voltage (`bldc/comm/commands.c:714-723`, `commands.cpp:411-418`).
  - The values only update while the ADC app thread runs (`bldc/applications/app_adc.c:163-285`).
  - No newer ADC command exists up to the last ID, 159 (`datatypes.h:852-1041`), and VESC Tool still polls 32 itself (`mainwindow.cpp:497`).
- **Not established:**
  - Whether the 2 s FW version deadline holds over a real BLE plus cellular path; it should be checked with a live attach.
  - Commands sent by pages beyond the connect path and idle state (wizards, firmware, Lisp, scripting) were not traced.
  - IDs 127, 140-144 and some BMS IDs are not handled in `bldc`; they are classified by name and by VESC Tool's sender.

Full findings: branch `research/how-desktop-vesc-tool-behaves-as-a-hub-client`, file `docs/research/how-desktop-vesc-tool-behaves-as-a-hub-client.md`.
