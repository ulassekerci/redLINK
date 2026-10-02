# Distribution

Type: grilling
Status: open

Map: [Hub migration](../map.md)

## Question

How do the two apps reach the people who use them?

Decide: how the APK is built and gets onto the phone, and how a newer build replaces it; how the Electron app is built, packaged and signed for macOS and Windows, and how the pit crew's laptops get a newer version; the macOS Bluetooth permission text, handed on from [Desktop app architecture](11-desktop-app-architecture.md); what happens when the phone and a laptop run builds with different protocol versions on race day.

Carry this from [Repo layout and cutover](12-repo-layout-and-cutover.md): `desktop/` uses pnpm. Check what that asks of the Electron build: pnpm does not run dependency install scripts unless they are allowed, and Electron's binary download is one; packagers can also need a hoisted `node_modules` layout.
