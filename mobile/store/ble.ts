import { create } from 'zustand'
import { BleError, Device } from 'react-native-ble-plx'
import * as ble from '../services/bluetooth'

interface BLEState {
  devices: Device[]
  isScanning: boolean
  connectedDevice: Device | null

  scan: () => Promise<void>
  stopScan: () => Promise<void>
  connect: (deviceID: string) => void
  disconnect: () => void
}

export const useBLEStore = create<BLEState>((set, get) => {
  return {
    devices: [],
    isScanning: false,
    connectedDevice: null,

    scan: async () => {
      await ble.startScan((error: BleError | null, newDevice: Device | null) => {
        if (error) console.log(error)
        if (!newDevice) return
        const oldDevices = get().devices
        const deviceExists = oldDevices.find((d) => d.id === newDevice.id)
        if (deviceExists) return
        else set({ devices: [...get().devices, newDevice] })
      })
      set({ isScanning: true })
    },

    stopScan: async () => {
      await ble.stopScan()
      set({ isScanning: false })
    },

    connect: async (id: string) => {
      const oldDevice = get().connectedDevice
      if (oldDevice) get().disconnect()
      try {
        const connectedDevice = await ble.connectToDevice(id)
        await connectedDevice.requestMTU(185)
        set({ connectedDevice })
        await connectedDevice.discoverAllServicesAndCharacteristics()
        get().stopScan()
        ble.startStreamingData(connectedDevice)
      } catch (error) {
        console.log('Failed to connect', error)
      }
    },

    disconnect: async () => {
      const deviceID = get().connectedDevice?.id
      if (!deviceID) return set({ connectedDevice: null })
      await ble.disconnectFromDevice(deviceID)
      set({ connectedDevice: null })
    },
  }
})
