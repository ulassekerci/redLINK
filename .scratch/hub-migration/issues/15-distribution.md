# Distribution

Type: grilling
Status: resolved

Map: [Hub migration](../map.md)

## Question

How do the two apps reach the people who use them?

Decide: how the APK is built and gets onto the phone, and how a newer build replaces it; how the Electron app is built, packaged and signed for macOS and Windows, and how the pit crew's laptops get a newer version; the macOS Bluetooth permission text, handed on from [Desktop app architecture](11-desktop-app-architecture.md); what happens when the phone and a laptop run builds with different protocol versions on race day.

Carry this from [Repo layout and cutover](12-repo-layout-and-cutover.md): `desktop/` uses pnpm. Check what that asks of the Electron build: pnpm does not run dependency install scripts unless they are allowed, and Electron's binary download is one; packagers can also need a hoisted `node_modules` layout.

## Answer

Both apps are built by GitHub Actions from a version tag and fetched by hand from the GitHub Release; nothing is signed with a paid certificate and nothing installs itself. Decided by grilling on 2026-10-02. Nothing here was built or run.

- **Building:** one workflow, run only on `v*` tags, runs both test suites and builds the APK, the macOS app and the Windows app. A failing test produces no release. Pushes to main build nothing. Local builds stay possible. Actions is free here because the repo is public. Building by hand on borrowed machines was rejected: each release would need a Windows laptop set up for it.
- **Release candidates:** a plain tag (`v1.2.0`) publishes a GitHub Release; a tag with a suffix (`v1.2.0-rc1`) publishes a pre-release. Candidates are how a build is tried, and how the workflow itself is fixed, without using up a version number. There is no manual trigger.
- **Version:** one number for the whole repo, taken from the tag alone. The workflow stamps it into both apps and each shows it on its settings screen; a local build shows `0.0.0-dev`. The Android version code is major x 10000 + minor x 100 + patch, so a candidate and its final release share a code; Android allows installing over an equal code.
- **Android:** the APK is sideloaded from the Release, and a newer one installs over the old. The Play Store was rejected: one or two phones, and review of an app asking for "all the time" location. The release keystore is made once and kept as an Actions secret, with a copy the maintainer holds offline; losing it means uninstalling, which makes a new team code. The application ID is `org.metucet.redlink`, the old React Native app's, so that app is uninstalled first. Builds from Android Studio use the suffix `.debug` and the name "redLINK dev", because a debug build cannot install over a release-signed one and uninstalling would discard the phone's team code.
- **Desktop build:** electron-builder with `electron-vite`. Electron Forge was rejected for its Squirrel installer and experimental Vite plugin. For pnpm: allow Electron's install script in `desktop/package.json` and set `node-linker=hoisted` in `desktop/.npmrc`. These two settings are from memory and must be checked on the first build.
- **Desktop packages:** macOS is arm64 only, a `dmg`, ad-hoc signed and not notarized. Windows is x64 only, a zip that is unzipped anywhere and run in place; an installer was not wanted, and the single-file portable target was rejected because it unpacks itself on every launch. The bundle ID is `org.metucet.redlink`. A paid Apple or Windows certificate was rejected: with 3-8 known laptops the first-open warning is a one-time cost. `README.md` carries the first-open steps ("Open Anyway" in macOS System Settings, "Run anyway" on Windows SmartScreen).
- **macOS Bluetooth permission text** (`NSBluetoothAlwaysUsageDescription`): "redLINK, araca doğrudan bağlanmak için Bluetooth kullanır."
- **Updates:** each app asks GitHub for the latest release and, when it is newer, shows one "update available" line that opens the Release page. Pre-releases are ignored and a failed check says nothing. The desktop checks on launch. The phone checks when the app is opened with no run active and shows the line on the Setup screen only, never as a notification. Auto-update was rejected: an unsigned macOS app cannot do it.
- **Mismatched versions:** the protocol version byte in the status message is the app's major version. Within a major, changes are additive only: a viewer ignores message types it does not know and extra bytes at the end of a message it does. When the majors differ, in either direction, the viewer shows nothing from the stream and its status line states both versions, such as "Telefon sürüm 1, bu uygulama sürüm 2", and recommends no action. Older desktop releases stay on the Releases page. Recorded in [ADR 0002](../../../docs/adr/0002-protocol-version-is-the-app-major-version.md).

Amends [What travels on the stream](05-what-travels-on-the-stream.md) (the protocol version was a counter of its own) and [Desktop app architecture](11-desktop-app-architecture.md) (the state was "protocol version newer than this app knows" and told the user to update).

No change to `CONTEXT.md`.

Amended 2026-10-02 by [Testing without the car](16-testing-without-the-car.md): the tag workflow runs both test suites first, and a failing test blocks the release.

Amended 2026-10-02 by [Write the hub usage spec](19-write-the-hub-usage-spec.md): a local build no longer shows `0.0.0-dev`. It takes its version from the latest version tag in git with `-dev` added, such as `1.2.0-dev`, and falls back to `0.0.0-dev` only when there is no tag to read. With the protocol version being the app's major version, a fixed `0.0.0-dev` made every local build a version mismatch against every released build of the other app, which is the usual situation when debugging from source. The cost: a local build with incompatible message changes, made before the next major is tagged, is not refused.
