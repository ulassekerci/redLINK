# 26: Electron shell around the dashboard

**What to build:** the dashboard becomes a desktop app. Starting the dev command opens one Electron window showing today's gauges, bottom section and map with everything reading zero. The old data paths to the relay are removed; no new data path exists yet.

Spec: [desktop app spec](../spec-desktop.md) 2.1, 2.9 ("Dropped from `web/`"), 2.11 (version stamping only) and 2.12 (stack).

**Blocked by:** 24 (Cut over the repo).

**Status:** ready-for-agent

- [x] The app runs under Electron with `electron-vite`; `pnpm dev` opens one window
- [x] Closing the window quits the app on macOS as on Windows
- [x] There is no single-instance lock: starting the app twice gives two independent windows
- [x] The renderer runs with context isolation and without Node integration, and a preload script is in place for later tickets to extend
- [x] `BrowserRouter` is replaced by `HashRouter`, with routes for the gauges, the map and settings (settings may be an empty screen)
- [x] The Inter font is bundled, not loaded from the network
- [x] `socket.io-client`, the socket service and store, the Bluetooth/socket source switch and the socket URL setting are removed; the settings entry takes the switch's place in the bottom section
- [x] The app's version is stamped at build time from the latest version tag in git with `-dev` added, `0.0.0-dev` with no tag, and is readable from main and through the preload API
- [x] The gauges, bottom section and map render, reading zero
- [x] `pnpm test` still passes

## Comments

The renderer moved to `desktop/src/renderer/` (with `git mv`), beside `src/main/`, `src/preload/` and the shared `src/protocol/`, which is electron-vite's default layout. The preload API is typed in `src/preload/api.ts` and exposed as `window.redlink`; for now it only has `version()`. Settings is a stub with the title, `Geri` and `Sürüm …`, which ticket 29 fills in.

Found on the first build:

- **Electron 44 has no install script.** It downloads its binary the first time `require('electron')` runs. electron-vite 5 only reads `node_modules/electron/path.txt` and never triggers that download, so `pnpm dev` failed on a fresh install. `desktop/package.json` now has `"postinstall": "install-electron"`, which runs Electron's own installer. pnpm still lists Electron as having a build to approve, so `electron: true` stays in `allowBuilds` in `pnpm-workspace.yaml`, which is where this package keeps the setting (the spec said `package.json`).
- **electron-vite 5 has no entry for Electron 44** in its Node and Chrome target tables. It falls back to its newest entry (Electron 39), which only makes the output a little more conservative. electron-vite 6 is in beta.
- **The sandboxed preload must be CommonJS**, but the package is `"type": "module"`, so electron-vite would emit ESM. The config forces `out/preload/index.cjs`.
- **The map style is imported as JSON** instead of fetched from `/mapstyle.json`, because under `file://` that absolute path points at the disk root. `public/` is gone with it; its other file was the old favicon.
- `.npmrc` with `node-linker=hoisted` is for electron-builder and is left to ticket 32.

The version is `localVersion(latestVersionTag())` in `scripts/version.ts`: the nearest `v*` tag, with any suffix dropped, plus `-dev`, or `0.0.0-dev` (the repo has only `legacy-stack` today). Ticket 46 needs to override it with the tag being released. `package.json` still says `0.0.0`, so `app.getVersion()` disagrees with `appVersion` in `src/main/version.ts`. Main code should read `appVersion`, and ticket 46 should stamp `package.json` too, because electron-builder reads it.

Decided: release-candidate tags count as version tags. After `v2.0.0-rc1` a local build is `2.0.0-dev`, protocol version 2, which is what testing against the candidate's phone build needs.

Checked by hand on macOS: `pnpm dev` opens one window; two instances of the built app run side by side and closing one window quits only that instance; the renderer has no `require` or `process`; Inter is loaded from the bundle with no network link; the gauges, the map ("Konum verisi yok") and settings render. Not checked: Windows, and the map with a fix, since nothing produces one yet.
