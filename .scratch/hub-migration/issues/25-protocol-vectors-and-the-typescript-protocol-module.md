# 25: Protocol vectors and the TypeScript protocol module

**What to build:** the shared test cases and the first implementation that passes them. `protocol/vectors.json` gets every case the wire format's vector list names, with its hex and expected values computed. The desktop app gets one plain TypeScript protocol module (framing with a buffering decoder, the CRC, the request encoders, the reply parsers, our four messages and the team code functions) with no Electron, Node or DOM imports. Running the desktop test command passes two suites that read the vectors file.

Nothing in the app uses the module yet; later tickets wire it in.

Spec: [hub usage spec](../spec-hub.md) 2.2 to 2.4, 2.7 and 4.1 (now in `protocol/README.md`); [desktop app spec](../spec-desktop.md) 2.4 and section 4.

**Blocked by:** 24 (Cut over the repo).

**Status:** ready-for-agent

- [x] `protocol/vectors.json` holds every frame case and every team-code case the vector list names, each with its input and its expected result (decoded fields, rejected, or ignored)
- [x] The file's shape is documented in `protocol/README.md` well enough for the Kotlin suite to read it without looking at the TypeScript code
- [x] The protocol module decodes short and long frames from a byte stream, buffering across reads and skipping one byte on a bad start byte, CRC or stop byte
- [x] It encodes the three board requests and parses the setup, ADC and firmware version replies, treating a short setup reply as missing its tail
- [x] It encodes and decodes GPS, lobby request, heartbeat and status, ignoring unknown types and trailing bytes
- [x] It normalises and validates a typed team code, and derives the lobby ID, viewer ID and password; it generates a token
- [x] The module imports nothing from Electron, Node or the DOM
- [x] Vitest is set up and `pnpm test` runs it
- [x] The `protocol-vectors` suite runs every frame case from the file and passes
- [x] The `team-code` suite runs every team-code case from the file, checks that a generated token is 8 characters of the alphabet, and passes

## Comments

Implemented in `desktop/src/protocol/` with suites `protocol-vectors.test.ts` and `team-code.test.ts`. The vectors were computed by an independent Python implementation, `../assets/25-vectors/generate.py`, so the expected values don't come from the code under test. `tsconfig.protocol.json` (ES2022 lib, no ambient types) keeps the DOM out of the module; an ESLint `no-restricted-imports` rule keeps Electron and Node out.

Open points from review, not decided here:

- **Long-frame stall after corruption.** The stop byte `0x03` is also the long start byte, so after a bad frame the one-byte skip can land on a "long frame" with a length up to 65,535 and wait for that many bytes before rescanning. Nothing is lost, but the stream can stall: up to ~16 s at 2 kB/s on the hub. Capping the accepted long length (VESC firmware caps packets at 512 bytes) would bound it, but the spec says a decoder accepts up to 65,535.
- **Lobby request with bytes after the token.** The decoder reads the first 8 bytes and ignores the rest, by the tolerance rule. The README's "a request whose token is anything else" could also be read as rejecting it. No vector pins this down; the Kotlin side should match.
- **GPS encoding range.** Speed, heading and accuracy are u16 on the wire. The encoder doesn't clamp, so a value outside the range wraps. Only the Kotlin bridge encodes GPS for real.
- `desktop/src/utils/crc.ts` (the old table CRC, also exporting `crc16`) should go with the legacy Bluetooth code in ticket 27.
