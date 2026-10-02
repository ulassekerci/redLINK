# VESC TCP Hub — protocol notes for a custom client

Source: [vedderb/vesc_tool](https://github.com/vedderb/vesc_tool) at commit `dc53c658` (cloned to `vesc-docs/vesc_tool/`).
Everything below is read from that source; the `PING` exchange was also checked against the live public hub.

## 1. What the hub is

The hub is a dumb TCP relay. Two parties dial **out** to it, so neither needs a public IP:

```
 VESC ──BLE── VESC Tool mobile ──TCP──▶  HUB  ◀──TCP── your app
              ("Connect as Server")   veschub.vedder.se:65101   (the "VESCTOOL" role)
              registers as  VESC:<id>:<pass>            logs in as VESCTOOL:<id>:<pass>
```

- Each side sends **one ASCII line** to identify itself. After that the hub copies raw bytes between the two sockets and never looks at them again.
- The bytes are ordinary **VESC serial packets** — the same framing and commands used over UART/USB/BLE.
- Default public hub: `veschub.vedder.se`, port `65101`. You can self-host with `vesc_tool --tcpHub <port>` ([main.cpp:474](vesc_tool/main.cpp)).
- The whole server is ~100 lines: [tcphub.cpp:81](vesc_tool/tcphub.cpp).

## 2. Handshake

The first line must arrive within **5 s** and has exactly three `:`-separated tokens, terminated by `\n`:

```
<TYPE>:<ID>:<PASSWORD>\n
```

| Type | Sent by | Hub behaviour |
|---|---|---|
| `VESC` | The board side (VESC Tool mobile in "Connect as Server", or a VESC Express) | Registers the socket under `ID` with that password. Replaces any existing registration for the same ID. No reply. |
| `VESCTOOL` | **Your app** | If `ID` is registered and the password matches, the two sockets are bridged. **No reply on success.** On unknown ID or wrong password the hub just **closes the socket**. |
| `PING` | Anyone | Replies `PONG\n` if `ID` is registered, `NULL\n` if not, then closes. Password is ignored (VESC Tool sends `0`). |

Rules the hub applies ([tcphub.cpp:96-103](vesc_tool/tcphub.cpp)):

- `TYPE` and `ID` are upper-cased and have spaces stripped. The password is compared **exactly**.
- `ID` must be at least 3 characters.
- A `:` in the ID or password breaks the 3-token split, so the connection is rejected.
- One client per VESC: a new `VESCTOOL` login kicks the previous one.
- If the VESC side drops, the registration is deleted and your socket is closed. If your side drops, the VESC stays registered and you can log in again.

Because success is silent, detect it yourself: send a `COMM_FW_VERSION` request right after the login line and wait for the reply. Socket closed = bad ID/password or board offline. VESC Tool does exactly this ([vescinterface.cpp:3116](vesc_tool/vescinterface.cpp), then polls FW version every ~4 timer ticks, giving up after 25 tries).

### Where the ID and password come from

They are whatever was typed in VESC Tool mobile's TCP Hub box ([mobile/TcpHubBox.qml](vesc_tool/mobile/TcpHubBox.qml)). "Reset Defaults" fills the ID with the CRC-32C of the board's UUID string, as a decimal number ([utility.cpp:1636](vesc_tool/utility.cpp)). Your app only needs the same two strings; it never has to compute the ID.

### Check if the board is online

```
→ PING:<ID>:0\n
← PONG\n      (or NULL\n)
```

Open a fresh socket for each ping; the hub closes it after answering ([tcphub.cpp:49](vesc_tool/tcphub.cpp)).

## 3. Packet framing (after the handshake)

Both directions use the standard VESC frame ([packet.cpp:72](vesc_tool/packet.cpp)):

```
short:  0x02 | len (1 byte)        | payload | crc16 hi | crc16 lo | 0x03
long:   0x03 | len (2 bytes, BE)   | payload | crc16 hi | crc16 lo | 0x03
huge:   0x04 | len (3 bytes, BE)   | payload | crc16 hi | crc16 lo | 0x03
```

- Use the short form for payloads ≤ 255 bytes, long for ≤ 65535. A decoder rejects a long frame whose length would have fitted the shorter form.
- CRC is **CRC-16/XMODEM** (poly `0x1021`, init `0`, no reflection) over the payload only.
- `payload[0]` is the command ID (`COMM_*`); the rest is command-specific. All integers are **big-endian**.
- TCP is a stream: a `data` event may hold half a frame or several. Buffer and scan. On a bad start byte, bad CRC or bad stop byte, skip one byte and retry ([packet.cpp:186](vesc_tool/packet.cpp)).

Example — request firmware version: `02 01 00 00 00 03`. Request values: `02 01 04 40 84 03`.

### What the phone does in between

VESC Tool mobile decodes each frame from the hub and re-sends it to the board over BLE, and re-frames **every** packet the board sends back onto the hub socket ([vescinterface.cpp:218-225](vesc_tool/vescinterface.cpp)). Two consequences:

1. You will also receive replies to requests the phone itself made (e.g. if its RT Data page is open). Treat incoming packets as an event stream keyed by command ID, not as strict request/response.
2. Throughput and latency are bounded by BLE plus cellular. Poll at 5–10 Hz, not 50.

## 4. Commands useful for a dashboard

Full enum: [datatypes.h:852](vesc_tool/datatypes.h). Parsers: [commands.cpp](vesc_tool/commands.cpp). Requests are a single byte unless noted.

| ID | Name | Request payload | Use |
|---|---|---|---|
| 0 | `COMM_FW_VERSION` | `[0]` | Liveness check, FW version, hardware name, UUID |
| 4 | `COMM_GET_VALUES` | `[4]` | Raw motor-controller values for this one VESC |
| 47 | `COMM_GET_VALUES_SETUP` | `[47]` | **Best for a dashboard**: speed, battery %, distance, odometer |
| 50 | `COMM_GET_VALUES_SELECTIVE` | `[50, mask u32]` | Same as 4, only the fields whose bit is set |
| 51 | `COMM_GET_VALUES_SETUP_SELECTIVE` | `[51, mask u32]` | Same as 47, masked — saves bandwidth |
| 65 | `COMM_GET_IMU_DATA` | `[65, mask u16]` | Roll/pitch/yaw, accel, gyro, mag, quaternion |
| 96 | `COMM_BMS_GET_VALUES` | `[96]` | Cell voltages, SoC, temps (if a VESC BMS is present) |
| 128 | `COMM_GET_STATS` | `[128, mask u16]` | Avg/max speed, power, current, temps |
| 30 | `COMM_ALIVE` | `[30]` | Keep-alive; only needed while sending control commands |
| 34 | `COMM_FORWARD_CAN` | `[34, canId, ...inner payload]` | Wrap any command to reach another VESC on the CAN bus |

Selective replies start with the same `mask u32` you sent, then the selected fields in bit order.

### `COMM_FW_VERSION` reply (after the ID byte)

`major u8`, `minor u8`, `hw name` (NUL-terminated string), `uuid` (12 bytes), then optional trailing bytes: `isPaired`, `isTestFw`, `hwType`, `customConfigNum`, … ([commands.cpp:109](vesc_tool/commands.cpp)). Check remaining length before reading each optional field.

### `COMM_GET_VALUES_SETUP` reply (ID 47)

Number types: `i16/N` = signed 16-bit divided by N; `i32/N` likewise.

| Bit | Field | Encoding | Unit |
|---|---|---|---|
| 0 | temp_mos | i16/10 | °C |
| 1 | temp_motor | i16/10 | °C |
| 2 | current_motor | i32/100 | A |
| 3 | current_in | i32/100 | A |
| 4 | duty_now | i16/1000 | −1…1 |
| 5 | rpm | i32/1 | ERPM |
| 6 | speed | i32/1000 | m/s |
| 7 | v_in | i16/10 | V |
| 8 | battery_level | i16/1000 | 0…1 |
| 9 | amp_hours | i32/10000 | Ah |
| 10 | amp_hours_charged | i32/10000 | Ah |
| 11 | watt_hours | i32/10000 | Wh |
| 12 | watt_hours_charged | i32/10000 | Wh |
| 13 | tachometer (trip distance) | i32/1000 | m |
| 14 | tachometer_abs | i32/1000 | m |
| 15 | position | i32/1e6 | |
| 16 | fault_code | i8 | see below |
| 17 | vesc_id | u8 | CAN ID |
| 18 | num_vescs | u8 | |
| 19 | battery_wh | i32/1000 | Wh |
| 20 | odometer | u32 | m |
| 21 | uptime_ms | u32 | ms |

Total 69 bytes of fields (70-byte payload with the command ID). Speed, distance and battery level depend on the motor/wheel/battery settings stored on the board. Units are my reading of how VESC Tool displays them; verify against your board.

### `COMM_GET_VALUES` reply (ID 4)

In order: temp_mos `i16/10`, temp_motor `i16/10`, current_motor `i32/100`, current_in `i32/100`, id `i32/100`, iq `i32/100`, duty_now `i16/1000`, rpm `i32`, v_in `i16/10`, amp_hours `i32/1e4`, amp_hours_charged `i32/1e4`, watt_hours `i32/1e4`, watt_hours_charged `i32/1e4`, tachometer `i32`, tachometer_abs `i32`, fault_code `i8`, then (if bytes remain) position `i32/1e6`, vesc_id `u8`, temp_mos_1..3 `3 × i16/10`, vd `i32/1000`, vq `i32/1000`, status `u8` (bit0 = has_timeout, bit1 = kill switch) ([commands.cpp:189](vesc_tool/commands.cpp)).

### Other encodings

- **Float "auto" (`vbPopFrontDouble32Auto`)**, used by IMU and stats replies: bit-compatible with an IEEE-754 single, so `DataView.getFloat32(offset, false)` reads it.
- **Fault codes** ([datatypes.h:93](vesc_tool/datatypes.h)): 0 none, 1 over-voltage, 2 under-voltage, 3 DRV, 4 abs over-current, 5 over-temp FET, 6 over-temp motor, … up to 33.

## 5. Building it in React

**A browser cannot open raw TCP sockets**, and the hub speaks plain TCP (no WebSocket, no TLS). So a pure web React app cannot talk to it directly. Options:

| App type | How |
|---|---|
| **React Native / Expo** (recommended if this lives in redLink) | `react-native-tcp-socket`. It is a native module, so it needs a dev build / EAS build, not Expo Go. |
| Web React | Run a small WebSocket↔TCP relay (Node `net` + `ws`, ~40 lines) and connect the browser to that. Keep the framing code in the client. |
| Electron / Tauri | Use Node's `net` (or Rust) in the main process, forward frames to the renderer. |

### Reference client (TypeScript, transport-agnostic)

```ts
// --- framing -------------------------------------------------------------
export function crc16(buf: Uint8Array): number {
  let crc = 0
  for (const b of buf) {
    crc ^= b << 8
    for (let i = 0; i < 8; i++) crc = crc & 0x8000 ? ((crc << 1) ^ 0x1021) & 0xffff : (crc << 1) & 0xffff
  }
  return crc
}

export function encodePacket(payload: Uint8Array): Uint8Array {
  const long = payload.length > 255
  const out = new Uint8Array(payload.length + (long ? 6 : 5))
  let o = 0
  if (long) { out[o++] = 3; out[o++] = payload.length >> 8; out[o++] = payload.length & 0xff }
  else { out[o++] = 2; out[o++] = payload.length }
  out.set(payload, o); o += payload.length
  const crc = crc16(payload)
  out[o++] = crc >> 8; out[o++] = crc & 0xff; out[o] = 3
  return out
}

export class PacketDecoder {
  private buf = new Uint8Array(0)

  /** Feed raw socket bytes, get back zero or more complete payloads. */
  push(chunk: Uint8Array): Uint8Array[] {
    const merged = new Uint8Array(this.buf.length + chunk.length)
    merged.set(this.buf); merged.set(chunk, this.buf.length)
    this.buf = merged

    const packets: Uint8Array[] = []
    let i = 0
    while (i < this.buf.length) {
      const start = this.buf[i]
      if (start !== 2 && start !== 3) { i++; continue }
      const hdr = start // 2 => 1 length byte, 3 => 2 length bytes
      if (this.buf.length - i < hdr) break
      const len = start === 2 ? this.buf[i + 1] : (this.buf[i + 1] << 8) | this.buf[i + 2]
      if (len === 0 || (start === 3 && len < 255)) { i++; continue }
      const total = hdr + len + 3
      if (this.buf.length - i < total) break
      const payload = this.buf.subarray(i + hdr, i + hdr + len)
      const crcRx = (this.buf[i + hdr + len] << 8) | this.buf[i + hdr + len + 1]
      if (this.buf[i + total - 1] !== 3 || crc16(payload) !== crcRx) { i++; continue }
      packets.push(payload.slice())
      i += total
    }
    this.buf = this.buf.slice(i)
    return packets
  }
}

// --- one parser ----------------------------------------------------------
export function parseValuesSetup(p: Uint8Array) {
  const v = new DataView(p.buffer, p.byteOffset, p.byteLength)
  let o = 1 // skip command id (47)
  const i16 = (s: number) => { const x = v.getInt16(o) / s; o += 2; return x }
  const i32 = (s: number) => { const x = v.getInt32(o) / s; o += 4; return x }
  return {
    tempMos: i16(10), tempMotor: i16(10),
    currentMotor: i32(100), currentIn: i32(100),
    duty: i16(1000), erpm: i32(1), speedMs: i32(1000),
    vIn: i16(10), batteryLevel: i16(1000),
    ah: i32(1e4), ahCharged: i32(1e4), wh: i32(1e4), whCharged: i32(1e4),
    tripM: i32(1000), tripAbsM: i32(1000), position: i32(1e6),
    fault: v.getInt8(o++), vescId: v.getUint8(o++), numVescs: v.getUint8(o++),
    batteryWh: i32(1000),
    odometerM: (() => { const x = v.getUint32(o); o += 4; return x })(),
    uptimeMs: (() => { const x = v.getUint32(o); o += 4; return x })(),
  }
}
```

### Connection flow (React Native, `react-native-tcp-socket`)

```ts
import TcpSocket from 'react-native-tcp-socket'

const socket = TcpSocket.createConnection({ host: 'veschub.vedder.se', port: 65101 }, () => {
  socket.write(`VESCTOOL:${id}:${pass}\n`)           // 1. login (no reply on success)
  socket.write(encodePacket(Uint8Array.of(0)))        // 2. COMM_FW_VERSION as a liveness probe
})

const decoder = new PacketDecoder()
socket.on('data', (d) => {
  for (const p of decoder.push(new Uint8Array(d as Buffer))) {
    if (p[0] === 0) startPolling()                    // board answered: we are bridged
    if (p[0] === 47) onValues(parseValuesSetup(p))
  }
})
socket.on('close', () => { /* wrong id/pass, board offline, or link dropped → retry with backoff */ })

const startPolling = () => setInterval(() => socket.write(encodePacket(Uint8Array.of(47))), 200)
```

(Depending on the library version `write` may want a `Buffer`; wrap with `Buffer.from(...)`.)

## 6. Things to watch out for

- **No encryption.** ID, password and all traffic cross the internet in clear text, and the password is the only access control. Anyone who is bridged can send *any* VESC command — including motor control, config writes and firmware upload. For a read-only dashboard, only ever send the `GET_*` commands above. Consider self-hosting the hub if this matters.
- **Silent failures.** Wrong password, unknown ID and board-offline all look the same: the socket closes. Use `PING` first to tell "offline" apart from "wrong password".
- **The phone is part of the link.** If VESC Tool mobile is backgrounded and the OS suspends it, or BLE drops, the data stops. The hub only closes your socket if the phone's TCP connection actually ends.
- **Single client.** Opening desktop VESC Tool against the same ID disconnects your app, and vice versa.
- **CAN-connected VESCs.** Wrap requests in `COMM_FORWARD_CAN` (`[34, canId, ...]`); the reply comes back unwrapped, so use `vesc_id` in the reply to tell boards apart.

## 7. GPS / location

**The phone's GPS is not available through the hub when the board side is VESC Tool mobile.**

- The phone↔hub bridge forwards only packets to and from the VESC ([vescinterface.cpp:218-225](vesc_tool/vescinterface.cpp)); VESC Tool never injects packets of its own.
- VESC Tool reads the phone's position only while RT logging is on, and only writes it to the local log file ([vescinterface.cpp:427](vesc_tool/vescinterface.cpp), [vescinterface.cpp:1848](vesc_tool/vescinterface.cpp)).
- The protocol does have `COMM_GET_GNSS` (ID 150, request `[150, mask u16]`), but it returns position held by the **VESC hardware** (e.g. a VESC Express with a GNSS module), not the phone. Reply: `mask u32`, then lat `i64/1e16`, lon `i64/1e16`, height, speed, hdop (float32), ms_today `i32`, yy `i16`, mo `i8`, dd `i8`, age_s (float32) ([commands.cpp:1100](vesc_tool/commands.cpp)).

Ways to get location to the remote client:

1. Keep a custom phone app that registers with the hub as `VESC:<id>:<pass>`, bridges BLE↔TCP itself, and injects its own framed packets carrying GPS (the hub is byte-transparent, so any payload works; `COMM_CUSTOM_APP_DATA` = 36 is a safe command ID to borrow).
2. Add a GNSS module to the vehicle (VESC Express) and poll `COMM_GET_GNSS`.

### Injection test (public hub, throwaway ID)

Registered as `VESC:<random>:<random>`, then logged in as `VESCTOOL` from a second socket:

- Arbitrary bytes from the VESC side (`deadbeef`, and a framed `COMM_CUSTOM_APP_DATA` packet) arrived at the client unchanged, and vice versa. The hub does not inspect or validate the stream.
- Bytes sent by the VESC side **before** a client was bridged were not dropped: the hub held them and delivered them together with the next data after the client connected. So a phone app that pushes GPS continuously with no client attached builds up a stale backlog on the hub. Prefer request/response (client asks, phone answers), or push only while requests from the client keep arriving.
