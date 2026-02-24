import { create } from 'zustand'
import { BleError, Device, Subscription } from 'react-native-ble-plx'
import * as ble from '../services/bluetooth'
import { useVehicleStore } from './vehicle'
import { gps } from '../services/location'

interface BLEState {
  devices: Device[]
  isScanning: boolean
  connectedDevice: Device | null
  isReconnecting: boolean

  scan: () => Promise<void>
  stopScan: () => Promise<void>
  connect: (deviceID: string) => void
  disconnect: () => Promise<void>
}

export const useBLEStore = create<BLEState>((set, get) => {
  let reconnecting = false
  let intentionalDisconnect = false
  let disconnectSub: Subscription | null = null

  const cleanupDisconnectListener = () => {
    if (disconnectSub) {
      disconnectSub.remove()
      disconnectSub = null
    }
  }

  const listenForDisconnect = (device: Device, deviceID: string) => {
    cleanupDisconnectListener()
    disconnectSub = device.onDisconnected(() => {
      cleanupDisconnectListener()
      if (!intentionalDisconnect) {
        attemptReconnect(deviceID)
      }
    })
  }

  const attemptReconnect = async (deviceID: string) => {
    if (intentionalDisconnect) return
    if (reconnecting) return
    reconnecting = true

    const reconnect = async () => {
      if (intentionalDisconnect) {
        reconnecting = false
        return
      }

      console.log(`Attempting reconnect...`)
      set({ isReconnecting: true })

      try {
        const connectedDevice = await ble.connectToDevice(deviceID)
        set({ connectedDevice, isReconnecting: false })
        await connectedDevice.requestMTU(185)
        await connectedDevice.discoverAllServicesAndCharacteristics()
        ble.startStreamingData(connectedDevice)
        listenForDisconnect(connectedDevice, deviceID)
        reconnecting = false
        console.log('Reconnected successfully')
      } catch (error) {
        console.log(`Reconnect attempt failed:`, error)
        await reconnect()
      }
    }

    await reconnect()
  }

  return {
    devices: [],
    isScanning: false,
    connectedDevice: null,
    isReconnecting: false,

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
      intentionalDisconnect = false
      reconnecting = false
      try {
        const connectedDevice = await ble.connectToDevice(id)
        set({ connectedDevice })
        await connectedDevice.requestMTU(185)
        await connectedDevice.discoverAllServicesAndCharacteristics()
        get().stopScan()
        ble.startStreamingData(connectedDevice)
        gps.start()
        listenForDisconnect(connectedDevice, id)
      } catch (error) {
        console.log('Failed to connect', error)
      }
    },

    disconnect: async () => {
      intentionalDisconnect = true
      reconnecting = false
      cleanupDisconnectListener()
      const deviceID = get().connectedDevice?.id
      if (deviceID) ble.disconnectFromDevice(deviceID)
      set({ connectedDevice: null, isReconnecting: false })
      gps.stop()
      useVehicleStore.getState().clear()
    },
  }
})
