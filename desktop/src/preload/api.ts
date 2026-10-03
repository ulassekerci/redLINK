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
}

// The name is empty when the device reports none.
export interface BluetoothDeviceEntry {
  id: string
  name: string
}
