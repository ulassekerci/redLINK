"""Experiment 3: several VESCTOOL logins hit one registration almost at once,
each sending one packet right after the login line. How many packets arrive?"""
import threading
import time
from hub import *


def trial(n, gap, spread_ms, hold):
    id_, pw = rid(), "pw"
    s = register(id_, pw)
    time.sleep(0.6)
    barrier = threading.Barrier(n)
    socks = []

    def worker(i):
        try:
            c = conn()
            barrier.wait()
            time.sleep(i * spread_ms / 1000 / max(n - 1, 1))
            line = f"VESCTOOL:{id_}:{pw}\n".encode()
            pkt = f"<P{i:02d}>".encode()
            if gap is None:
                c.sendall(line + pkt)
            else:
                c.sendall(line)
                time.sleep(gap)
                c.sendall(pkt)
            if hold:
                socks.append(c)
            else:
                c.close()
        except OSError as e:
            log("   worker", i, "error", e)

    ts = [threading.Thread(target=worker, args=(i,)) for i in range(n)]
    [t.start() for t in ts]
    [t.join() for t in ts]
    got, st = drain(s, 2.0)
    arrived = sorted(set(got.decode(errors="replace").replace(">", "> ").split()))
    s.close()
    [c.close() for c in socks]
    return len(arrived), arrived


for gap in (None, 0.0, 0.05, 0.2, 0.5):
    for n in (1, 2, 4, 8):
        for hold in (False, True):
            res = []
            for _ in range(3):
                k, arr = trial(n, gap, spread_ms=30, hold=hold)
                res.append(k)
            log(f"gap={'same-write' if gap is None else gap}  n={n}  {'stay' if hold else 'send+close'}  arrived per trial: {res} of {n}")
log("done")
