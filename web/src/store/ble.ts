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

  return {
    connected: false,
    connecting: false,

    connectBLE: async () => {
      set({ connecting: true })
      try {
        await ble.connect()
        set({ connected: true, connecting: false })

        intervalId = window.setInterval(() => {
          if (ble.isConnected()) ble.requestValues()
        }, 100)
      } catch (err) {
        console.error('BLE connection failed:', err)
        set({ connected: false, connecting: false })
      }
    },

    disconnectBLE: () => {
      if (intervalId) {
        clearInterval(intervalId)
        intervalId = undefined
      }
      set({ connected: false, connecting: false })
      ble.disconnect()
    },
  }
})
