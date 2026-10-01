"""Experiment 2: how long a silently dead registration keeps answering PONG.
usage: exp2_longevity.py <mode> <max_seconds>   mode = idle | heartbeat | once
"""
import sys
import time
from hub import *

mode, max_s = sys.argv[1], int(sys.argv[2])
id_, pw = rid(), "pw"
s1 = register(id_, pw)
time.sleep(1)
log(mode, "id", id_, "ping:", ping(id_))
silence(s1)
t_dead = time.time()
log("silenced")

c = None
c_state = "none"
if mode in ("heartbeat", "once"):
    c = login(id_, pw, b"HB", gap=0.4)
    c_state = "open"
    if mode == "once":
        time.sleep(0.5)
        c.close()
        c = None
        c_state = "left"

interval = 60 if mode == "idle" else 10
next_ping = time.time() + interval
last = "PONG"
while time.time() - t_dead < max_s:
    time.sleep(1)
    if c is not None:
        try:
            c.sendall(b"HEARTBEAT-0123456789")
            got, st = drain(c, 0)
            if st != "open":
                raise OSError(st)
        except OSError as e:
            log(f"client closed at +{time.time() - t_dead:.0f}s: {e}")
            c = None
    if time.time() >= next_ping:
        next_ping += interval
        r = ping(id_)
        if r != last:
            log(f"ping changed {last} -> {r} at +{time.time() - t_dead:.0f}s")
            last = r
        if r == "NULL":
            break
        if int(time.time() - t_dead) % 600 < interval:
            log(f"still {r} at +{time.time() - t_dead:.0f}s")
g, st = drain(s1, 0.2)
log(f"end: last={last} at +{time.time() - t_dead:.0f}s; S1 local view: {len(g)} bytes, {st}")
