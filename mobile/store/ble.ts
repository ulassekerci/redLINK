import { create } from 'zustand'
import { Device } from 'react-native-ble-plx'
import { BLE } from '../services/bluetooth'

interface BLEState {
  scanner: {
    isScanning: boolean
    devices: Map<string, Device> // id, device
    start: () => void
    stop: () => void
    scanCallback: (newDevice: Device) => void
  }

  connection: {
    device: Device | null
    state: 'connected' | 'disconnected' | 'reconnecting'
    intentionalDisconnect: boolean
  }

  connect: (deviceID: string) => void
  disconnect: () => Promise<void>
}

export const useBLEStore = create<BLEState>((set, get) => {
  return {
    scanner: {
      isScanning: false,
      devices: new Map<string, Device>(),
      start: async () => {
        await BLE.scanner.start(get().scanner.scanCallback)
        set((state) => ({ scanner: { ...state.scanner, isScanning: true } }))
      },
      stop: async () => {
        await BLE.scanner.stop()
        set((state) => ({ scanner: { ...state.scanner, isScanning: false } }))
      },
      scanCallback: (newDevice: Device) => {
        set((state) => {
          return {
            scanner: {
              ...state.scanner,
              devices: new Map(state.scanner.devices).set(newDevice.id, newDevice),
            },
          }
        })
      },
    },

    connection: {
      device: null,
      state: 'disconnected',
      intentionalDisconnect: true,
    },

    connect: async (deviceID) => {
      const oldDevice = get().connection.device
      if (oldDevice) await get().disconnect()

      const connectedDevice = await BLE.connect(deviceID)
      await connectedDevice.requestMTU(185)
      await connectedDevice.discoverAllServicesAndCharacteristics()

      set((state) => ({
        connection: {
          ...state.connection,
          device: connectedDevice,
          state: 'connected',
          intentionalDisconnect: false,
        },
      }))
      get().scanner.stop()
      BLE.startStream(connectedDevice)
    },

    disconnect: async () => {
      const oldDevice = get().connection.device
      if (!oldDevice) return
      await BLE.disconnect(oldDevice.id)
      set((state) => ({
        connection: {
          ...state.connection,
          device: null,
          state: 'disconnected',
          intentionalDisconnect: true,
        },
      }))
    },
  }
})
