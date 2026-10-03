# 32: Desktop packaging

**What to build:** a local build of the desktop app produces the two packages the pit crew will download: a `dmg` for Apple-silicon Macs and a zip for 64-bit Windows that is unzipped anywhere and run in place. Neither is signed with a paid certificate, so `README.md` tells the crew how to open them the first time.

The release workflow that builds these from a tag is ticket 46.

Spec: [desktop app spec](../spec-desktop.md) 2.12; [front page](../spec.md) section 4 ("Local builds").

**Blocked by:** 26 (Electron shell around the dashboard).

**Status:** resolved

- [x] electron-builder is configured; the product name is `redLINK` and the bundle ID is `org.metucet.redlink`
- [x] The macOS target is arm64 only, a `dmg`, ad-hoc signed and not notarized
- [x] The Windows target is x64 only, a zip run in place: no installer and not the single-file portable target
- [x] The packaged app carries the version stamped at build time
- [x] The packaged app loads its window from a file and its routes work
- [x] The two pnpm settings the spec wrote from memory (Electron's install script allowed, `node-linker=hoisted`) are checked on a real build and corrected if wrong
- [x] The macOS package is built and opened on this machine; the Windows package's configuration is in place and its first real build is left to the release workflow if it cannot be built here
- [x] `README.md` has the first-open steps: "Open Anyway" in macOS System Settings and "Run anyway" on Windows SmartScreen

## Comments

`pnpm package` builds the app and packages it for the system it runs on, into `desktop/dist/`: `redLINK-<version>-mac-arm64.dmg` on a Mac, `redLINK-<version>-win-x64.zip` on Windows. The configuration is `desktop/electron-builder.ts`, which replaces the `electron-builder.yml` stub so that it can read the version.

Found on the first packaged build:

- **Neither pnpm setting was needed.** Electron 44 has no install script, so there is nothing to allow: a fresh install with an empty pnpm store, without `electron: true` in `allowBuilds`, reports no ignored build and has the binary, which the package's own `postinstall` fetches. The entry is removed. An existing `node_modules` that was installed with the entry keeps a stale record and fails with `ERR_PNPM_IGNORED_BUILDS` until it is deleted and installed again. And packaging ran on pnpm's default linker: electron-vite bundles every dependency into `out/`, so the package's `app.asar` holds `out/` and `package.json` and no `node_modules`. No `.npmrc` was added. The spec's 2.12 now says both.
- **electron-builder brings `electron-winstaller`,** whose build script pnpm asks about. It is for the Squirrel installer, which is not used, so it is set to `false` in `allowBuilds`.
- **The hardened runtime is off.** Only notarization needs it, and under it macOS gives an app Bluetooth only with the `com.apple.security.device.bluetooth` entitlement, which the direct link would then need. This is from Apple's documentation, not from a run with it on.
- **The package's version** comes from `buildVersion` in `scripts/version.ts`, which `electron.vite.config.ts` now calls too, and reaches electron-builder as `extraMetadata.version`. `package.json` keeps its placeholder `0.0.0`, and in a packaged app `app.getVersion()` agrees with `appVersion`. Ticket 46 overrides the version in that one function.
- **The Windows zip builds on a Mac** (`pnpm package --win`), with the executable's name and version resources written.

Checked on this Mac (arm64): the `dmg` mounts and holds `redLINK.app` beside the Applications link; the app is arm64 only, `Signature=adhoc`, passes `codesign --verify --deep --strict`, and its `Info.plist` has the bundle ID, `0.0.0-dev` and the Bluetooth text. Started from the mounted `dmg` with an empty settings folder, it opened on `index.html#/settings` from a `file://` URL inside `app.asar`, showed `Sürüm 0.0.0-dev`, and `#/`, `#/map` and `#/settings` each rendered their screen. `buildVersion` was also run against a clone tagged `v1.2.0-rc1` and gave `1.2.0-dev`. The Windows zip was built and inspected (an x64 `redLINK.exe` with `FileVersion 0.0.0-dev`, files at the zip's root) but not run: its first run is on the release candidate, ticket 47.

Not checked: opening a downloaded copy, which is what makes macOS and Windows ask. The README's first-open steps are written from how the two systems behave, not from a run.

Added after the first build, on the maintainer's word:

- **Icon.** The icons of the old React Native app, taken from the `legacy-stack` tag, are in `desktop/build/`. macOS uses `icon.icon`, the Icon Composer file that was the iOS icon, which electron-builder compiles with Xcode's `actool`; the macOS runner in ticket 46 therefore needs Xcode 26 or newer. Windows uses `icon.png`, which electron-builder turns into an `ico`. Both were looked at in the rebuilt packages.
- **Author.** `package.json` names `METU CET`, so the Windows executable's company name and both packages' copyright line read `METU CET` and not Electron's default.

Left open:

- A packaged app keeps its settings under the product name (`~/Library/Application Support/redLINK`), not where `pnpm dev` keeps them, so a team code typed in one is not seen by the other.
