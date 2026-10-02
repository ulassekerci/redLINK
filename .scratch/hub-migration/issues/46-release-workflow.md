# 46: Release workflow

**What to build:** pushing a version tag produces a release. One GitHub Actions workflow, triggered only by `v*` tags, runs both apps' test suites, then builds the release APK, the macOS app and the Windows app, each stamped with the version from the tag, and attaches all three to one GitHub Release. A tag with a suffix publishes a pre-release. A push to main builds nothing and tests nothing.

The release keystore does not exist yet. This ticket writes the workflow to read it from Actions secrets and documents which secrets it expects; creating them and cutting the first candidate is ticket 47.

Spec: [front page](../spec.md) sections 4 and 5; [Android app spec](../spec-android.md) 2.13 and section 4; [desktop app spec](../spec-desktop.md) 2.12 and section 4.

**Blocked by:** 32 (Desktop packaging), 33 (Android project and Kotlin protocol module).

**Status:** ready-for-agent

- [ ] There is one workflow, triggered on `v*` tags only, with no manual trigger and none on ordinary pushes
- [ ] Its first step runs `./gradlew testDebugUnitTest` and the desktop `pnpm test`; a failing test produces no release
- [ ] The version comes from the tag alone and is stamped into both apps; no file in the repo holds it
- [ ] `v1.2.0-rc1` and `v1.2.0` give the same Android version code
- [ ] The APK is a release build signed with the keystore from Actions secrets
- [ ] The macOS package is the arm64 `dmg` and the Windows package is the x64 zip
- [ ] All three are attached to one GitHub Release for the tag; a plain tag publishes a release and a suffixed tag a pre-release
- [ ] The names of the secrets the workflow expects, and how the keystore is encoded into them, are written in `README.md`
- [ ] Local builds of both apps still need nothing from the workflow
