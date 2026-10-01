"""Experiment 1: re-register an ID whose old socket is still registered."""
import sys
import time
from hub import *


def sanity_silence():
    """Check the TTL trick: after silence(), bytes from the VESC side must not reach a client."""
    id_, pw = rid(), "pw"
    s1 = register(id_, pw)
    time.sleep(0.5)
    c = login(id_, pw)
    time.sleep(0.5)
    s1.sendall(b"before")
    got, _ = drain(c, 1.5)
    silence(s1)
    s1.sendall(b"after")
    got2, st = drain(c, 4)
    log(f"SANITY before-silence client got {got!r}; after-silence client got {got2!r} ({st})")
    c.close()
    return s1  # keep open so no RST is produced


def case(name, old_silenced, prior, same_pw=True, flood=0):
    id_, pw = rid(), "pw1"
    log(f"--- {name}: id={id_}")
    s1 = register(id_, pw)
    time.sleep(0.5)
    log("  ping after S1:", ping(id_))
    if old_silenced:
        silence(s1)
        time.sleep(0.3)
    c_old = None
    if prior != "none":
        c_old = login(id_, pw, b"OLDREQ", gap=0.3)
        if flood:
            c_old.settimeout(20)
            sent = 0
            chunk = b"x" * 65536
            try:
                while sent < flood:
                    c_old.sendall(chunk)
                    sent += len(chunk)
            except OSError as e:
                log(f"  flood stopped at {sent}: {e}")
            log(f"  flooded {sent} bytes into old registration")
        time.sleep(0.7)
        if prior == "wrote-left":
            c_old.close()
            time.sleep(0.3)
    pw2 = pw if same_pw else "pw2"
    s2 = register(id_, pw2)
    t_reg = time.time()
    for d in (0.3, 1, 3):
        time.sleep(d)
        log(f"  ping +{time.time() - t_reg:.1f}s after S2:", ping(id_))
    if not old_silenced:
        got, st = drain(s1, 0.5)
        log(f"  S1 state: {st}, got {got[:20]!r} len={len(got)}")
    if c_old is not None and prior == "attached":
        got, st = drain(c_old, 0.5)
        log(f"  old client state: {st}")
    c = login(id_, pw2, b"NEWREQ", gap=0.4)
    time.sleep(0.2)
    g2, st2 = drain(s2, 1.5)
    log(f"  S2 got {g2!r} ({st2})")
    if not old_silenced:
        g1, st1 = drain(s1, 0.2)
        log(f"  S1 got {g1[:20]!r} ({st1})")
    else:
        g1, st1 = drain(s1, 0.2)
        log(f"  S1(silenced, local view) got {len(g1)} bytes ({st1})")
    try:
        s2.sendall(b"REPLY-FROM-S2")
    except OSError as e:
        log("  S2 send failed", e)
    gc, stc = drain(c, 1.5)
    log(f"  new client got {gc!r} ({stc})")
    if not same_pw:
        c_bad = login(id_, pw, b"OLDPW", gap=0.4)
        gb, stb = drain(c_bad, 1.5)
        log(f"  login with old password: {stb}")
    time.sleep(10)
    log("  ping +~17s:", ping(id_))
    c.close()
    s2.close()
    time.sleep(0.5)
    log("  ping after S2 closed:", ping(id_))
    return s1, c_old


keep = [sanity_silence()]
keep.append(case("A live-idle old, no client", False, "none"))
keep.append(case("B silenced old, no client", True, "none"))
keep.append(case("C silenced old, client wrote and left", True, "wrote-left"))
keep.append(case("D silenced old, client still attached", True, "attached"))
keep.append(case("E silenced old, different password", True, "none", same_pw=False))
keep.append(case("F live-idle old, client still attached", False, "attached"))
if "flood" in sys.argv:
    keep.append(case("G silenced old, client flooded 1 MiB and still attached", True, "attached", flood=1 << 20))
log("done")
