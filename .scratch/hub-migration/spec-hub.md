# Hub usage spec

Part of the [hub migration spec](spec.md). Read the front page first. The [Android app spec](spec-android.md) and the [desktop app spec](spec-desktop.md) cite this file for every message name, state name and timing on the hub; they do not restate them.

Vocabulary is from `CONTEXT.md`. Where this spec and a ticket disagree, this spec wins.

Sections marked **(moves)** are the wire format. The first implementation step moves them into `protocol/README.md` and leaves a link here. Until then this file is their only home.

## 1. Purpose

The hub joins one registration to one client and copies bytes between them. It knows nothing about several viewers, about GPS, or about whether anyone is watching. This spec describes the protocol the bridge and the viewers run on top of it so that:

- every laptop in the pit watches the board's stream at once, with no shared network and no setup beyond one team code;
- a viewer can tell live from board unreachable from phone lost;
- nothing is written to the hub that nobody reads;
- the stock hub is used unchanged, public or self-hosted.

## 2. Behaviour

### 2.1 The hub

What the hub does, as far as this protocol relies on it. `tcp-hub.md` has the full notes.

- **Address:** the public hub is `veschub.vedder.se`, port `65101`. Plain TCP, no encryption.
- **Login line:** the first bytes on a connection are one ASCII line, `<TYPE>:<ID>:<PASSWORD>\n`, which must arrive within 5 s.
  - `VESC` registers the connection under the ID. It replaces any existing registration of the same ID, whose socket and attached client the hub closes. No reply.
  - `VESCTOOL` attaches the connection to the registration of that ID if the password matches. No reply on success; on an unknown ID or a wrong password the hub closes the socket. A new attach kicks the client attached before it.
  - `PING` answers `PONG\n` if the ID is registered and `NULL\n` if not, then closes. The password is ignored; send `0`. Every `PING` is a fresh connection.
- **IDs and passwords:** the hub upper-cases the ID and strips its spaces, compares the password exactly, and rejects a `:` in either. An ID is at least 3 characters.
- **After login** the hub copies bytes in both directions and never inspects them.
- **When the registering side closes,** the registration is deleted and the attached client's socket is closed. When the client closes, the registration stays.
- **Facts the protocol is built around:**
  - Bytes written to a registration with no client attached are buffered without bound and delivered to the next client.
  - Bytes sent in the same write as the login line are lost. A party cannot know when its login took effect: another party's slow connection can make the hub discard what we send for up to 5 s, or reset our connection.
  - Of several `VESCTOOL` logins to one registration within about 100 ms, each followed by a packet, only one packet arrives.
  - When the registering side goes silent without closing, the hub keeps the registration: `PING` answers `PONG` for about 17 minutes once anything has been written to it, and for about 2 hours 13 minutes if nothing ever was.
  - There is no cap on registrations and no idle timeout in the hub's code. Nothing is published about the public hub's capacity or uptime.

Tickets: [Hub limits for multiple registrations](issues/01-hub-limits-for-multiple-registrations.md), [Stale re-registration on the public hub](issues/13-stale-re-registration-on-the-public-hub.md).

### 2.2 Framing (moves)

Every byte after the login line, in both directions, is a standard VESC frame. The direct link uses the same framing, so one parser serves both paths.

```
short:  0x02 | length (1 byte)             | payload | crc hi | crc lo | 0x03
long:   0x03 | length (2 bytes, big-endian) | payload | crc hi | crc lo | 0x03
```

- The short form carries payloads up to 255 bytes, the long form up to 65,535. Nothing in this protocol needs the long form, but a decoder accepts it. A long frame whose length would have fitted the short form is rejected, and so is a length of zero.
- The CRC is CRC-16/XMODEM (polynomial `0x1021`, initial value 0, not reflected) over the payload only.
- The first payload byte is the command ID. All integers are big-endian.
- TCP delivers a stream: one read may hold part of a frame or several frames. A decoder buffers and scans. On a bad start byte, bad CRC or bad stop byte it skips one byte and tries again.

Tickets: [What travels on the stream](issues/05-what-travels-on-the-stream.md), [How several pit laptops watch at once](issues/04-how-several-pit-laptops-watch-at-once.md).

### 2.3 Board commands (moves)

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

**`COMM_FW_VERSION` reply,** after the command ID: firmware major (u8), firmware minor (u8), then bytes this protocol does not read. The bridge sends the request once after each Bluetooth connect, before polling starts, and waits up to 250 ms; the Android spec says what it does with the answer. Viewers never see it.

Tickets: [What the phone log contains](issues/08-what-the-phone-log-contains.md), [Board speed settings and firmware version](issues/14-board-speed-settings-and-firmware-version.md). `COMM_FW_VERSION` was added while writing the Android spec.

### 2.4 Our messages (moves)

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

**Status,** 2 bytes:

| Offset | Field | Encoding |
|---|---|---|
| 0 | protocol version | u8, the bridge app's major version |
| 1 | board state | u8: 0 answering, 1 unreachable |

**Lobby request:** the token is 8 characters from the team code alphabet (2.7). The bridge ignores a request whose token is anything else.

**Tolerance, which must hold from the first release:**

- A viewer ignores a message type it does not know.
- A viewer ignores extra bytes at the end of a message it does know.
- The bridge discards every frame from a viewer that is not a lobby request on the lobby or a heartbeat on a viewer's registration. Nothing a viewer sends reaches the board.

Tickets: [What travels on the stream](issues/05-what-travels-on-the-stream.md), [How hub credentials are set and shared](issues/10-how-hub-credentials-are-set-and-shared.md), [Distribution](issues/15-distribution.md).

### 2.5 Protocol version

- The protocol version byte in the status message is the major version of the app the bridge runs in. A viewer compares it with its own app's major version.
- Within one major the messages change only by addition, which the tolerance rules in 2.4 make safe. An incompatible change is a new major release of both apps.
- When the two majors differ, in either direction, the viewer is in the state **version mismatch**: it shows nothing from the stream. It stays attached to its registration, keeps sending its heartbeat and keeps reading status, and leaves the state only through phone lost and a new lobby visit.
- A local build takes its version from the latest version tag in git, with `-dev` added (`1.2.0-dev`), so its protocol version is the major of the latest release and it works against released builds of that major. Nothing refuses a local build whose messages have changed incompatibly before the next major is tagged. With no tag to read, a local build is `0.0.0-dev` and its protocol version is 0.
- The direct link carries no protocol version.

Tickets: [Distribution](issues/15-distribution.md), [ADR 0002](../../docs/adr/0002-protocol-version-is-the-app-major-version.md). That a mismatched viewer stays attached, and that a local build takes its version from the latest tag, were decided while writing this spec.

### 2.6 Polling and the stream

- **Polling:** during a run the bridge sends `COMM_GET_VALUES_SETUP`, waits for the reply, sends `COMM_GET_DECODED_ADC`, waits again, on a 50 ms cycle (20 Hz). Only the bridge polls; a viewer never does.
- **Missing replies:** the bridge gives up on a command after 250 ms and sends the next. After 2 s with no reply at all, or at once on a Bluetooth disconnect, the board is unreachable. Polling continues.
- **What is copied:** each reply frame is written unchanged to every active registration. Nothing is added to a board frame; viewers stamp samples on arrival.
- **GPS:** one GPS message per fix, at 1 Hz, to every active registration. Nothing is sent while there is no fix. GPS keeps flowing while the board is unreachable.
- **Status:** to every active registration once a second, at once when the board state changes, and at once on a registration's first heartbeat.
- **Active registration:** a registration is active from its first heartbeat. The bridge writes nothing to a registration before that. After 3 s without a heartbeat it stops writing until the next one arrives. After 10 s without a heartbeat, counted from opening for a registration that never had one, it closes the registration. The most that can sit unread on the hub is 3 s of stream.
- **The lobby is never written to.**
- **Slow viewer:** each registration has a small bounded queue. When it is full the oldest samples are dropped for that viewer only. Other viewers and the log of record are unaffected.
- **Sockets:** the bridge turns on TCP keep-alive on every hub socket and never sends an in-band keep-alive.
- **Cost:** a poll cycle is 97 bytes of frames (75 for the setup reply, 22 for the ADC reply). With GPS and status that is about 2 kB/s per viewer, about 57 MB per hour of phone data with 8 viewers.

Tickets: [What travels on the stream](issues/05-what-travels-on-the-stream.md), [What the phone log contains](issues/08-what-the-phone-log-contains.md), [Hub limits for multiple registrations](issues/01-hub-limits-for-multiple-registrations.md).

### 2.7 Team code and hub identities

- **Team code:** 8 characters from the 31-character alphabet `23456789ABCDEFGHJKMNPQRSTUVWXYZ` (no `0`, `1`, `I`, `L` or `O`), shown as `XXXX-XXXX`. The first seven are random, about 35 bits. The eighth is a check character.
- **Check character:** give each character its position in the alphabet (0 to 30). Multiply the first seven positions by the weights 1 to 7, add the products and take the sum modulo 31. The check character is the alphabet entry at that position. This catches every single wrong character and every swap of two neighbours. Example: `K7QM-3XPC` (17x1 + 5x2 + 21x3 + 18x4 + 1x5 + 28x6 + 20x7 = 475, and 475 modulo 31 is 10, which is `C`).
- **Reading a typed code:** lower case is accepted, dashes and spaces are ignored, and a code whose check character is wrong is rejected.
- **Lobby ID:** `REDLINK` followed by the code without its dash: `REDLINKK7QM3XPC`.
- **Viewer ID:** the lobby ID followed by the viewer's token: `REDLINKK7QM3XPC` + `4HT9WQ2B`.
- **Password:** the code without its dash, for the lobby and for every viewer registration.
- **Token:** 8 random characters from the same alphabet, made fresh for every lobby visit.
- **What it protects:** the code keeps out strangers who are guessing. It crosses the internet in clear text on every connection, so anyone who can see the traffic has it.

Who generates the code and where it is typed is in the Android and desktop specs.

Tickets: [How hub credentials are set and shared](issues/10-how-hub-credentials-are-set-and-shared.md).

### 2.8 The lobby, bridge side

- **During a run** the bridge holds the lobby: it registers `VESC:<lobby ID>:<password>` and confirms it with `PING`, registering again on `NULL`. Outside a run the bridge holds no registration.
- **On a lobby request** with a well-formed token the bridge opens a new connection, registers `VESC:<viewer ID>:<password>` and confirms it with `PING`, registering again on `NULL`. A request for a token that already has a registration is a no-op. A malformed request is ignored.
- **The bridge never refuses a viewer.** The protocol is tested with 8 and promises nothing above that.
- **Registrations follow the bridge's hub connection, not the board's.** When the board is unreachable every registration stays, and viewers learn it from the status message.
- **When the hub connection drops,** the bridge re-registers the lobby only, confirming with `PING`: first attempt at once, then waits of 250 ms doubling to a 10 s cap, each randomised by 20% either way, and at once when Android reports a network change. It does not re-open viewer registrations it lost; viewers come back through the lobby. It closes its own old sockets itself, because the hub may leave them open.
- **Stale registrations:** a lobby the hub still holds from before a silent drop is replaced at once by registering the same ID again. Stale viewer registrations stay on the hub for about 17 minutes and harm nothing, because viewers return with fresh tokens. The hub can lose a re-registered ID when a large backlog sits on the old one; heartbeats and lobby requests never build such a backlog, so the bridge does not guard against it.

Tickets: [How several pit laptops watch at once](issues/04-how-several-pit-laptops-watch-at-once.md), [Android runtime and stack](issues/07-android-runtime-and-stack.md), [Stale re-registration on the public hub](issues/13-stale-re-registration-on-the-public-hub.md), [Desktop app architecture](issues/11-desktop-app-architecture.md), [ADR 0001](../../docs/adr/0001-one-registration-per-viewer-through-a-lobby.md).

### 2.9 Joining, viewer side

A viewer with a team code joins by itself and keeps trying for as long as the app is open.

**One lobby visit:**

1. `PING` the lobby ID. If the hub cannot be reached, the state is hub unreachable. On `NULL` the state is phone not found. On `PONG` continue; the state is joining.
2. Make a fresh token.
3. Connect and write the login line `VESCTOOL:<lobby ID>:<password>\n`.
4. Write the lobby request as its own write, never in the same write as the login line, and repeat it every 100 ms for about 500 ms. Then close.
5. `PING` the viewer's own ID every 250 ms for 2 s.
6. On `PONG`, attach with `VESCTOOL:<viewer ID>:<password>\n` and start the heartbeat. On no `PONG`, wait and visit again.

The repeats in step 4 ride out the short windows in which the hub discards bytes. The visit is kept short because the lobby holds one viewer at a time: two viewers can displace each other only there, and the retry covers the one that lost.

**Waiting between visits:** a random 1 to 2 s, so that viewers returning together do not displace each other round after round. After a minute with no `PONG` from the lobby, every 5 s. A `PONG` from the lobby restores the 1 to 2 s pace. There is no give-up.

**Attached:**

- The viewer sends a heartbeat once a second, as its own write. A heartbeat lost to the hub is covered by the next.
- The first status message makes the viewer live, board unreachable or version mismatch, by its two bytes.
- After 3 s without a status message the state is phone lost: the viewer closes its socket and visits the lobby with a fresh token. It does not `PING` its old registration, which the hub may answer `PONG` for long after the bridge is gone.
- If the hub closes the viewer's socket, the viewer visits the lobby with a fresh token.
- A viewer that is watching cannot be kicked by another laptop, because no other laptop knows its ID.

**Leaving:** a viewer leaves by closing its socket. The bridge closes the registration 10 s after the last heartbeat. Starting a direct link makes the desktop app leave.

**A code from the wrong phone** is an unknown ID to the hub, so the viewer sees phone not found and cannot tell it from the phone not running.

Tickets: [How several pit laptops watch at once](issues/04-how-several-pit-laptops-watch-at-once.md), [What travels on the stream](issues/05-what-travels-on-the-stream.md), [Desktop app architecture](issues/11-desktop-app-architecture.md), [Stale re-registration on the public hub](issues/13-stale-re-registration-on-the-public-hub.md).

### 2.10 Host, port and the self-hosted hub

- **Setting:** both apps have a host and a port, pre-filled with `veschub.vedder.se` and `65101`. The team code carries neither.
- **Self-hosted hub:** a stock hub on a small rented Linux server with a public address, started with `vesc_tool --tcpHub 65101 --offscreen`. The port is kept at 65101 so only the host differs. No custom server code. Whether the released Linux build of VESC Tool starts on a bare server without extra libraries is not established; the desk checklist item on the front page finds out.
- **No standing hub:** nobody keeps one running, and there is no script or recipe. It is proven once at a desk and then destroyed. On race day it is set up from scratch by the software side of the team. A laptop in the pit cannot host it: the phone is on mobile data and the laptops share no network.
- **Switching:** a person on the software side decides, starts the server and tells the pit crew. There is no automatic failover and no written rule for when to switch. Host and port are typed by hand on the phone and on each laptop. The team code does not change, so every ID and the password stay the same.
- **What an outage costs:** live viewing only. The log of record is written on the phone either way.

When the phone allows the change and where each app shows a non-default host are in the Android and desktop specs. The desk test is on the front page.

Tickets: [Self-run hub fallback](issues/17-self-run-hub-fallback.md), [How hub credentials are set and shared](issues/10-how-hub-credentials-are-set-and-shared.md).

## 3. States and messages shown to the user

This spec shows nothing to a user. It names the states a viewer can be in on the hub; the desktop spec gives each its Turkish status line and the Android spec its rows.

| State | Condition |
|---|---|
| no team code | no code is stored |
| hub unreachable | a connection to the host and port cannot be made |
| phone not found | `PING` on the lobby ID answers `NULL` |
| joining | the lobby answered `PONG` and the viewer is not yet receiving status |
| live | status arriving, same major version, board state 0 |
| board unreachable | status arriving, same major version, board state 1 |
| version mismatch | status arriving, different major version |
| phone lost | attached, and no status for 3 s |

Board states, in the status message: **answering** (0) and **unreachable** (1).

## 4. Required automated tests

The suites themselves are named in the Android and desktop specs. This spec fixes the data they share.

### 4.1 Vectors (moves)

`protocol/vectors.json` holds two groups. Both the Kotlin and the TypeScript suite read the file and run every case. The list below names the cases; the hex and the expected values are computed during implementation.

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

The team-code group was added while writing this spec; the file was first decided as frames only.

Tickets: [Repo layout and cutover](issues/12-repo-layout-and-cutover.md), [Testing without the car](issues/16-testing-without-the-car.md), [Where the specs live and how they are split](issues/18-where-the-specs-live-and-how-they-are-split.md).

### 4.2 Behaviour each side must test

Named here so the two part specs cover the whole protocol between them; each spec says how.

- **Bridge (Android spec):** a lobby request opens a registration; a repeated request for the same token is a no-op; a malformed request is ignored; nothing is written before the first heartbeat; writing stops at 3 s and the registration closes at 10 s without a heartbeat; status is sent on the first heartbeat and on a board state change.
- **Viewer (desktop spec):** the lobby visit, step by step; the pacing between visits and its change after a minute; the heartbeat; every state in section 3 and the moves between them, version mismatch included; phone lost returning to the lobby with a fresh token and no `PING` of the old ID.

No automated test opens a socket to a hub. Desk tests use the public hub.

## 5. Out of scope

- Any custom relay or server code, and any change to the hub.
- Desktop VESC Tool, or VESC Tool mobile, attaching to the bridge's registrations. Compatibility with VESC Tool is not a goal.
- Forwarding anything a viewer sends to the board. The dashboard is read-only.
- Encryption or stronger access control than the team code.
- A standing self-hosted hub, automatic failover between hubs, and a stored backup host.
- A cap on viewers, or any promise above 8.
- Polling by viewers, and any board command beyond the three in 2.3.
