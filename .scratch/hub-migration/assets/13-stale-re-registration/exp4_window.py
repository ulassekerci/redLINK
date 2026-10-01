"""Experiment 4: (a) smallest safe gap between login line and packet,
(b) how far apart two lobby requests must be for both to arrive."""
import threading
import time
from hub import *

t = time.time(); c = conn(); rtt = (time.time() - t) * 1000; c.close()
log(f"TCP connect time to hub: {rtt:.0f} ms")

# (a) single client, varying gap
id_, pw = rid(), "pw"
s = register(id_, pw)
time.sleep(0.6)
for gap in (0.0, 0.01, 0.02, 0.03, 0.05, 0.1, 0.2):
    ok = 0
    N = 10
    for i in range(N):
        c = login(id_, pw, b"<P>", gap=gap)
        c.close()
        got, _ = drain(s, 0.6)
        ok += got.count(b"<P>")
    log(f"(a) gap={gap * 1000:.0f} ms: {ok}/{N} packets arrived")
s.close()


# (b) n clients, logins staggered, each: login, wait gap, packet, close
def trial(n, stagger, gap):
    id_, pw = rid(), "pw"
    s = register(id_, pw)
    time.sleep(0.6)
    conns = [conn() for _ in range(n)]

    def worker(i):
        c = conns[i]
        try:
            c.sendall(f"VESCTOOL:{id_}:{pw}\n".encode())
            time.sleep(gap)
            c.sendall(f"<P{i}>".encode())
            c.close()
        except OSError:
            pass

    ts = []
    for i in range(n):
        th = threading.Thread(target=worker, args=(i,))
        th.start(); ts.append(th)
        time.sleep(stagger)
    [th.join() for th in ts]
    got, _ = drain(s, 1.5)
    s.close()
    return got.decode(errors="replace")


for gap in (0.05, 0.1):
    for stagger in (0.0, 0.02, 0.05, 0.1, 0.15, 0.25, 0.5):
        res = [trial(4, stagger, gap) for _ in range(4)]
        log(f"(b) gap={gap * 1000:.0f} ms stagger={stagger * 1000:.0f} ms n=4: arrived {res}")
log("done")
