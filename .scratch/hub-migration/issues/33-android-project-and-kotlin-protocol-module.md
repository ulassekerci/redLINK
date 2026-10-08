# 33: Android project and Kotlin protocol module

**What to build:** the phone app exists as a project that builds, installs as "redLINK dev" beside any release app, and opens on an empty black screen. Its Kotlin protocol module passes the same vectors the desktop app passes, which is what keeps the two implementations from drifting apart.

The React Native app's VESC parsing code is reachable through the `legacy-stack` tag as a reference.

Spec: [Android app spec](../spec-android.md) 2.12 (debug build), 2.13 (version), 2.14 (stack) and the first two suites in section 4; [front page](../spec.md) section 3 step 5; the wire format in `protocol/README.md`.

**Blocked by:** 25 (Protocol vectors and the TypeScript protocol module).

**Status:** resolved

- [x] `android/` is a Gradle project: Kotlin, Jetpack Compose with Material 3, minimum API 31, targeting the current API
- [x] The release application ID is `org.metucet.redlink`; debug builds are `org.metucet.redlink.debug` and named "redLINK dev"
- [x] The version name is stamped at build time from the latest version tag in git with `-dev` added, `0.0.0-dev` with no tag, and can be overridden by the release workflow
- [x] The version code is major x 10000 + minor x 100 + patch
- [x] The app builds, installs and opens on a dark, pure black screen
- [x] The Kotlin protocol module covers the same ground as the TypeScript one: buffering frame decoder, CRC, the three requests, the three reply parsers, our four messages, and the team code functions including generating a code
- [x] `ProtocolVectorsTest` reads `protocol/vectors.json` from the sibling directory, runs every frame case and passes
- [x] `TeamCodeTest` runs every team-code case from the same file, checks that a generated code is 8 characters of the alphabet and passes its own check, and passes
- [x] `./gradlew testDebugUnitTest` runs both with no device, emulator or network
- [x] There is no tooling at the repo root

## Comments

Implemented as one Gradle module, `android/app`, with the protocol in the package `org.metucet.redlink.protocol`, so `./gradlew testDebugUnitTest` runs both suites. Gradle 9.8.1 through the wrapper, AGP 9.4.1 with its built-in Kotlin at 2.4.21, compile and target API 37 (Android 17). The debug build was installed and launched on an Android 16 emulator image, the reference phone's version.

- **Version code of 0.** Android refuses it, and the formula gives 0 for `0.0.0-dev`, which is every local build until the first version tag. Such a build gets 1. The Android spec 2.13 says so now.
- **Version override.** The release workflow passes `-PredlinkVersion=1.2.0-rc1`: the version without its `v`. The name is stamped as given and the code drops the suffix.
- **Vectors path.** The build passes the file's path to the tests and declares it as a test input, so editing `protocol/vectors.json` runs the Kotlin suites again.
- **Stricter than the TypeScript encoder.** The Kotlin encoder also refuses a negative fix time, a fix value that is not a number, and a status byte outside 0 to 255, where the TypeScript one wraps. It follows the wire format's rule that an encoder never wraps. No vector covers these.
- **Lobby request with bytes after the token.** Matches the TypeScript decoder: the first 8 bytes are the token and the rest is ignored.
- **Not ported:** generating a token (only a viewer makes one), the public hub's address (ticket 41) and the desktop's sample types.
- The `ProtocolVectorsTest` fails a case whose message has a key it does not read, so a field added to a vector cannot pass unchecked on the phone.

Handed on, not decided here:

- **Backup.** The manifest leaves Android's Auto Backup at its default, on. Once the team code is stored, a reinstall could bring the old code back, against 2.9's "Reinstalling the app makes a new code". Whichever ticket stores the code should set `android:allowBackup="false"`.
- **Launcher icon.** The app has none yet and shows Android's default.
