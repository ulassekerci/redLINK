"""Helpers for poking the public VESC TCP hub with throwaway IDs."""
import secrets
import select
import socket
import time

HOST, PORT = "veschub.vedder.se", 65101
T0 = time.time()


def log(*a):
    print(f"[{time.time() - T0:8.2f}]", *a, flush=True)


def rid(prefix="RLT"):
    return (prefix + secrets.token_hex(6)).upper()


def conn():
    s = socket.create_connection((HOST, PORT), timeout=10)
    s.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
    return s


def register(id_, pw):
    s = conn()
    s.sendall(f"VESC:{id_}:{pw}\n".encode())
    return s


def login(id_, pw, payload=b"", gap=None):
    """VESCTOOL login. gap=None: payload in the same write as the login line."""
    s = conn()
    line = f"VESCTOOL:{id_}:{pw}\n".encode()
    if gap is None:
        s.sendall(line + payload)
    else:
        s.sendall(line)
        if payload:
            time.sleep(gap)
            s.sendall(payload)
    return s


def ping(id_):
    try:
        s = conn()
        s.sendall(f"PING:{id_}:0\n".encode())
        s.settimeout(5)
        buf = b""
        while not buf.endswith(b"\n"):
            d = s.recv(64)
            if not d:
                break
            buf += d
        s.close()
        return buf.decode().strip() or "EMPTY"
    except OSError as e:
        return f"ERR({e})"


def silence(s):
    """Make every later packet from this socket die at the first router hop,
    so the hub sees a peer that has gone silent without FIN or RST."""
    s.setsockopt(socket.IPPROTO_IP, socket.IP_TTL, 1)


def drain(s, wait=1.0):
    """Read whatever arrives within `wait` seconds. Returns (bytes, state)."""
    end = time.time() + wait
    buf = b""
    state = "open"
    while True:
        left = end - time.time()
        if left <= 0:
            break
        r, _, _ = select.select([s], [], [], left)
        if not r:
            break
        try:
            d = s.recv(65536)
        except OSError as e:
            state = f"error({e.__class__.__name__})"
            break
        if not d:
            state = "closed-by-hub"
            break
        buf += d
    return buf, state
