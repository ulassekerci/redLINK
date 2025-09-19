import { create } from 'zustand'
import { BleError, Device } from 'react-native-ble-plx'
import * as ble from '../services/bluetooth'
import { useVehicleStore } from './vehicle'
import { gps } from '../services/location'

interface BLEState {
  devices: Device[]
  isScanning: boolean
  connectedDevice: Device | null

  scan: () => Promise<void>
  stopScan: () => Promise<void>
  connect: (deviceID: string) => void
  disconnect: () => Promise<void>
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
      if (oldDevice) await get().disconnect()
      try {
        const connectedDevice = await ble.connectToDevice(id)
        set({ connectedDevice })
        await connectedDevice.requestMTU(185)
        await connectedDevice.discoverAllServicesAndCharacteristics()
        get().stopScan()
        ble.startStreamingData(connectedDevice)
        gps.start()
        connectedDevice.onDisconnected(get().disconnect)
      } catch (error) {
        console.log('Failed to connect', error)
      }
    },

    disconnect: async () => {
      const deviceID = get().connectedDevice?.id
      if (deviceID) ble.disconnectFromDevice(deviceID)
      set({ connectedDevice: null })
      gps.stop()
      useVehicleStore.getState().clear()
    },
  }
})
