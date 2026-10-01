# What travels on the stream

Type: grilling
Status: resolved
Blocked by: 01, 03

Map: [Hub migration](../map.md)

## Question

The phone owns polling and desktops listen. What exactly does the phone poll and forward, and how does it avoid streaming into a slot nobody is attached to?

Decide: which commands the phone polls and at what rate (today: `COMM_GET_VALUES` and decoded ADC); the layout of the phone-GPS packet carried as `COMM_CUSTOM_APP_DATA`; the form of the lobby request (carrying the viewer's token) and of the roughly 1 Hz viewer heartbeat decided in [How several pit laptops watch at once](04-how-several-pit-laptops-watch-at-once.md); how the phone tells a viewer that the board is unreachable; how a desktop tells "connected and live" from "attached but silent".

## Answer

The phone polls the board at 20 Hz and copies the replies unchanged to every active registration; everything of our own travels as `COMM_CUSTOM_APP_DATA` (36) with a type byte. Decided by grilling on 2026-10-01, on top of [How several pit laptops watch at once](04-how-several-pit-laptops-watch-at-once.md).

- **Polling:** the phone sends `COMM_GET_VALUES` (4), waits for the reply, then `COMM_GET_DECODED_ADC` (32), waits again, on a 50 ms cycle (20 Hz), as today's app does. Each reply is logged and copied unchanged to every active registration. The selective variant (50) was rejected: it saves about 34 bytes per cycle, but the dropped fields could never reach the log.
- **Missing replies:** the phone gives up on a command after 250 ms and sends the next. After 2 s with no reply at all, or at once on a Bluetooth disconnect, the board counts as unreachable. Polling continues.
- **Envelope:** our own messages are `[36, type, ...]` in both directions, integers big-endian. The phone discards any other frame from a viewer.
  - **1, GPS (phone to viewer):** once per fix at 1 Hz, nothing while there is no fix. 26 bytes after the type: latitude `i32` (degrees x 1e7), longitude `i32` (degrees x 1e7), altitude `i32` (metres x 100), speed `u16` (m/s x 100), heading `u16` (degrees x 100), horizontal accuracy `u16` (metres x 10), fix time `u64` (Unix milliseconds).
  - **2, lobby request (viewer to phone):** the token as ASCII filling the rest of the payload. The phone never writes to the lobby; the viewer learns the outcome from `PING`. A malformed request is ignored.
  - **3, heartbeat (viewer to phone):** no content, once a second.
  - **4, status (phone to viewer):** protocol version `u8`, then board state `u8` (0 answering, 1 unreachable). Sent once a second, at once on a change, and at once on a registration's first heartbeat. The protocol version starts at 1 and is raised whenever these messages change.
- **Empty registrations:** the phone writes nothing to a registration until its first heartbeat, pauses after 3 s without one and closes it at 10 s. The worst backlog on the hub is 3 s of stream.
- **Slow viewer:** a small bounded queue per registration; the oldest samples are dropped for that viewer only. Other viewers and the log are unaffected.
- **Timestamps:** viewers stamp samples on arrival; nothing is added to board frames. Only the GPS message carries a time.
- **What a viewer shows:**
  - Live: status arriving, board state 0.
  - Board unreachable: status arriving, board state 1. GPS and status keep flowing.
  - Phone lost: no status for 3 s. The viewer sends `PING`; on `NULL` it returns to the lobby, on `PONG` it keeps waiting.
- **Cost:** about 101 bytes per cycle, so roughly 2 kB/s per viewer and 58 MB per hour of phone data with 8 viewers.

Handed on: token length and alphabet go to [How hub credentials are set and shared](10-how-hub-credentials-are-set-and-shared.md); how the desktop polls on its direct-Bluetooth path, where no status message exists, goes to [Desktop app architecture](11-desktop-app-architecture.md); which polled fields the log keeps goes to [What the phone log contains](08-what-the-phone-log-contains.md).
