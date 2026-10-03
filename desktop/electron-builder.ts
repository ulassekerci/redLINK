import type { Configuration } from 'electron-builder'
import { buildVersion } from './scripts/version'

// The same stamp electron.vite.config.ts gives the app (desktop spec 2.11).
// The version in package.json is a placeholder, so the package takes this one.
const version = buildVersion(import.meta.dirname)

// https://www.electron.build/configuration
const config: Configuration = {
  appId: 'org.metucet.redlink',
  productName: 'redLINK',
  extraMetadata: { version },
  // electron-vite bundles every dependency into out/, so the package carries
  // no node_modules.
  files: ['out/**', '!node_modules'],
  npmRebuild: false,
  artifactName: '${productName}-${version}-${os}-${arch}.${ext}',
  // The app never updates itself, so no update metadata is written.
  publish: null,
  dmg: { writeUpdateInfo: false },
  mac: {
    target: [{ target: 'dmg', arch: 'arm64' }],
    icon: 'build/icon.icon',
    // Ad-hoc signed and not notarized. Only notarization needs the hardened
    // runtime, and under it the direct link's Bluetooth would need an
    // entitlement.
    identity: '-',
    hardenedRuntime: false,
    notarize: false,
    extendInfo: {
      NSBluetoothAlwaysUsageDescription: 'redLINK, araca doğrudan bağlanmak için Bluetooth kullanır.',
    },
  },
  win: {
    // A zip that is unzipped anywhere and run in place.
    target: [{ target: 'zip', arch: 'x64' }],
    icon: 'build/icon.png',
  },
}

export default config
