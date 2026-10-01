"""Experiment 8: like 7 (old registration silent with a small backlog, then
re-register), but check every 60 s that the new registration still delivers."""
import time
from hub import *

id_, pw = rid(), "pw"
s1 = register(id_, pw)
time.sleep(0.6)
silence(s1)
c_old = login(id_, pw, b"OLDREQ", gap=0.3)
for _ in range(30):
    c_old.sendall(b"HEARTBEAT-0123456789"); time.sleep(0.3)
s2 = register(id_, pw)
t0 = time.time()
time.sleep(1)
log("id", id_, "ping after re-register:", ping(id_))
n = 0
while time.time() - t0 < 1260:
    time.sleep(60)
    n += 1
    r = ping(id_)
    c = login(id_, pw, f"<REQ{n}>".encode(), gap=0.3)
    g, st = drain(s2, 3)
    s2.sendall(b"ok") if st == "open" else None
    gc, stc = drain(c, 2)
    c.close()
    log(f"+{time.time() - t0:.0f}s ping={r} S2 got {g!r} ({st}); client got {gc!r} ({stc})")
log("done")
