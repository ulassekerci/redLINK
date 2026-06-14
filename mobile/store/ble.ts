import { create } from 'zustand'
import { Device, Subscription } from 'react-native-ble-plx'
import { BLE } from '../services/bluetooth'

let disconnectionSubscription: Subscription | null = null
let reconnectTimeoutId: ReturnType<typeof setTimeout> | null = null

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

      // Listen to disconnection
      const subscription = connectedDevice.onDisconnected((error, device) => {
        const { intentionalDisconnect } = get().connection
        if (!intentionalDisconnect) {
          set((state) => ({
            connection: {
              ...state.connection,
              state: 'reconnecting',
            },
          }))
          BLE.txSubscription?.remove()

          startReconnectionTimeout(deviceID)
          attemptReconnection(deviceID)
        }
      })
      disconnectionSubscription = subscription

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

      set((state) => ({
        connection: {
          ...state.connection,
          intentionalDisconnect: true,
          state: 'disconnected',
          device: null,
        },
      }))

      if (reconnectTimeoutId) {
        clearTimeout(reconnectTimeoutId)
        reconnectTimeoutId = null
      }

      disconnectionSubscription?.remove()
      disconnectionSubscription = null

      if (oldDevice) {
        try {
          await BLE.disconnect(oldDevice.id)
        } catch (error) {
          console.warn('Error during manual disconnect:', error)
        }
      }
    },
  }
})

const startReconnectionTimeout = (deviceID: string) => {
  if (reconnectTimeoutId) clearTimeout(reconnectTimeoutId)
  reconnectTimeoutId = setTimeout(
    async () => {
      const currentStore = useBLEStore.getState()
      if (!currentStore.connection.intentionalDisconnect && currentStore.connection.state === 'reconnecting') {
        console.log('Reconnection timed out')
        try {
          await BLE.disconnect(deviceID)
        } catch (e) {
          // Ignore disconnect errors
        }
        useBLEStore.setState((state) => ({
          connection: {
            ...state.connection,
            device: null,
            state: 'disconnected',
          },
        }))
      }
    },
    5 * 60 * 1000, // 5 minutes is more than enough
  )
}

const attemptReconnection = async (deviceID: string) => {
  const store = useBLEStore.getState()
  if (store.connection.intentionalDisconnect || store.connection.state === 'disconnected') return

  try {
    console.log(`Attempting to reconnect to device: ${deviceID}`)
    // Clean up any old subscription and disconnect
    disconnectionSubscription?.remove()
    disconnectionSubscription = null
    try {
      await BLE.disconnect(deviceID)
    } catch (e) {
      // Ignore disconnection errors
    }

    if (useBLEStore.getState().connection.state === 'disconnected') return

    const connectedDevice = await BLE.connect(deviceID)
    await connectedDevice.requestMTU(185)
    await connectedDevice.discoverAllServicesAndCharacteristics()

    // Successfully reconnected, clear the timeout
    if (reconnectTimeoutId) {
      clearTimeout(reconnectTimeoutId)
      reconnectTimeoutId = null
    }

    const subscription = connectedDevice.onDisconnected((error, device) => {
      const currentStore = useBLEStore.getState()
      if (!currentStore.connection.intentionalDisconnect) {
        useBLEStore.setState((state) => ({
          connection: {
            ...state.connection,
            state: 'reconnecting',
          },
        }))
        BLE.txSubscription?.remove()

        startReconnectionTimeout(deviceID)
        attemptReconnection(deviceID)
      }
    })
    disconnectionSubscription = subscription

    useBLEStore.setState((state) => ({
      connection: {
        ...state.connection,
        device: connectedDevice,
        state: 'connected',
      },
    }))
    BLE.startStream(connectedDevice)
  } catch (error) {
    console.warn('Reconnection attempt failed:', error)

    const currentStore = useBLEStore.getState()
    if (!currentStore.connection.intentionalDisconnect && currentStore.connection.state === 'reconnecting') {
      setTimeout(() => {
        attemptReconnection(deviceID)
      }, 3000)
    }
  }
}
