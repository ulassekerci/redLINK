# 32: Desktop packaging

**What to build:** a local build of the desktop app produces the two packages the pit crew will download: a `dmg` for Apple-silicon Macs and a zip for 64-bit Windows that is unzipped anywhere and run in place. Neither is signed with a paid certificate, so `README.md` tells the crew how to open them the first time.

The release workflow that builds these from a tag is ticket 46.

Spec: [desktop app spec](../spec-desktop.md) 2.12; [front page](../spec.md) section 4 ("Local builds").

**Blocked by:** 26 (Electron shell around the dashboard).

**Status:** ready-for-agent

- [ ] electron-builder is configured; the product name is `redLINK` and the bundle ID is `org.metucet.redlink`
- [ ] The macOS target is arm64 only, a `dmg`, ad-hoc signed and not notarized
- [ ] The Windows target is x64 only, a zip run in place: no installer and not the single-file portable target
- [ ] The packaged app carries the version stamped at build time
- [ ] The packaged app loads its window from a file and its routes work
- [ ] The two pnpm settings the spec wrote from memory (Electron's install script allowed, `node-linker=hoisted`) are checked on a real build and corrected if wrong
- [ ] The macOS package is built and opened on this machine; the Windows package's configuration is in place and its first real build is left to the release workflow if it cannot be built here
- [ ] `README.md` has the first-open steps: "Open Anyway" in macOS System Settings and "Run anyway" on Windows SmartScreen
