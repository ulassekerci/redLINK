# 25: Protocol vectors and the TypeScript protocol module

**What to build:** the shared test cases and the first implementation that passes them. `protocol/vectors.json` gets every case the wire format's vector list names, with its hex and expected values computed. The desktop app gets one plain TypeScript protocol module (framing with a buffering decoder, the CRC, the request encoders, the reply parsers, our four messages and the team code functions) with no Electron, Node or DOM imports. Running the desktop test command passes two suites that read the vectors file.

Nothing in the app uses the module yet; later tickets wire it in.

Spec: [hub usage spec](../spec-hub.md) 2.2 to 2.4, 2.7 and 4.1 (now in `protocol/README.md`); [desktop app spec](../spec-desktop.md) 2.4 and section 4.

**Blocked by:** 24 (Cut over the repo).

**Status:** ready-for-agent

- [ ] `protocol/vectors.json` holds every frame case and every team-code case the vector list names, each with its input and its expected result (decoded fields, rejected, or ignored)
- [ ] The file's shape is documented in `protocol/README.md` well enough for the Kotlin suite to read it without looking at the TypeScript code
- [ ] The protocol module decodes short and long frames from a byte stream, buffering across reads and skipping one byte on a bad start byte, CRC or stop byte
- [ ] It encodes the three board requests and parses the setup, ADC and firmware version replies, treating a short setup reply as missing its tail
- [ ] It encodes and decodes GPS, lobby request, heartbeat and status, ignoring unknown types and trailing bytes
- [ ] It normalises and validates a typed team code, and derives the lobby ID, viewer ID and password; it generates a token
- [ ] The module imports nothing from Electron, Node or the DOM
- [ ] Vitest is set up and `pnpm test` runs it
- [ ] The `protocol-vectors` suite runs every frame case from the file and passes
- [ ] The `team-code` suite runs every team-code case from the file, checks that a generated token is 8 characters of the alphabet, and passes
