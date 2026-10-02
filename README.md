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
Run the dashboard in a browser
```bash
pnpm dev
```
Or build it and serve the build
```bash
pnpm build
pnpm preview
```
