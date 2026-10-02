# 33: Android project and Kotlin protocol module

**What to build:** the phone app exists as a project that builds, installs as "redLINK dev" beside any release app, and opens on an empty black screen. Its Kotlin protocol module passes the same vectors the desktop app passes, which is what keeps the two implementations from drifting apart.

The React Native app's VESC parsing code is reachable through the `legacy-stack` tag as a reference.

Spec: [Android app spec](../spec-android.md) 2.12 (debug build), 2.13 (version), 2.14 (stack) and the first two suites in section 4; [front page](../spec.md) section 3 step 5; the wire format in `protocol/README.md`.

**Blocked by:** 25 (Protocol vectors and the TypeScript protocol module).

**Status:** ready-for-agent

- [ ] `android/` is a Gradle project: Kotlin, Jetpack Compose with Material 3, minimum API 31, targeting the current API
- [ ] The release application ID is `org.metucet.redlink`; debug builds are `org.metucet.redlink.debug` and named "redLINK dev"
- [ ] The version name is stamped at build time from the latest version tag in git with `-dev` added, `0.0.0-dev` with no tag, and can be overridden by the release workflow
- [ ] The version code is major x 10000 + minor x 100 + patch
- [ ] The app builds, installs and opens on a dark, pure black screen
- [ ] The Kotlin protocol module covers the same ground as the TypeScript one: buffering frame decoder, CRC, the three requests, the three reply parsers, our four messages, and the team code functions including generating a code
- [ ] `ProtocolVectorsTest` reads `protocol/vectors.json` from the sibling directory, runs every frame case and passes
- [ ] `TeamCodeTest` runs every team-code case from the same file, checks that a generated code is 8 characters of the alphabet and passes its own check, and passes
- [ ] `./gradlew testDebugUnitTest` runs both with no device, emulator or network
- [ ] There is no tooling at the repo root
