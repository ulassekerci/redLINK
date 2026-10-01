# How desktop VESC Tool behaves as a hub client

Research for the Hub migration map, ticket 03.

## Sources and method

- `vedderb/vesc_tool` at `dc53c658cbb89a947246034f7a00149cf79abdfc` (master, 2026-09-04). Paths below with no prefix are in this repo.
- `vedderb/bldc` at `4fd8279ea45a17c0d69357438ae2f7237a32514f` (master, 2026-09-17). Paths prefixed `bldc/`.
- **Everything here is a reading of source code.** Nothing was run: desktop VESC Tool was not attached to a hub, and no board was probed. Rates are computed from timer intervals in the code, not measured on the wire.
- "Desktop VESC Tool" means the Qt Widgets build (`mainwindow.cpp`). The mobile QML build shares `vescinterface.cpp` and `commands.cpp` but not `mainwindow.cpp`.

## 1. What it sends on connect and while idle

### Sequence after the TCP socket connects

| Step | Command (ID) | When / rate | Source |
|---|---|---|---|
| 0 | ASCII `VESCTOOL:<id>:<pass>\n` | once, on TCP connect | `vescinterface.cpp:3116-3122` |
| 1 | `COMM_FW_VERSION` (0), 1 byte | first at about 80 ms after connect, repeated until a reply arrives | `vescinterface.cpp:3245-3250` |
| 2 | `COMM_GET_CUSTOM_CONFIG_XML` (92), `COMM_GET_QML_UI_HW` (117), `COMM_GET_QML_UI_APP` (118), then `COMM_GET_CUSTOM_CONFIG` (93) per config | only if the FW version reply advertises custom configs or QML UIs; chunked reads of up to 400 bytes, 5 tries x 1.5 s each, cached on disk by `hwConfCrc` afterwards | `vescinterface.cpp:3888-4160` |
| 3 | `COMM_PING_CAN` (62) | once, as soon as the FW version (and custom configs) are in and the CAN list is empty; always on, not gated by a setting in `mainwindow.cpp` | `mainwindow.cpp:810-814`, `2126-2132`, `commands.cpp:1844-1855` |
| 4 | `COMM_FW_VERSION` (0), plus `COMM_FORWARD_CAN` (34) wrapping `COMM_FW_VERSION` for every CAN ID found | once, after the ping reply; each waits up to 4 s | `mainwindow.cpp:2142-2172`, `utility.cpp:1498-1530`, `utility.h:99-100` |
| 5 | `COMM_GET_MCCONF` (14) and `COMM_GET_APPCONF` (17) | every 400 ms tick until each config has been deserialised once in this VESC Tool process; each getter is gated for 1 s after a send, so on the wire about one request per 1.2 s until answered | `mainwindow.cpp:864-882`, `304-312`, `commands.cpp:1395-1405`, `1442-1451` |

Details that matter for a bridge:

- **FW version deadline is about 2 s.** `VescInterface::timerSlot` runs every 20 ms (`vescinterface.cpp:83-85`); every 4th tick it calls `getFwVersion()` and increments `mFwRetries`; at 25 retries (25 x 80 ms = 2.0 s) it shows "Could not read firmware version" and calls `disconnectPort()` (`vescinterface.cpp:3245-3263`). `getFwVersion()` itself is gated for 1 s after each send (`commands.cpp:1170-1180`, `mTimeoutCount = 100` ticks of a 10 ms timer at `commands.cpp:36-42`), so only about two `COMM_FW_VERSION` packets actually go out in that window (at roughly 80 ms and 1.1 s). The clock starts at TCP connect, before the hub has even paired the sockets.
- **Any `COMM_FW_VERSION` reply satisfies the probe**, solicited or not: the handler clears the gate and emits `fwVersionReceived` unconditionally (`commands.cpp:109-168`). Once `mFwVersionReceived` is true, later FW version replies are ignored by `VescInterface` (`vescinterface.cpp:3462-3467`).
- **Config reads repeat until answered.** `mMcConfRead` / `mAppConfRead` only flip when the config deserialises (`mainwindow.cpp:307-312`). If a bridge drops `COMM_GET_MCCONF` / `COMM_GET_APPCONF`, desktop VESC Tool keeps asking about every 1.2 s for as long as it is connected. A reply that fails to deserialise stops the retries (`mainwindow.cpp:872`, `vescinterface.cpp:4849-4851`). The flags are per process: reconnecting without restarting VESC Tool does not re-read.
- **CAN scan on connect** makes the board ping CAN IDs 0-254 with a 10 ms timeout each, about 2.5 s in the firmware's single blocking-command thread (`bldc/comm/commands.c:2355-2370`, `bldc/comm/comm_can.c:648-670`). It returns immediately-false per ID if `can_mode != CAN_MODE_VESC` (`comm_can.c:650-652`). VESC Tool waits up to 5 s for the reply (`commands.cpp:1850`, `2364-2369`).
- **Reconnect-to-last-CAN**: if VESC Tool previously talked to this board's UUID through CAN forwarding, on connect it sends `COMM_FORWARD_CAN` + `COMM_FW_VERSION` to that CAN ID (1.5 s wait) and, if the UUID matches, switches all traffic to CAN-forwarded (`vescinterface.cpp:3480-3493`). From then on every command is prefixed `[34, canId]` (`commands.cpp:2399-2402`).

### Idle (connected, no toolbar toggles on)

Once the FW version, custom configs and both configs are read, **desktop VESC Tool sends nothing periodically**. Every periodic sender is behind a checkable toolbar action that is unchecked by default (`mainwindow.ui:793-803`, `943-953` have no `checked` property):

| Toggle | Sends | Default rate | Source |
|---|---|---|---|
| "Stream realtime data" (`actionRtData`) | `COMM_GET_STATS` (128), `COMM_GET_VALUES` (4), `COMM_GET_VALUES_SETUP` (47) | 50 Hz setting (`poll_rate_rt_data`) | `mainwindow.cpp:481, 486-493` |
| "Stream realtime app data" (`actionRtDataApp`) | `COMM_GET_DECODED_ADC` (32), `COMM_GET_DECODED_CHUK` (33), `COMM_GET_DECODED_PPM` (31) | 50 Hz setting | `mainwindow.cpp:482, 495-502` |
| IMU (`actionIMU`) | `COMM_GET_IMU_DATA` (65), mask `0xFFFF` | 50 Hz setting | `mainwindow.cpp:483, 504-509` |
| BMS (`actionrtDataBms`) | `COMM_BMS_GET_VALUES` (96) | 10 Hz setting | `mainwindow.cpp:484, 511-516` |
| "Send Alive Commands" (`actionSendAlive`) | `COMM_ALIVE` (30) | every 10 x 20 ms = 200 ms (5 Hz) | `mainwindow.cpp:479, 849-857` |

- The poll timers feed a `PollManager` that drains one request per 10 ms (`pollmanager.cpp:25-91`), and each getter refuses to send again until its previous request is answered or 1 s passes (e.g. `commands.cpp:1256-1267`). So over a slow link the real rate self-limits to one request per round trip, not 50 Hz. Unsolicited replies of the same ID also clear that gate (see section 2), so a phone that already streams values makes VESC Tool's own polling free-run up to its timer rate.
- `COMM_ALIVE` is **not** sent by default. It turns on by itself when the user uses keyboard control, the full-brake button, or other control buttons (`mainwindow.cpp:638-655`, `945-949`, `1133-1137`).
- One remaining 1 Hz timer re-requests `COMM_GET_CUSTOM_CONFIG` (93) for any custom config that has never been received (`vescinterface.cpp:241-251`); it is silent on a board without custom configs.

## 2. How it handles unsolicited replies

There is no request/reply matching. `Commands::processPacket` switches on the first payload byte and emits a Qt signal for whatever arrives (`commands.cpp:103-1168`). Consequences:

- **Unsolicited `COMM_GET_VALUES` (4) / `COMM_GET_VALUES_SELECTIVE` (50)** are parsed and emitted as `valuesReceived` (`commands.cpp:189-291`). Listeners act on every one, whether or not RT streaming is on:
  - the status-bar current and duty gauges update (`mainwindow.cpp:241-242`, `1074-1079`);
  - the Realtime Data page text and plots append a sample (`pages/pagertdata.cpp:193-194`, `310-345`);
  - if RT logging is open, a log row is written per packet (`vescinterface.cpp:415-470`);
  - the Experiments page records only while an experiment runs (`pages/pageexperiments.cpp:172-199`).
- **Unsolicited `COMM_GET_VALUES_SETUP` (47) / selective (51)** are parsed (`commands.cpp:457-536`) and stored as the latest setup values (`vescinterface.cpp:405-408`).
- **Unsolicited `COMM_GET_DECODED_ADC` (32)** updates the ADC mapping widget on the ADC app page (`widgets/adcmap.cpp:58-59`, `63-95`).
- At 5-10 Hz this is far below the 50 Hz the tool polls at itself, so load is not a concern. The effect is that the desktop shows live data without the user enabling streaming.
- **Side effect on polling:** each received reply zeroes that command's in-flight gate (`mTimeoutValues = 0` at `commands.cpp:191`; same pattern at `110`, `315`, `351`, `405`, `412`, `421`, `459`). An unsolicited reply therefore lets the next request go out immediately.
- **Unsolicited config replies are applied.** A `COMM_GET_MCCONF` / `COMM_GET_APPCONF` reply that VESC Tool did not request is still deserialised into its in-memory config and marks it as updated (`commands.cpp:313-359`). Not a concern if the phone only polls values.
- **Unknown or unhandled command IDs** fall to `default: break;` and are dropped silently (`commands.cpp:1165-1166`).
- Decoded packets are also re-emitted to VESC Tool's own TCP/UDP server sockets if the user has those enabled (`vescinterface.cpp:218-225`, `254-261`); irrelevant unless someone chains tools.

## 3. How it handles `COMM_CUSTOM_APP_DATA` (36)

- The handler emits `customAppDataReceived(payload)` and does nothing else (`commands.cpp:437-439`). No parsing, no logging, no status message.
- In the desktop GUI **nothing is connected to that signal** by default. A repo-wide search finds listeners only in:
  - the command-line `bridgeAppData` mode, which prints the bytes to stdout (`main.cpp:1109-1114`);
  - example QML scripts the user must load by hand (`res/qml/Examples/BalanceUi.qml:149`, `res/qml/Examples/Mp3Stream.qml:79`).
- So phone GPS sent as `COMM_CUSTOM_APP_DATA` is **silently ignored** by desktop VESC Tool: not shown, not logged, no error. It would only be surfaced if the user loads a QML script that subscribes to `onCustomAppDataReceived`.
- Note the other direction: if a viewer sends ID 36 and the bridge forwards it to the board, the firmware hands it to a registered app-data handler and to LispBM (`bldc/comm/commands.c:771-778`). The bridge should consume its own ID-36 traffic rather than forward it.

## 4. Which commands write to the board

Classified from VESC Tool's senders (`commands.cpp:1170-2346`) and the firmware handlers (`bldc/comm/commands.c:231-1720`, `2096-2552`). IDs from `datatypes.h:852-1041`. "Read-only" means the firmware handler only builds a reply.

### Read-only (safe to forward)

| ID | Name |
|---|---|
| 0 | `COMM_FW_VERSION` |
| 4 | `COMM_GET_VALUES` |
| 14, 15 | `COMM_GET_MCCONF`, `COMM_GET_MCCONF_DEFAULT` |
| 17, 18 | `COMM_GET_APPCONF`, `COMM_GET_APPCONF_DEFAULT` |
| 31, 32, 33 | `COMM_GET_DECODED_PPM`, `COMM_GET_DECODED_ADC`, `COMM_GET_DECODED_CHUK` |
| 47 | `COMM_GET_VALUES_SETUP` |
| 50, 51 | `COMM_GET_VALUES_SELECTIVE`, `COMM_GET_VALUES_SETUP_SELECTIVE` |
| 65 | `COMM_GET_IMU_DATA` |
| 79 | `COMM_GET_DECODED_BALANCE` |
| 90 | `COMM_GET_IMU_CALIBRATION` (computes, does not store; runs in the blocking thread) |
| 91 | `COMM_GET_MCCONF_TEMP` |
| 92, 93, 94 | `COMM_GET_CUSTOM_CONFIG_XML`, `COMM_GET_CUSTOM_CONFIG`, `COMM_GET_CUSTOM_CONFIG_DEFAULT` |
| 96 | `COMM_BMS_GET_VALUES` |
| 111 | `COMM_PSW_GET_STATUS` |
| 115 | `COMM_GET_BATTERY_CUT` |
| 117, 118 | `COMM_GET_QML_UI_HW`, `COMM_GET_QML_UI_APP` |
| 122 | `COMM_IO_BOARD_GET_ALL` |
| 127 | `COMM_GET_EXT_HUM_TMP` |
| 128 | `COMM_GET_STATS` |
| 130, 134 | `COMM_LISP_READ_CODE`, `COMM_LISP_GET_STATS` |
| 137 | `COMM_BMS_GET_BATT_TYPE` |
| 140, 141 | `COMM_FILE_LIST`, `COMM_FILE_READ` |
| 150 | `COMM_GET_GNSS` |
| 157 | `COMM_FW_INFO` |

Read-only but with a side effect worth knowing:

- 62 `COMM_PING_CAN`: no state change, but puts up to 255 ping frames on the CAN bus and occupies the blocking thread for about 2.5 s. Sent automatically on every connect (section 1).
- 34 `COMM_FORWARD_CAN`: a wrapper. Payload is `[34, canId, innerId, ...]`; classify by `innerId` at byte 2 (`commands.cpp:2399-2402`, `bldc/comm/commands.c:733-747`).

### Motor control and control keep-alive (block)

| ID | Name |
|---|---|
| 5 | `COMM_SET_DUTY` |
| 6 | `COMM_SET_CURRENT` |
| 7 | `COMM_SET_CURRENT_BRAKE` |
| 8 | `COMM_SET_RPM` |
| 9 | `COMM_SET_POS` |
| 10 | `COMM_SET_HANDBRAKE` |
| 11 | `COMM_SET_DETECT` |
| 12 | `COMM_SET_SERVO_POS` |
| 30 | `COMM_ALIVE` (resets the board's command timeout, `bldc/comm/commands.c:700-703`) |
| 35 | `COMM_SET_CHUCK_DATA` |
| 63 | `COMM_APP_DISABLE_OUTPUT` (mutes the throttle app for a given time) |
| 84 | `COMM_SET_CURRENT_REL` |
| 159 | `COMM_MOTOR_ESTOP` (ignores all input for N ms, `bldc/comm/commands.c:1678-1682`) |

**The Escape key is a write.** In desktop VESC Tool, pressing Esc anywhere clicks the stop button (`mainwindow.cpp:601-605`), which sends `COMM_SET_CURRENT` 0 and `COMM_MOTOR_ESTOP` with 5000 ms by default (`mainwindow.cpp:1122-1131`). Forwarded to the car, that cuts throttle input for 5 s.

### Configuration and persistent-state writes (block)

| ID | Name |
|---|---|
| 13 | `COMM_SET_MCCONF` |
| 16, 149 | `COMM_SET_APPCONF`, `COMM_SET_APPCONF_NO_STORE` |
| 48, 49 | `COMM_SET_MCCONF_TEMP`, `COMM_SET_MCCONF_TEMP_SETUP` |
| 86 | `COMM_SET_BATTERY_CUT` |
| 87, 88 | `COMM_SET_BLE_NAME`, `COMM_SET_BLE_PIN` |
| 89 | `COMM_SET_CAN_MODE` |
| 95 | `COMM_SET_CUSTOM_CONFIG` |
| 110 | `COMM_SET_ODOMETER` |
| 129 | `COMM_RESET_STATS` |
| 158 | `COMM_CAN_UPDATE_BAUD_ALL` |

### Firmware, bootloader, reboot (block)

| ID | Name |
|---|---|
| 1, 59, 102, 106 | `COMM_JUMP_TO_BOOTLOADER` and `_ALL_CAN`, `_HW`, `_ALL_CAN_HW` |
| 2, 60, 103, 107 | `COMM_ERASE_NEW_APP` and variants |
| 3, 61, 81, 82, 104, 108 | `COMM_WRITE_NEW_APP_DATA` and `_ALL_CAN`, `_LZO`, `_ALL_CAN_LZO`, `_HW`, `_ALL_CAN_HW` |
| 73, 74, 105, 109 | `COMM_ERASE_BOOTLOADER` and variants |
| 29 | `COMM_REBOOT` |
| 156 | `COMM_SHUTDOWN` |

### Terminal, scripting, files (block)

| ID | Name |
|---|---|
| 20, 64 | `COMM_TERMINAL_CMD`, `COMM_TERMINAL_CMD_SYNC` (arbitrary terminal commands) |
| 120, 121 | `COMM_QMLUI_ERASE`, `COMM_QMLUI_WRITE` |
| 131, 132, 133 | `COMM_LISP_WRITE_CODE`, `COMM_LISP_ERASE_CODE`, `COMM_LISP_SET_RUNNING` |
| 138, 139, 152 | `COMM_LISP_REPL_CMD`, `COMM_LISP_STREAM_CODE`, `COMM_LISP_RMSG` (arbitrary code) |
| 142, 143, 144 | `COMM_FILE_WRITE`, `COMM_FILE_MKDIR`, `COMM_FILE_REMOVE` |

### Detection and sampling: these drive the motor or start board activity (block)

| ID | Name |
|---|---|
| 19 | `COMM_SAMPLE_PRINT` (starts a sampling run that streams back) |
| 24, 25, 26, 27, 28, 57, 58 | `COMM_DETECT_MOTOR_PARAM`, `_R_L`, `_FLUX_LINKAGE`, `COMM_DETECT_ENCODER`, `COMM_DETECT_HALL_FOC`, `_FLUX_LINKAGE_OPENLOOP`, `COMM_DETECT_APPLY_ALL_FOC` (the last also stores config) |

### Peripherals and other hardware (block)

| ID | Name |
|---|---|
| 36, 119 | `COMM_CUSTOM_APP_DATA`, `COMM_CUSTOM_HW_DATA` (handler-defined; treat as write) |
| 37, 52-56 | `COMM_NRF_START_PAIRING`, `COMM_EXT_NRF_*` |
| 38-46 | `COMM_GPD_*` (general-purpose drive output) |
| 66-72, 80, 83, 116, 125 | `COMM_BM_*` (SWD programmer for another chip; includes `COMM_BM_MEM_READ` because it drives pins) |
| 85 | `COMM_CAN_FWD_FRAME` (injects a raw CAN frame) |
| 97-101, 126, 136 | `COMM_BMS_SET_CHARGE_ALLOWED`, `_SET_BALANCE_OVERRIDE`, `_RESET_COUNTERS`, `_FORCE_BALANCE`, `_ZERO_CURRENT_OFFSET`, `_BLNC_SELFTEST`, `_SET_BATT_TYPE` |
| 112 | `COMM_PSW_SWITCH` |
| 123, 124 | `COMM_IO_BOARD_SET_PWM`, `COMM_IO_BOARD_SET_DIGITAL` |

### Board-to-tool only (a viewer never legitimately sends these)

21 `COMM_PRINT`, 22 `COMM_ROTOR_POSITION`, 23 `COMM_EXPERIMENT_SAMPLE`, 75-78 `COMM_PLOT_*`, 113 `COMM_BMS_FWD_CAN_RX`, 114 `COMM_BMS_HW_DATA`, 135 `COMM_LISP_PRINT`, 145-148 and 151 `COMM_LOG_*`.

### Practical shape for a bridge filter

An allow-list is the safer form: forward only the read-only IDs above, unwrap `COMM_FORWARD_CAN` and apply the same list to the inner ID, drop everything else. A deny-list would silently pass any command added to the firmware later.

Minimum allow-list for desktop VESC Tool to connect cleanly and show live data: 0, 4, 14, 17, 47, 50, 51, 62, 128, plus 34 wrapping those. Add 31, 32, 33, 65, 96 for the other streaming toggles, and 92, 93, 117, 118 if the board has custom configs or QML UIs. Without 14 and 17 the tool connects but retries the config reads about every 1.2 s forever (section 1).

## 5. `COMM_GET_DECODED_ADC` (32) layout

Request: one byte, `[32]` (`commands.cpp:1538-1549`).

Reply payload, 17 bytes, all big-endian signed 32-bit integers scaled by 1e6:

| Offset | Size | Field | Meaning |
|---|---|---|---|
| 0 | 1 | `32` | command ID |
| 1 | 4 | `decoded_level` x 1e6 | ADC1 after mapping, normally 0..1 |
| 5 | 4 | `voltage` x 1e6 | ADC1 pin voltage, V |
| 9 | 4 | `decoded_level2` x 1e6 | ADC2 after mapping, normally 0..1 |
| 13 | 4 | `voltage2` x 1e6 | ADC2 pin voltage, V |

- Firmware writer: `bldc/comm/commands.c:714-723`. VESC Tool parser: `commands.cpp:411-418`. Both agree on order and scale.
- The four values are statics written only inside the ADC app thread (`bldc/applications/app_adc.c:57-60`, `111-125`, `163-285`). If the board's app is not an ADC app, the reply still comes but the values stay at their last (initially zero) state.
- **No newer command supersedes it.** The enum has no other decoded-ADC command up to the last ID, 159 (`datatypes.h:852-1041`, `bldc/datatypes.h:997`, `1135-1152`), and VESC Tool's own app-data streaming still polls ID 32 (`mainwindow.cpp:497`). `COMM_GET_VALUES` and `COMM_GET_VALUES_SETUP` do not carry ADC inputs. `COMM_IO_BOARD_GET_ALL` (122) reports ADCs of a separate CAN IO board, not this.

## What was not established

- Nothing was tested live. In particular the 2 s FW version deadline over a real BLE + cellular path, and how desktop VESC Tool looks to a user while the phone streams unsolicited values, should be checked with a real attach before the specs rely on them.
- `COMM_FILE_*`, `COMM_GET_EXT_HUM_TMP` and a few BMS commands are not handled in `bldc/comm/commands.c` (they belong to VESC Express / BMS firmware); their classification above is by name and by VESC Tool's sender.
- Pages other than the main window (wizards, firmware page, Lisp page, scripting) send further commands when the user opens or uses them; only the connect path and the idle state were traced.
