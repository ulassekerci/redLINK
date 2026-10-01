"""Experiment 5: realistic lobby requests (connect, login line, gap, packet, close)
from n viewers started `stagger` apart; and the effect of a stranger's silent connection."""
import threading
import time
from hub import *


def trial(n, stagger, gap, repeats=1, every=0.1):
    id_, pw = rid(), "pw"
    s = register(id_, pw)
    time.sleep(0.6)

    def worker(i):
        try:
            c = conn()
            c.sendall(f"VESCTOOL:{id_}:{pw}\n".encode())
            time.sleep(gap)
            for r in range(repeats):
                c.sendall(f"<P{i}>".encode())
                if r < repeats - 1:
                    time.sleep(every)
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
    txt = got.decode(errors="replace")
    return "".join(sorted(set(str(i) for i in range(n) if f"<P{i}>" in txt))) or "-"


for gap in (0.05, 0.15):
    for stagger in (0.0, 0.02, 0.05, 0.1, 0.2, 0.4):
        res = [trial(4, stagger, gap) for _ in range(5)]
        log(f"gap={gap * 1000:.0f} ms stagger={stagger * 1000:.0f} ms n=4: viewers whose request arrived, per trial: {res}")
for stagger in (0.0, 0.05):
    res = [trial(8, stagger, 0.05) for _ in range(5)]
    log(f"gap=50 ms stagger={stagger * 1000:.0f} ms n=8: {res}")

# A stranger's silent connection, opened just after ours, holds our login.
for hold_s in (1.0, 3.0, 6.0):
    id_, pw = rid(), "pw"
    s = register(id_, pw)
    time.sleep(0.6)
    c = conn()
    stranger = conn()          # connects after us, sends nothing
    t0 = time.time()
    c.sendall(f"VESCTOOL:{id_}:{pw}\n".encode())
    arrivals = []
    for k in range(int(hold_s * 10) + 20):
        try:
            c.sendall(f"<{k}>".encode())
        except OSError as e:
            arrivals.append(f"client send failed at {time.time() - t0:.1f}s")
            break
        time.sleep(0.1)
        if abs(time.time() - t0 - hold_s) < 0.05:
            stranger.sendall(b"PING:XXX:0\n")
    got, st = drain(s, 1.0)
    cg, cst = drain(c, 0.2)
    log(f"stranger silent for {hold_s}s: packets sent every 100 ms; first to arrive: {got[:12]!r}, total bytes {len(got)}; client socket {cst} {arrivals}")
    for x in (s, c, stranger):
        x.close()
log("done")
