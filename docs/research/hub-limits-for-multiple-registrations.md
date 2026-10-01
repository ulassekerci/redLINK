# Hub limits for multiple registrations

Research for ticket `.scratch/hub-migration/issues/01-hub-limits-for-multiple-registrations.md`.
Date: 2026-10-01.

## Question

Can one phone hold several simultaneous `VESC:<id>:<pass>` registrations on the VESC TCP Hub (one socket per ID), and what limits apply?

## Short answer

Yes. The hub source has no cap of any kind, and a live test held 8 registrations from one host on the public hub for 5 idle minutes and bridged all 8 at once. The things to design around are not caps but three behaviours of the hub code:

1. Bytes sent by a registered VESC side with no client attached are kept in an unbounded buffer and never discarded; they are delivered to the next client.
2. The hub has no idle timeout and no application keep-alive. A registration whose phone vanished without closing TCP can stay "online" for a long time.
3. The handshake uses nested event loops, so a registration can be rejected by the hub through no fault of its own, and re-registering an ID that is still registered has a race that can leave the new socket unregistered. The phone must verify each registration with `PING` and retry.

Nothing published states terms, capacity or uptime for `veschub.vedder.se`. No VESC source shows more than one hub client per board; the hub actively kicks the previous client.

## Sources

| Repo | Commit read | Commit date |
|---|---|---|
| [vedderb/vesc_tool](https://github.com/vedderb/vesc_tool) | `dc53c658cbb89a947246034f7a00149cf79abdfc` | 2026-09-04 |
| [vedderb/vesc_express](https://github.com/vedderb/vesc_express) | `c085911d8f74a6d1f52f69c17bc035e00e060201` | 2026-09-17 |
| [vedderb/bldc](https://github.com/vedderb/bldc) | `4fd8279ea45a17c0d69357438ae2f7237a32514f` | 2026-09-17 |

`vesc_tool` is the same commit `tcp-hub.md` was written from. File and line references below are to these commits, e.g.
`https://github.com/vedderb/vesc_tool/blob/dc53c658cbb89a947246034f7a00149cf79abdfc/tcphub.cpp#L81-L182`.

Confidence labels used below:

- **Source**: read directly in the code cited.
- **Qt**: follows from documented Qt behaviour that I did not re-read in Qt's own source ([QAbstractSocket](https://doc.qt.io/qt-5/qabstractsocket.html), [QTcpServer](https://doc.qt.io/qt-5/qtcpserver.html), [QEventLoop](https://doc.qt.io/qt-5/qeventloop.html)).
- **Tested**: observed against the public hub on 2026-10-01 (see "Live test").
- **Inferred**: my reading of how the code behaves; not tested.
- **Not established**: could not be determined.

A caveat that applies to everything marked Source: the code read is the repository's hub. Whether the public server runs exactly this commit, and what sits in front of it (firewall, connection limits, OS settings), is **not established**. The live test is the only evidence about the deployed server.

## 1. Caps and rate limiting

**No cap per IP, per process or in total, and no rate limiting, exists in the hub code.** (Source)

- The whole server is `TcpHub::newTcpHubConnection()`, `tcphub.cpp:81-182`. It never reads the peer address, keeps no counters and has no timers other than the 5 s handshake wait (`tcphub.cpp:88`).
- Registrations live in `QMap<QString, TcpConnectedVesc*> mConnectedVescs`, keyed only by the upper-cased ID (`tcphub.h:84`, `tcphub.cpp:100`, `tcphub.cpp:118`). Nothing limits its size.
- The server is started with a bare `listen(addr, port)` (`tcphub.cpp:44-47`), launched from `main.cpp:1580-1591` behind the `--tcpHub <port>` flag (`main.cpp:474-480`). No call to `setMaxPendingConnections`, `setReadBufferSize` or similar exists anywhere in the top-level sources (grep returned nothing).
- Each ID is an independent entry with its own password, VESC socket and at most one tool socket (`tcphub.h:43-65`). Several IDs registered from one host are indistinguishable, to the hub, from several boards.

Limits that come from outside the hub code:

- `QTcpServer` queues at most 30 not-yet-accepted connections by default (Qt). The hub takes each connection off the queue as soon as `newConnection` fires (`tcphub.cpp:83`), so this only matters for a burst far larger than 3-8.
- The practical total is the hub process's file-descriptor limit and memory. Their values on the public server are **not established**.
- One client per ID: a second `VESCTOOL` login for an ID closes the first (`tcphub.cpp:134-138`). This is the reason one registration per viewer is needed at all.
- One registration per ID: a second `VESC` login for an ID closes the first (`tcphub.cpp:111-113`). The IDs the phone uses must therefore be distinct, and unguessable enough not to collide with strangers; the VESC Express config text gives the same advice ("Longer strings decrease the probability of id collisions on the HUB", `vesc_express/main/config/settings.xml:231`).

Tested: 8 registrations opened from one host in 1.5 s were all accepted and all answered `PONG`.

### Handshake hazards that matter when opening several sockets

These are not limits, but they decide how the phone must register.

**A registration can be rejected because of somebody else's connection.** (Source for the code; Qt + Inferred for the consequence)
The handshake line is read by `Utility::waitForLine`, which spins a nested `QEventLoop` with a 5 s timer and returns an empty string if that timer is no longer active when the loop returns (`utility.cpp:2379-2415`). It is called from inside the `newConnection` slot (`tcphub.cpp:88`). While it waits, further connections are handled re-entrantly, each in a deeper nested loop. Nested loops unwind last-in-first-out, so connection A's wait cannot return until every later connection's wait has returned. If a later connection sends nothing (a port scanner, a stalled mobile link), it holds A for its own 5 s, A's timer expires in the meantime, and A is closed with "Waiting for connect string timed out" (`tcphub.cpp:90-95`) even though A's line arrived on time. On a public server this will happen occasionally. In the live test, 8 near-simultaneous registrations all succeeded.

**Do not send anything in the same burst as the login line.** (Source; consequence Inferred)
After seeing `\n`, the read loop in `waitForLine` does not stop: it keeps consuming every byte already available and appends it after a NUL terminator (`utility.cpp:2389-2401`), so those bytes never reach the bridge. Wait a moment after the login line before sending payload. (VESC Express itself sends one extra NUL byte after the line, `comm_wifi.c:300-301`, which is harmless either way.)

**Re-registering an ID that is still registered can lose the new registration.** (Inferred, not tested)
On a duplicate `VESC` login the hub closes the old socket, then inserts the new entry under the same ID (`tcphub.cpp:111-118`). The old socket's `disconnected` handler removes the map entry *by ID* (`tcphub.cpp:120-126`). If Qt emits `disconnected` synchronously inside `close()`, the order is remove-then-insert and all is well. If the old socket still has unsent bytes queued (for example client requests the hub was forwarding to a phone that had gone silent), Qt delays the disconnect until they are written (Qt), the handler runs after the insert, and it removes the **new** entry. The new socket then stays open but unregistered: `PING` returns `NULL` and no client can attach. This is exactly the phone-reconnects-after-a-network-change case.

Consequence for the phone: after every registration, confirm with `PING:<id>:0` on a fresh socket and re-register if the answer is not `PONG`.

## 2. Bytes sent by the VESC side while no client is attached

**They are buffered without bound and never discarded.** (Source + Qt; confirmed by test)

- On a `VESC` login the hub connects only a `disconnected` handler to the socket (`tcphub.cpp:115-129`). No `readyRead` handler exists until a `VESCTOOL` attaches (`tcphub.cpp:146-150`), so nothing reads the socket.
- Qt keeps reading from the OS into the `QTcpSocket`'s internal buffer regardless, and that buffer is unlimited unless `setReadBufferSize` is called (Qt), which the hub never does. So the phone sees no back-pressure and the hub's memory grows by whatever is sent.
- The same happens after a client leaves: the `readyRead` handler only forwards if the tool socket is open (`tcphub.cpp:147`); otherwise the bytes stay in the buffer. The tool socket pointer is not cleared on client disconnect (`tcphub.cpp:152-154`).
- The backlog is delivered in full to the next client, at the first `readyRead` after it attaches, i.e. when the VESC side next sends anything (`tcphub.cpp:146-149`).
- The only thing that frees the backlog without delivering it is the VESC socket closing, which deletes the entry and both sockets (`tcphub.cpp:120-126`, `tcphub.h:50-60`).

Tested: 65 536 bytes sent on a registration with no client, left for 5 minutes; a client then attached and received 0 bytes until the VESC side sent 1 more byte, at which point it received all 65 537.

The mirror direction is also unbounded: the hub forwards with `QTcpSocket::write` (`tcphub.cpp:142`, `tcphub.cpp:148`), whose write buffer has no limit (Qt), so a slow or dead receiver makes the hub queue data rather than push back.

Consequence: a registration with no viewer attached must stay silent. Send telemetry only in response to requests arriving on that socket (as `tcp-hub.md` section 7 already recommends), or a late-joining laptop first receives the whole stale backlog.

## 3. Idle timeouts and keep-alive

**The hub never times out an idle registration or an idle bridged pair.** (Source)

- The only timer in the hub is the 5 s wait for the login line (`tcphub.cpp:88`). After that no timer is associated with either socket.
- The header comment describes "Timeout if no valid data from connected thing" and periodic "IM-ALIVE messages" (`tcphub.h:30-41`), but neither is implemented in `tcphub.cpp`. Treat that comment as intent, not behaviour.
- The hub sets `SO_KEEPALIVE` on every accepted socket (`tcphub.cpp:86`) but sets no interval, so the operating system's defaults apply. Those defaults on the public server are **not established**; the usual Linux default is about two hours before the first probe.

Tested: 8 registrations with no traffic at all were still registered (`PONG`) and still bridgeable after 300 s. Longer idle periods were not tested.

Consequences:

- No application keep-alive is needed to satisfy the hub. One must not be sent on an unattached registration anyway, because it would pile up in the buffer described in section 2.
- A keep-alive is still worth having for the path, not the hub: mobile carrier NATs drop idle TCP flows, and the hub will not notice that a phone has vanished without a FIN for a long time. During that window `PING` keeps answering `PONG` and a laptop can attach to a dead registration. Use TCP-level keep-alive on the phone's sockets, which carries no payload. VESC Express does exactly this: keep-alive on, idle 5 s, interval 5 s, 3 probes (`vesc_express/main/comm_wifi.c:225-241`), applied to its hub socket (`comm_wifi.c:296`).
- Recovery from a dead registration is by re-registering the same ID, which closes the stale socket (`tcphub.cpp:111-113`), subject to the race in section 1. Verify with `PING`.
- If the phone's socket for an ID does close, the hub deletes the entry and closes that ID's viewer (`tcphub.cpp:120-126`, `tcphub.h:50-60`). Other IDs are untouched, since entries share nothing.

## 4. Published terms, capacity or uptime for `veschub.vedder.se`

**None found.** (Not established beyond absence)

- The repositories contain no statement. The hostname appears only as a default value: `vescinterface.cpp:90`, `mobile/TcpHubBox.qml:93` and `:192`, `pages/pageconnection.cpp:754`, `res/qml/Examples/TcpHub.qml:24`, and `CONF_TCP_HUB_URL` in VESC Express (`main/hwconf/trampa/bms_rb/rb_conf_default.h:58`). The Express setting descriptions say only how to fill in ID and password (`main/config/settings.xml:186`, `:231`, `:244`).
- No changelog entry in `vesc_tool/res/CHANGELOG.md`, `vesc_express` or `bldc` mentions the hub. Neither README does.
- Web searches for the hostname returned only the source files, forum threads about connecting, and a video titled "VESC Express TCP Hub" (not reviewed). A vesc-project.com thread ([node/4620](https://vesc-project.com/node/4620)) mentions data being routed via vedder.se but states no policy. `http://veschub.vedder.se/` serves no usable web page (the TLS certificate is for `cloud.vedder.se`).

So the public hub is a courtesy default with no stated guarantee. The hub is trivially self-hostable (`vesc_tool --tcpHub <port>`, `main.cpp:474-480`, `main.cpp:1580-1591`), which is the only way to get known capacity and uptime. Since traffic is unencrypted and the password is the only access control (`tcphub.cpp:133`), that also matters for a race vehicle.

## 5. Established patterns for more than one client on one board

**None through the hub. The only pattern in the firmware is one client per transport.** (Source)

- The hub is strictly one client per ID (`tcphub.cpp:134-138`), and VESC Tool's own "connect as server" bridge holds exactly one hub socket (`tcpserversimple.cpp:45-93`, single `mTcpSocket`). Its local TCP server likewise rejects a second client (`tcpserversimple.cpp:129-149`).
- VESC Express holds exactly one hub registration: a single static `comm_hub` state (`comm_wifi.c:84`), one task (`comm_wifi.c:566-570`), one ID and password in the config (`main/main.h:38-42`), in a reconnect-forever loop (`comm_wifi.c:276-312`). Its local TCP server accepts one client at a time (`listen(listen_sock, 1)` and the listening socket is closed during the session, `comm_wifi.c:243-274`).
- `bldc` (the motor-controller firmware) contains no hub or TCP code at all; a search for hub-related identifiers found nothing.
- What the firmware does support is several clients on *different* interfaces at once (hub, local TCP, BLE, USB, CAN): each interface has its own packet state and passes its own reply function to `commands_process_packet` (`vesc_express/main/comm_wifi.c:348-354`), and direct replies go back through that function (`vesc_express/main/commands.c:216-236`, `bldc/comm/commands.c:197-227`). Unsolicited output goes to whichever interface sent the last command (`send_func = reply_func`, same lines).

So "phone holds N registrations and fans out" has no precedent in the VESC sources; it is our own design. The relevant existing behaviour to copy is the reply routing: answer each request on the socket it arrived on. Note that VESC Tool's bridge does the opposite and copies every board packet to its one hub socket (`vescinterface.cpp:218-225`), which is fine for one client but would multiply traffic by N if copied naively.

## Live test

Run once on 2026-10-01 against `veschub.vedder.se:65101` from a single host, with random throwaway IDs.

| Step | Result |
|---|---|
| Open 8 `VESC:` registrations, distinct IDs, same password | all accepted, 1.5 s total |
| `PING` each after 2 s | 8 x `PONG` |
| Send 65 536 bytes on registration 0, no client attached | accepted |
| Leave all 8 idle for 300 s, then `PING` each | 8 x `PONG`, no socket closed |
| Attach a client to registration 0 | 0 bytes received |
| Send 1 byte from the VESC side of registration 0 | client received 65 537 bytes |
| Attach 7 clients to registrations 1-7 at once, one round trip each | all 7 correct in both directions |
| Close everything, `PING` each | 8 x `NULL` |

Not tested: more than 8 registrations, idle periods beyond 5 minutes, sustained throughput across 8 bridges, behaviour from a cellular network, the re-registration race, and the nested-handshake rejection.

## What this means for the design

- Holding 3-8 registrations from the phone is permitted and worked in practice.
- Register IDs one after another or together, but confirm each with `PING` and retry; treat a closed socket during registration as normal.
- Send nothing after the login line until a request arrives on that socket; never push unsolicited data on a registration.
- Enable TCP keep-alive on each phone socket; do not use an in-band keep-alive.
- After any reconnect, re-register and re-verify every ID.
- The public hub gives no guarantee of any kind; self-hosting is one command if that becomes a concern.
