# 26: Electron shell around the dashboard

**What to build:** the dashboard becomes a desktop app. Starting the dev command opens one Electron window showing today's gauges, bottom section and map with everything reading zero. The old data paths to the relay are removed; no new data path exists yet.

Spec: [desktop app spec](../spec-desktop.md) 2.1, 2.9 ("Dropped from `web/`"), 2.11 (version stamping only) and 2.12 (stack).

**Blocked by:** 24 (Cut over the repo).

**Status:** ready-for-agent

- [ ] The app runs under Electron with `electron-vite`; `pnpm dev` opens one window
- [ ] Closing the window quits the app on macOS as on Windows
- [ ] There is no single-instance lock: starting the app twice gives two independent windows
- [ ] The renderer runs with context isolation and without Node integration, and a preload script is in place for later tickets to extend
- [ ] `BrowserRouter` is replaced by `HashRouter`, with routes for the gauges, the map and settings (settings may be an empty screen)
- [ ] The Inter font is bundled, not loaded from the network
- [ ] `socket.io-client`, the socket service and store, the Bluetooth/socket source switch and the socket URL setting are removed; the settings entry takes the switch's place in the bottom section
- [ ] The app's version is stamped at build time from the latest version tag in git with `-dev` added, `0.0.0-dev` with no tag, and is readable from main and through the preload API
- [ ] The gauges, bottom section and map render, reading zero
- [ ] `pnpm test` still passes
