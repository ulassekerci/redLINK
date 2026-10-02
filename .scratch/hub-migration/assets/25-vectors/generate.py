"""Writes protocol/vectors.json.

An implementation of the wire format in protocol/README.md that shares no code
with the apps, so the expected values in the file do not come from the code
they test. Run from anywhere: python3 generate.py
"""
import binascii
import json
import struct
from pathlib import Path

OUT = Path(__file__).resolve().parents[4] / "protocol" / "vectors.json"
ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ"
MAX_PAYLOAD = 512


def frame(payload: bytes, long: bool = False) -> bytes:
    crc = struct.pack(">H", binascii.crc_hqx(payload, 0))
    head = b"\x03" + struct.pack(">H", len(payload)) if long else bytes([2, len(payload)])
    return head + payload + crc + b"\x03"


def deframe(data: bytes) -> list[bytes]:
    """Reference scan, used only to check that each deframe case says what it means."""
    out, i = [], 0
    while i < len(data):
        if data[i] == 2 and i + 1 < len(data):
            n, start = data[i + 1], i + 2
        elif data[i] == 3 and i + 2 < len(data):
            n, start = struct.unpack(">H", data[i + 1:i + 3])[0], i + 3
            if n < 256 or n > MAX_PAYLOAD:
                n = 0
        else:
            i += 1
            continue
        end = start + n
        if n and end + 3 <= len(data) and data[end + 2] == 3 \
                and struct.unpack(">H", data[end:end + 2])[0] == binascii.crc_hqx(data[start:end], 0):
            out.append(data[start:end])
            i = end + 3
        else:
            i += 1
    return out


SETUP_FIELDS = [
    ("mosfet_temp_c", "h", 10), ("motor_temp_c", "h", 10), ("motor_current_a", "i", 100),
    ("battery_current_a", "i", 100), ("duty_cycle", "h", 1000), ("erpm", "i", 1),
    ("speed_m_s", "i", 1000), ("battery_voltage_v", "h", 10), ("battery_level", "h", 1000),
    ("charge_used_ah", "i", 10000), ("charge_charged_ah", "i", 10000),
    ("energy_used_wh", "i", 10000), ("energy_charged_wh", "i", 10000),
    ("distance_m", "i", 1000), ("distance_abs_m", "i", 1000), ("position", "i", 1000000),
    ("fault_code", "b", 1), ("board_id", "B", 1), ("board_count", "B", 1),
    ("battery_capacity_wh", "i", 1000), ("odometer_m", "I", 1), ("board_uptime_ms", "I", 1),
]


def setup_reply(raw: dict[str, int], fields: int = len(SETUP_FIELDS)) -> tuple[bytes, dict]:
    payload, message = bytes([47]), {"type": "values_setup"}
    for name, fmt, scale in SETUP_FIELDS[:fields]:
        payload += struct.pack(">" + fmt, raw[name])
        message[name] = raw[name] if scale == 1 else raw[name] / scale
    return payload, message


def gps(lat, lon, alt, speed, heading, accuracy, time_ms) -> tuple[bytes, dict]:
    payload = bytes([36, 1]) + struct.pack(">iiiHHHQ", lat, lon, alt, speed, heading, accuracy, time_ms)
    return payload, {
        "type": "gps", "gps_lat_deg": lat / 1e7, "gps_lon_deg": lon / 1e7, "gps_alt_m": alt / 100,
        "gps_speed_m_s": speed / 100, "gps_heading_deg": heading / 100,
        "gps_accuracy_m": accuracy / 10, "gps_fix_time_utc": time_ms,
    }


def check_character(first7: str) -> str:
    return ALPHABET[sum(ALPHABET.index(c) * w for c, w in zip(first7, range(1, 8))) % 31]


frames = []


def deframe_case(name, reads, payloads):
    assert deframe(b"".join(reads)) == payloads, name
    frames.append({"name": name, "check": "deframe", "reads": [r.hex() for r in reads],
                   "payloads": [p.hex() for p in payloads]})


def message_case(name, check, payload, message):
    assert len(payload) <= 255
    frames.append({"name": name, "check": check, "frame": frame(payload).hex(), "message": message})


short = bytes([36, 3])
long_payload = bytes([36, 200]) + bytes(i % 251 for i in range(298))
other = bytes([47, 1, 2, 3])

deframe_case("frame-short", [frame(short)], [short])
deframe_case("frame-long", [frame(long_payload, long=True)], [long_payload])
deframe_case("frame-long-fits-short", [frame(other, long=True)], [])
too_long = bytes([36, 200]) + bytes(i % 251 for i in range(MAX_PAYLOAD - 1))
deframe_case("frame-long-too-long", [frame(too_long, long=True)], [])
deframe_case("frame-zero-length", [bytes([2, 0, 0, 0, 3])], [])
bad_crc = bytearray(frame(other)); bad_crc[-2] ^= 0x01
deframe_case("frame-bad-crc", [bytes(bad_crc)], [])
bad_stop = bytearray(frame(other)); bad_stop[-1] = 0x04
deframe_case("frame-bad-stop-byte", [bytes(bad_stop)], [])
# The bad frame's CRC and stop byte read as a long frame of 33,424 bytes, so a
# decoder without the length cap would wait for them before finding the frame.
deframe_case("frame-after-bad-crc", [bytes(bad_crc) + frame(short)], [short])
# The garbage ends in a start byte and a length, so a decoder must skip a
# frame-shaped run that fails its check and then find the frame inside it.
deframe_case("frame-garbage-before", [bytes([0xA5, 0x02, 0x01]) + frame(short)], [short])
deframe_case("frame-two-in-one-read", [frame(short) + frame(other)], [short, other])
whole = frame(other)
deframe_case("frame-split-across-reads", [whole[:4], whole[4:]], [other])

message_case("request-values-setup", "encode", bytes([47]), {"type": "values_setup_request"})
message_case("request-decoded-adc", "encode", bytes([32]), {"type": "decoded_adc_request"})

full = dict(
    mosfet_temp_c=365, motor_temp_c=412, motor_current_a=1234, battery_current_a=856,
    duty_cycle=742, erpm=18450, speed_m_s=8333, battery_voltage_v=481, battery_level=873,
    charge_used_ah=12345, charge_charged_ah=678, energy_used_wh=594321, energy_charged_wh=32109,
    distance_m=1234567, distance_abs_m=1300000, position=123456789, fault_code=0, board_id=42,
    board_count=1, battery_capacity_wh=480000, odometer_m=3000000000, board_uptime_ms=3600000,
)
message_case("reply-values-setup", "decode", *setup_reply(full))
negative = dict(full, mosfet_temp_c=-53, motor_temp_c=-121, motor_current_a=-2050,
                battery_current_a=-1525, duty_cycle=-150, erpm=-3200, speed_m_s=-1250,
                distance_m=-2500, fault_code=3)
message_case("reply-values-setup-negative", "decode", *setup_reply(negative))
message_case("reply-values-setup-short", "decode", *setup_reply(full, fields=len(SETUP_FIELDS) - 2))

adc = [512345, 1691000, -250000, 0]
message_case("reply-decoded-adc", "decode", bytes([32]) + struct.pack(">iiii", *adc), {
    "type": "decoded_adc", "adc_level1": adc[0] / 1e6, "adc_voltage1": adc[1] / 1e6,
    "adc_level2": adc[2] / 1e6, "adc_voltage2": adc[3] / 1e6,
})

message_case("request-fw-version", "encode", bytes([0]), {"type": "fw_version_request"})
message_case("reply-fw-version", "decode",
             bytes([0, 6, 6]) + b"60\x00" + bytes(range(0x10, 0x1C)) + bytes([0, 0, 0, 1, 0]),
             {"type": "fw_version", "fw_major": 6, "fw_minor": 6})

fix = (398912345, 328765432, 93850, 1234, 27055, 48, 1790000000123)
message_case("gps", "both", *gps(*fix))
message_case("gps-southern-western", "both", *gps(-338567890, -1512093000, -1250, 0, 0, 0, 1790000001000))
payload, message = gps(*fix)
message_case("gps-trailing-bytes", "decode", payload + bytes([0xDE, 0xAD, 0xBE]), message)
# Accuracy one step past what the field carries: not a fix, so nothing is sent.
_, poor = gps(*fix)
frames.append({"name": "gps-poor-accuracy", "check": "encode", "frame": None,
               "message": poor | {"gps_accuracy_m": 6553.6}})

token = "4HT9WQ2B"
message_case("lobby-request", "both", bytes([36, 2]) + token.encode(), {"type": "lobby_request", "token": token})
message_case("lobby-request-short-token", "decode", bytes([36, 2]) + b"4HT9WQ2", None)
message_case("lobby-request-bad-alphabet", "decode", bytes([36, 2]) + b"4HT9wQ0I", None)
message_case("heartbeat", "both", bytes([36, 3]), {"type": "heartbeat"})
message_case("status-answering", "both", bytes([36, 4, 1, 0]),
             {"type": "status", "protocol_version": 1, "board_state": 0})
message_case("status-unreachable", "both", bytes([36, 4, 1, 1]),
             {"type": "status", "protocol_version": 1, "board_state": 1})
message_case("status-trailing-bytes", "decode", bytes([36, 4, 2, 0, 0x7F, 0x00]),
             {"type": "status", "protocol_version": 2, "board_state": 0})
message_case("custom-unknown-type", "decode", bytes([36, 200, 1, 2, 3]), None)
message_case("unknown-command", "decode", bytes([4]) + bytes(range(20)), None)

team_codes = []


def code_case(name, typed, code=None):
    case = {"name": name, "typed": typed, "valid": code is not None}
    if code:
        assert check_character(code[:7]) == code[7]
        case |= {"code": code, "lobby_id": "REDLINK" + code, "password": code,
                 "token": token, "viewer_id": "REDLINK" + code + token}
    team_codes.append(case)


assert check_character("K7QM3XP") == "C"
assert check_character("K8QM3XP") != "C" and check_character("7KQM3XP") != "C"
last = next(a + "222222" for a in ALPHABET if check_character(a + "222222") == "Z")

code_case("code-valid", "K7QM-3XPC", "K7QM3XPC")
code_case("code-lower-case-and-spaces", " k7qm 3xpc ", "K7QM3XPC")
code_case("code-wrong-check-character", "K7QM-3XPD")
code_case("code-one-wrong-character", "K8QM-3XPC")
code_case("code-neighbours-swapped", "7KQM-3XPC")
code_case("code-bad-alphabet", "K7OM-3XPC")
code_case("code-too-short", "K7QM-3XP")
code_case("code-check-character-last", last[:4] + "-" + last[4:] + "Z", last + "Z")

OUT.write_text(json.dumps({"frames": frames, "team_codes": team_codes}, indent=2) + "\n")
print(f"{len(frames)} frame cases, {len(team_codes)} team-code cases -> {OUT}")
