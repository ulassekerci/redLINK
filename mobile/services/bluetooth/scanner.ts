import { BLEManager } from './manager'
import { uartServiceUUID } from './uuid'
import { Device } from 'react-native-ble-plx'

class Scanner {
  start(scanCallback: (newDevice: Device) => void) {
    return BLEManager.startDeviceScan([uartServiceUUID], null, (err, newDevice) => {
      if (err) return console.error('Failed to scan')
      if (!newDevice) return
      scanCallback(newDevice)
    })
  }

  async stop() {
    return BLEManager.stopDeviceScan()
  }
}

export const BLEScanner = new Scanner()
