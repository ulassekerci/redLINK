"""Experiment 6: the delayed-disconnect race. Old registration is silent and has
a large unsent backlog on the hub; re-register and watch whether the NEW
registration later disappears while its socket stays open."""
import time
from hub import *

id_, pw = rid(), "pw"
s1 = register(id_, pw)
time.sleep(0.6)
silence(s1)
c_old = login(id_, pw, b"OLDREQ", gap=0.3)
for _ in range(16):
    c_old.sendall(b"x" * 65536)
time.sleep(1)
s2 = register(id_, pw)
t0 = time.time()
time.sleep(1)
log("id", id_, "ping after re-register:", ping(id_))
last = "PONG"
while time.time() - t0 < 1500:
    time.sleep(10)
    r = ping(id_)
    if r != last:
        log(f"ping changed {last} -> {r} at +{time.time() - t0:.0f}s")
        last = r
        break
g, st = drain(s2, 0.5)
log(f"S2 socket state: {st}")
c = login(id_, pw, b"NEWREQ", gap=0.3)
g, st = drain(s2, 1.5)
gc, stc = drain(c, 0.5)
log(f"after change: S2 got {g!r} ({st}); new client {stc}")
s3 = register(id_, pw)
time.sleep(1)
log("third registration ping:", ping(id_))
g, st = drain(s2, 0.5)
log(f"S2 after third registration: {st}")
log("done")
