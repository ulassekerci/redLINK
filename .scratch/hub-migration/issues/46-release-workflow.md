# 46: Release workflow

**What to build:** pushing a version tag produces a release. One GitHub Actions workflow, triggered only by `v*` tags, runs both apps' test suites, then builds the release APK, the macOS app and the Windows app, each stamped with the version from the tag, and attaches all three to one GitHub Release. A tag with a suffix publishes a pre-release. A push to main builds nothing and tests nothing.

The release keystore does not exist yet. This ticket writes the workflow to read it from Actions secrets and documents which secrets it expects; creating them and cutting the first candidate is ticket 47.

Spec: [front page](../spec.md) sections 4 and 5; [Android app spec](../spec-android.md) 2.13 and section 4; [desktop app spec](../spec-desktop.md) 2.12 and section 4.

**Blocked by:** 32 (Desktop packaging), 33 (Android project and Kotlin protocol module).

**Status:** resolved

- [x] There is one workflow, triggered on `v*` tags only, with no manual trigger and none on ordinary pushes
- [x] Its first step runs `./gradlew testDebugUnitTest` and the desktop `pnpm test`; a failing test produces no release
- [x] The version comes from the tag alone and is stamped into both apps; no file in the repo holds it
- [x] `v1.2.0-rc1` and `v1.2.0` give the same Android version code
- [x] The APK is a release build signed with the keystore from Actions secrets
- [x] The macOS package is the arm64 `dmg` and the Windows package is the x64 zip
- [x] All three are attached to one GitHub Release for the tag; a plain tag publishes a release and a suffixed tag a pre-release
- [x] The names of the secrets the workflow expects, and how the keystore is encoded into them, are written in `README.md`
- [x] Local builds of both apps still need nothing from the workflow

## Comments

The workflow is `.github/workflows/release.yml`. A `version` job checks the tag is `v<major>.<minor>.<patch>` with an optional suffix and fails otherwise; a `test` job runs both suites; the `android` and `desktop` jobs wait for both; a `release` job attaches the three files with `gh release create`, adding `--prerelease` for a suffixed tag.

- **Version into the apps.** Android already took `-PredlinkVersion`. The desktop's `buildVersion` now takes `REDLINK_VERSION` over the git-derived local version, and both `electron.vite.config.ts` and `electron-builder.ts` go through it.
- **Signing.** `android/app/build.gradle.kts` makes a release signing config only when `redlinkKeystore` is given, with the passwords and alias as further Gradle properties. The workflow stops with the secret's name when any of the four is unset, decodes the keystore to a file and passes the passwords as `ORG_GRADLE_PROJECT_` variables, so they are never on a command line. A local `assembleRelease` gives `app-release-unsigned.apk`, as before.
- **Runners.** The desktop job is one `macos-26` runner building both the `dmg` and the Windows zip (`pnpm package --mac --win`), since the macOS icon needs Xcode 26 and a Mac already builds the zip. The APK is named `redLINK-<version>.apk`.

Checked on this Mac: with a throwaway keystore, release APKs for `1.2.0-rc1` and `1.2.0` both have version code 10200 and are signed by it; with no properties the release APK is unsigned and `0.0.0-dev`. `REDLINK_VERSION=1.2.0-rc1 pnpm package --mac --win` produced `redLINK-1.2.0-rc1-mac-arm64.dmg` and `redLINK-1.2.0-rc1-win-x64.zip`, with the version in the main bundle and `Info.plist`. The tag step was run in bash against `v1.2.0`, `v1.2.0-rc1`, `v1.2`, `vfoo` and `v1.2.0-`.

Not checked: the workflow has not run on GitHub. Its first run is the release candidate in ticket 47.
