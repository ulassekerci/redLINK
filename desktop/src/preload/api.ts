// What the preload script exposes to the renderer as `window.redlink`, and the
// only way the renderer reaches main. Later tickets extend it to the full list
// in the desktop spec, 2.3.
export interface RedlinkApi {
  version: () => Promise<string>
  // The devices a Bluetooth scan has found so far, each time the list grows.
  // Returns a function that unsubscribes.
  onBluetoothDevices: (listener: (devices: BluetoothDeviceEntry[]) => void) => () => void
  pickBluetoothDevice: (id: string) => void
  // Ends the scan with no device.
  cancelBluetoothScan: () => void
  // Opens the system's save dialog with the given default file name, and
  // writes the CSV where the person chooses. Resolves when the dialog closes.
  saveCsv: (defaultName: string, csv: string) => Promise<void>
  // The settings this instance read on launch or last saved.
  readSettings: () => Promise<Settings>
  // Stores the three values together and resolves with them as stored.
  // Resolves with null, and stores nothing, when the team code is not valid.
  writeSettings: (typed: TypedSettings) => Promise<Settings | null>
}

// What the settings file holds. The team code is without its dash, and null
// when there is none.
export interface Settings {
  teamCode: string | null
  hubHost: string
  hubPort: number
}

// The settings screen's three fields, as typed.
export interface TypedSettings {
  teamCode: string
  hubHost: string
  hubPort: string
}

// The name is empty when the device reports none.
export interface BluetoothDeviceEntry {
  id: string
  name: string
}
