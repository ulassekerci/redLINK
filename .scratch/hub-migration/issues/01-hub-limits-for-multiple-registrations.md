# Hub limits for multiple registrations

Type: research
Status: resolved

Map: [Hub migration](../map.md)

## Question

Can one phone hold several simultaneous `VESC:<id>:<pass>` registrations on the hub (one socket per ID), and what limits apply?

Read the hub source (`tcphub.cpp` and its callers in vedderb/vesc_tool) and establish:

- Is there any cap on connections or registrations per IP, per process, or in total? Any rate limiting on new connections?
- What happens to bytes a registered VESC side sends while no client is attached: is the buffer bounded, and is it ever discarded?
- Are idle registrations or idle bridged sockets ever timed out? Is a keep-alive needed?
- Does anything published about the public hub (`veschub.vedder.se`) state usage terms, capacity or uptime expectations?
- Do VESC Express or other VESC firmware sources show any established pattern for more than one client watching one board?

## Answer

Yes: one phone can hold several `VESC:<id>:<pass>` registrations, one socket per distinct ID. Read from vedderb/vesc_tool at `dc53c658` and checked live on the public hub, where 8 registrations from one host stayed registered through 5 idle minutes and were all bridged at once.

- **Caps and rate limiting:** none in the hub code (`tcphub.cpp:81-182`, map keyed by ID at `tcphub.h:84`). The public server's file-descriptor and memory ceiling is not established.
- **Handshake hazards:** a registration can be rejected while another, silent connection is mid-handshake (`utility.cpp:2379-2415`). Re-registering an ID that is still registered can leave the new socket unregistered (`tcphub.cpp:111-126`); this is a reading of the code, not tested. The phone should confirm every registration with `PING` and retry.
- **Bytes with no client attached:** buffered without bound and never discarded (`tcphub.cpp:115-129`, `146-150`). In the test, 65,536 bytes sent while unattached all arrived at the next client 5 minutes later. An unattached registration must stay silent.
- **Idle timeouts:** none; the only timer is the 5 s login wait (`tcphub.cpp:88`). The timeout described in the `tcphub.h:30-41` comment is not implemented. Use TCP-level keep-alive on the phone, as VESC Express does (`vesc_express/main/comm_wifi.c:225-241`), never an in-band one. Idle beyond 5 minutes was not tested.
- **Public hub terms, capacity, uptime:** nothing published was found. Self-hosting is `vesc_tool --tcpHub <port>` (`main.cpp:474-480`).
- **Multi-client precedent:** none. The hub kicks the previous client (`tcphub.cpp:134-138`), VESC Tool and VESC Express each hold one hub socket, and `bldc` has no hub code. One registration per viewer would be our own design.

Full findings: branch `research/hub-limits-for-multiple-registrations`, file `docs/research/hub-limits-for-multiple-registrations.md`.
