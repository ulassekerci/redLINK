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
| `android/` | The Android app, a Gradle project (not in the repo yet) |
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
