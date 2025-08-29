import { create } from 'zustand'
import * as ble from '../services/bluetooth'

interface BLEState {
  connected: boolean
  connecting: boolean

  connectBLE: () => Promise<void>
  disconnectBLE: () => void
}

export const useBLEStore = create<BLEState>((set) => {
  let intervalId: number | undefined
  let retryTimeout: number | undefined

  const reconnect = () => {
    if (retryTimeout) return
    retryTimeout = setTimeout(async () => {
      retryTimeout = undefined
      await useBLEStore.getState().connectBLE()
    }, 500)
  }

  return {
    connected: false,
    connecting: false,

    connectBLE: async () => {
      set({ connecting: true })
      try {
        if (!ble.isConnected()) await ble.connect()
        set({ connected: true, connecting: false })

        if (intervalId) return
        intervalId = window.setInterval(() => {
          if (ble.isConnected()) ble.requestValues()
        }, 100)
      } catch (err) {
        ble.disconnect()
        set({ connected: false, connecting: false })
        console.error('BLE connection failed:', err)
      }
    },

    disconnectBLE: (retry = false) => {
      if (intervalId) {
        clearInterval(intervalId)
        intervalId = undefined
      }
      set({ connected: false, connecting: false })

      if (retry) reconnect()
      else ble.disconnect()
    },
  }
})
