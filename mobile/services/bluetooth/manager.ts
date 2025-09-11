import { BleManager } from 'react-native-ble-plx'

class BLEServiceInstance {
  manager: BleManager
  constructor() {
    this.manager = new BleManager()
  }
}

const BLEService = new BLEServiceInstance()
export const BLEManager = BLEService.manager
