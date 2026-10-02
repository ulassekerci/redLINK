# redLINK protocol

The wire format the Android app and the desktop app share. `android/` and `desktop/` each hold their own implementation; this directory holds no code.

- This file: framing, the board commands, our own messages, and the list of test vectors.
- `vectors.json`: the test cases both test suites read.
- [`tcp-hub.md`](tcp-hub.md): notes on the hub.

How the bridge and the viewers use these messages on the hub (the lobby, joining, timings and states) is in the [hub usage spec](../.scratch/hub-migration/spec-hub.md). Vocabulary is from [`CONTEXT.md`](../CONTEXT.md).

## Framing

Every byte after the login line, in both directions, is a standard VESC frame. The direct link uses the same framing, so one parser serves both paths.

```
short:  0x02 | length (1 byte)             | payload | crc hi | crc lo | 0x03
long:   0x03 | length (2 bytes, big-endian) | payload | crc hi | crc lo | 0x03
```

- The short form carries payloads up to 255 bytes, the long form up to 65,535. Nothing in this protocol needs the long form, but a decoder accepts it. A long frame whose length would have fitted the short form is rejected, and so is a length of zero.
- The CRC is CRC-16/XMODEM (polynomial `0x1021`, initial value 0, not reflected) over the payload only.
- The first payload byte is the command ID. All integers are big-endian.
- TCP delivers a stream: one read may hold part of a frame or several frames. A decoder buffers and scans. On a bad start byte, bad CRC or bad stop byte it skips one byte and tries again.

## Board commands

The bridge polls the board with two requests. Their replies are the board's part of the stream. A third request is sent once per Bluetooth connect, and its reply stays on the phone.

| ID | Command | Request payload | Reply |
|---|---|---|---|
| 0 | `COMM_FW_VERSION` | `[0]` | below; never copied to a registration |
| 47 | `COMM_GET_VALUES_SETUP` | `[47]` | 70-byte payload, below |
| 32 | `COMM_GET_DECODED_ADC` | `[32]` | 17-byte payload, below |

`COMM_GET_VALUES` (4) is not polled. Speed and distance come only from the setup reply, which the board computes from its own wheel diameter, gear ratio and pole count. Neither app holds those constants.

**`COMM_GET_VALUES_SETUP` reply,** after the command ID, in order. `i16/N` is a signed 16-bit integer divided by N.

| Field | Encoding | Unit |
|---|---|---|
| MOSFET temperature | i16/10 | °C |
| motor temperature | i16/10 | °C |
| motor current | i32/100 | A |
| battery current | i32/100 | A |
| duty cycle | i16/1000 | -1 to 1 |
| ERPM | i32 | ERPM |
| speed | i32/1000 | m/s |
| battery voltage | i16/10 | V |
| battery level | i16/1000 | 0 to 1 |
| charge used | i32/10000 | Ah |
| charge charged | i32/10000 | Ah |
| energy used | i32/10000 | Wh |
| energy charged | i32/10000 | Wh |
| distance | i32/1000 | m |
| absolute distance | i32/1000 | m |
| position | i32/1000000 | |
| fault code | i8 | |
| board ID | u8 | |
| number of boards | u8 | |
| battery capacity | i32/1000 | Wh |
| odometer | u32 | m |
| board uptime | u32 | ms |

The board runs firmware 6.06, which sends every field, the last two included (6.05 does too). A parser reads the fields it finds and treats a shorter reply as missing its tail, not as an error.

**`COMM_GET_DECODED_ADC` reply,** after the command ID: level 1, voltage 1, level 2, voltage 2, each `i32/1000000`.

**`COMM_FW_VERSION` reply,** after the command ID: firmware major (u8), firmware minor (u8), then bytes this protocol does not read. The bridge sends the request once after each Bluetooth connect, before polling starts, and waits up to 250 ms; the [Android app spec](../.scratch/hub-migration/spec-android.md) says what it does with the answer. Viewers never see it.

## Our messages

Everything the bridge and a viewer say to each other that is not a board reply is a `COMM_CUSTOM_APP_DATA` frame: payload `[36, type, ...]`, integers big-endian.

| Type | Name | Direction | Content after the type byte |
|---|---|---|---|
| 1 | GPS | bridge to viewer | 26 bytes, below |
| 2 | lobby request | viewer to bridge | the token, 8 ASCII characters |
| 3 | heartbeat | viewer to bridge | nothing |
| 4 | status | bridge to viewer | 2 bytes, below |

**GPS,** 26 bytes:

| Offset | Field | Encoding |
|---|---|---|
| 0 | latitude | i32, degrees x 10,000,000 |
| 4 | longitude | i32, degrees x 10,000,000 |
| 8 | altitude | i32, metres x 100 |
| 12 | speed | u16, m/s x 100 |
| 14 | heading | u16, degrees x 100 |
| 16 | horizontal accuracy | u16, metres x 10 |
| 18 | fix time | u64, Unix milliseconds |

A field the fix does not carry (altitude, speed, heading or accuracy) is sent as 0. A viewer cannot tell that from a measured 0.

**Status,** 2 bytes:

| Offset | Field | Encoding |
|---|---|---|
| 0 | protocol version | u8, the bridge app's major version |
| 1 | board state | u8: 0 answering, 1 unreachable |

**Lobby request:** the token is 8 characters from the team code alphabet (the [hub usage spec](../.scratch/hub-migration/spec-hub.md), 2.7). The bridge ignores a request whose token is anything else.

**Tolerance, which must hold from the first release:**

- A viewer ignores a message type it does not know.
- A viewer ignores extra bytes at the end of a message it does know.
- The bridge discards every frame from a viewer that is not a lobby request on the lobby or a heartbeat on a viewer's registration. Nothing a viewer sends reaches the board.

## Vectors

`vectors.json` holds two groups. Both the Kotlin and the TypeScript suite read the file and run every case. The list below names the cases; "The file" after it gives their shape.

**Frames,** each a hex string with the fields a decoder must produce, or the fact that it produces nothing:

- `frame-short`: a valid short frame.
- `frame-long`: a valid long frame.
- `frame-long-fits-short`: a long frame with a length under 256; rejected.
- `frame-zero-length`: rejected.
- `frame-bad-crc`: rejected.
- `frame-bad-stop-byte`: rejected.
- `frame-garbage-before`: bytes that are no frame, then a valid frame; the frame is decoded.
- `frame-two-in-one-read`: two frames back to back; both decoded.
- `frame-split-across-reads`: one frame delivered in two pieces; decoded once.
- `request-values-setup`: the encoded request for command 47.
- `request-decoded-adc`: the encoded request for command 32.
- `reply-values-setup`: a full 6.06 reply with every field.
- `reply-values-setup-negative`: negative currents, speed and temperature.
- `reply-values-setup-short`: a reply without the odometer and uptime; the rest is decoded.
- `reply-decoded-adc`: a full reply.
- `request-fw-version`: the encoded request for command 0.
- `reply-fw-version`: a 6.06 reply; major 6 and minor 6 are decoded, the rest ignored.
- `gps`: a full GPS message.
- `gps-southern-western`: negative latitude and longitude.
- `gps-trailing-bytes`: extra bytes after the fix time; decoded, the extra ignored.
- `lobby-request`: a request with a well-formed token.
- `lobby-request-short-token`: ignored by the bridge.
- `lobby-request-bad-alphabet`: a token containing `0`, `I` or a lower-case letter; ignored by the bridge.
- `heartbeat`: the heartbeat.
- `status-answering`: board state 0.
- `status-unreachable`: board state 1.
- `status-trailing-bytes`: extra bytes after the board state; decoded, the extra ignored.
- `custom-unknown-type`: a `COMM_CUSTOM_APP_DATA` frame with a type byte nobody defined; ignored.
- `unknown-command`: a valid frame with a command ID neither app handles; ignored.

**Team codes,** each a typed string with its validity and, when valid, its normalised code, lobby ID, password and the viewer ID for a given token:

- `code-valid`: a code as the phone shows it.
- `code-lower-case-and-spaces`: the same code typed in lower case with spaces and no dash; valid, same result.
- `code-wrong-check-character`: rejected.
- `code-one-wrong-character`: one of the first seven changed; rejected.
- `code-neighbours-swapped`: two neighbouring characters swapped; rejected.
- `code-bad-alphabet`: contains `0`, `1`, `I`, `L` or `O`; rejected.
- `code-too-short`: rejected.
- `code-check-character-last`: a valid code whose check character is `Z`, the last alphabet entry, to catch an off-by-one in the modulo.

### The file

One JSON object with two arrays, `frames` and `team_codes`. Every case has a `name` from the lists above. Hex strings are lower case with no separators. A suite fails when it meets a `check` it does not know, so no case is skipped silently.

**Frame cases** have a `check` saying what to run:

| `check` | Fields | What a suite does |
|---|---|---|
| `deframe` | `reads`: hex strings; `payloads`: hex strings | feeds each read in turn to one decoder and expects exactly these payloads, in order, from all reads together. An empty `payloads` means the bytes are rejected. |
| `encode` | `message`; `frame`: hex | encodes the message and expects exactly the frame's bytes. |
| `decode` | `frame`: hex; `message`, or `null` | deframes the frame, decodes its payload and expects the message. `null` means the payload is ignored. |
| `both` | `frame`; `message` | runs `encode` and `decode` on the same pair. |

A `message` is an object whose `type` names it; the other keys are its fields, with the units in their names. Numbers are JSON numbers: a scaled field is the integer on the wire divided by its scale (`i16/10` as 36.5), and a decoder that divides in double precision gets exactly the value in the file. An encoder multiplies by the scale and rounds to the nearest integer; it does not truncate. `odometer_m`, `board_uptime_ms` and `gps_fix_time_utc` can exceed a signed 32-bit integer and need a 64-bit one.

| `type` | Fields |
|---|---|
| `fw_version_request`, `values_setup_request`, `decoded_adc_request` | none |
| `fw_version` | `fw_major`, `fw_minor` |
| `values_setup` | in reply order: `mosfet_temp_c`, `motor_temp_c`, `motor_current_a`, `battery_current_a`, `duty_cycle`, `erpm`, `speed_m_s`, `battery_voltage_v`, `battery_level`, `charge_used_ah`, `charge_charged_ah`, `energy_used_wh`, `energy_charged_wh`, `distance_m`, `distance_abs_m`, `position`, `fault_code`, `board_id`, `board_count`, `battery_capacity_wh`, `odometer_m`, `board_uptime_ms`. A short reply's message leaves out the keys it did not reach. |
| `decoded_adc` | `adc_level1`, `adc_voltage1`, `adc_level2`, `adc_voltage2` |
| `gps` | `gps_lat_deg`, `gps_lon_deg`, `gps_alt_m`, `gps_speed_m_s`, `gps_heading_deg`, `gps_accuracy_m`, `gps_fix_time_utc` (Unix milliseconds) |
| `lobby_request` | `token` |
| `heartbeat` | none |
| `status` | `protocol_version`, `board_state` |

**Team-code cases** have `typed`, the string as a person typed it, and `valid`. A valid case also has `code` (the normalised code, without its dash), `lobby_id`, `password`, `token` and `viewer_id`, the viewer ID for that code and token.
