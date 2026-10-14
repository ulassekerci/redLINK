# redLINK

An end-to-end telemetry system for our Shell Eco-marathon vehicle
- **Android app** → Rides in the car, polls the vehicle's VESC over Bluetooth, adds GPS, logs every run and streams it to the pit
- **Desktop app (Electron + React)** → Shows live vehicle data on the pit laptops, and can connect straight to the VESC over Bluetooth
- **Protocol** → The wire format the two apps share

The apps reach each other through a [VESC TCP hub](protocol/tcp-hub.md), so there is no server of our own to run.

<img src="https://github.com/user-attachments/assets/8b8db57c-dbb5-43e1-873e-c2d54d18e5df" alt="Desktop App Screenshot" width="70%" />

---

## Repo layout

| Path | What it is |
|---|---|
| `android/` | The Android app, a Gradle project |
| `desktop/` | The desktop app, a standalone pnpm package |
| `protocol/` | The wire format in prose and notes on the hub |
| `docs/` | Architecture decision records and agent docs |

The stack is being rebuilt. The plan is in the [hub migration spec](.scratch/hub-migration/spec.md), and vocabulary is in [CONTEXT.md](CONTEXT.md).
The previous stack is kept under the `legacy-stack` tag.

## Getting Started
Clone and navigate into the repository:
```bash
git clone https://github.com/ulassekerci/redLINK.git
cd redLINK
```
### Desktop App
Navigate to the desktop folder and install dependencies with [pnpm](https://pnpm.io)
```bash
cd desktop
pnpm install
```
Run the app from source
```bash
pnpm dev
```
Or package it for the system you are on
```bash
pnpm package
```
A Mac gets `dist/redLINK-<version>-mac-arm64.dmg` and a Windows machine gets `dist/redLINK-<version>-win-x64.zip`. A Mac can build the Windows zip too, with `pnpm package --win`.

### Android App
Open the `android` folder in [Android Studio](https://developer.android.com/studio) and run `app`. A build from source installs as **redLINK dev**, beside the release app and with its own settings.

Or build from a terminal. Gradle needs a JDK, and the one inside Android Studio will do (the path is a Mac's)
```bash
cd android
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew installDebug
```
Run the tests, which need no phone or emulator
```bash
./gradlew testDebugUnitTest
```

## Making a release

Pushing a version tag runs [the release workflow](.github/workflows/release.yml): it runs both apps' tests, builds the APK, the macOS `dmg` and the Windows zip with the tag's version, and attaches all three to a GitHub Release. A tag with a suffix makes a pre-release, which is how a build is tried without using up a version number.
```bash
git tag v1.2.0-rc1
git push origin v1.2.0-rc1
```

The APK is signed with the release keystore, which the workflow reads from these Actions secrets (Settings → Secrets and variables → Actions):

| Secret | What it holds |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | The keystore file, base64 encoded |
| `ANDROID_KEYSTORE_PASSWORD` | The keystore's password |
| `ANDROID_KEY_ALIAS` | The alias of the key inside it |
| `ANDROID_KEY_PASSWORD` | The key's password |

The keystore is made once, and a copy of it and its passwords is kept offline: if it is lost, a newer APK can no longer install over the app on the phone. To make it with the JDK's `keytool`, with `JAVA_HOME` set as above, and copy it encoded for the first secret, on a Mac
```bash
"$JAVA_HOME/bin/keytool" -genkeypair -keystore redlink-release.jks -alias redlink -keyalg RSA -keysize 4096 -validity 36500
```
```bash
base64 -i redlink-release.jks | pbcopy
```

## Opening the desktop app for the first time

The packages are not signed with a paid certificate, so each system has to be told once that the app may run.

### macOS (Apple silicon)

1. Open the `dmg` and drag redLINK into Applications.
2. Open redLINK. macOS says it could not verify the app. Click **Done**.
3. Open System Settings, go to **Privacy & Security** and scroll down to **Security**.
4. Click **Open Anyway** beside the line about redLINK, click **Open Anyway** again and enter your password.

From then on redLINK opens like any other app.

**Open Anyway** is offered for about an hour after step 2. If it is not there, open redLINK again and go back to step 3. On macOS 14 and older there is no trip to System Settings: right-click redLINK in Applications, choose **Open**, then **Open** again.

### Windows (64-bit)

1. Right-click the zip, choose **Extract All** and pick any folder. The app does not run from inside the zip.
2. Open the folder and run `redLINK.exe`.
3. SmartScreen says "Windows protected your PC". Click **More info**, then **Run anyway**.

There is no installer. To remove the app, delete the folder.

On a Windows 11 laptop with Smart App Control turned on there is no **Run anyway**: Smart App Control blocks unsigned apps outright, and redLINK runs only where it is off.
