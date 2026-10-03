import { create } from 'zustand'
import type { BluetoothDeviceEntry } from '../../preload/api'
import { startPolling, type BoardState, type Clock } from '../services/direct-link/polling'
import { WebBluetoothTransport, requestBoard } from '../services/direct-link/web-bluetooth'
import { useVehicleStore } from './vehicle'

// off: no direct link. choosing: the device list is open. connecting: a board
// was picked and its link is being made. Then the board answers or not.
export type DirectLinkPhase = 'off' | 'choosing' | 'connecting' | BoardState

interface DirectLinkState {
  phase: DirectLinkPhase
  // The devices the scan has found so far, while choosing.
  devices: BluetoothDeviceEntry[]
  // Bluetooth was off or not permitted; shown for 5 s.
  bluetoothUnavailable: boolean

  // Starts a scan. Call it from a click: Web Bluetooth needs a user gesture.
  start: () => void
  pick: (id: string) => void
  cancel: () => void
  // Closes the link and puts this laptop back on the hub.
  stop: () => void
}

const clock: Clock = {
  now: () => Date.now(),
  setTimeout: (fn, ms) => window.setTimeout(fn, ms),
  clearTimeout: (handle) => window.clearTimeout(handle as number),
}

let polling: ReturnType<typeof startPolling> | null = null
let cancelled = false
let unavailableTimer: number | undefined

export const useDirectLinkStore = create<DirectLinkState>()((set, get) => {
  const showUnavailable = () => {
    set({ phase: 'off', devices: [], bluetoothUnavailable: true })
    window.clearTimeout(unavailableTimer)
    unavailableTimer = window.setTimeout(() => set({ bluetoothUnavailable: false }), 5000)
  }

  const connect = (device: BluetoothDevice) => {
    set({ phase: 'connecting', devices: [] })
    // A board was picked: main leaves the hub, and from here on only the
    // direct link writes the store.
    window.redlink.directLinkStarted()
    // A direct link has no GPS. A fix left from the hub is dropped, so the
    // map shows none and the CSV's GPS cells are empty.
    useVehicleStore.setState({ gps: null })
    polling = startPolling(new WebBluetoothTransport(device), clock, {
      boardSample: (board) => useVehicleStore.setState({ board }),
      adcSample: (adc) => useVehicleStore.setState({ adc }),
      state: (phase) => set({ phase }),
    })
  }

  window.redlink.onBluetoothDevices((devices) => {
    if (get().phase === 'choosing') set({ devices })
  })

  return {
    phase: 'off',
    devices: [],
    bluetoothUnavailable: false,

    start: () => {
      if (get().phase !== 'off') return
      if (!navigator.bluetooth) return showUnavailable()
      cancelled = false
      set({ phase: 'choosing', devices: [], bluetoothUnavailable: false })
      requestBoard().then(connect, () => {
        if (cancelled) set({ phase: 'off', devices: [] })
        else showUnavailable()
      })
    },

    pick: (id) => window.redlink.pickBluetoothDevice(id),

    cancel: () => {
      cancelled = true
      window.redlink.cancelBluetoothScan()
    },

    stop: () => {
      polling?.stop()
      polling = null
      set({ phase: 'off' })
      window.redlink.directLinkEnded()
    },
  }
})

export const onDirectLink = (phase: DirectLinkPhase) =>
  phase === 'connecting' || phase === 'answering' || phase === 'not_answering'

// The status line's text on and around a direct link, or null for none.
export const directLinkStatus = ({ phase, bluetoothUnavailable }: DirectLinkState) => {
  if (phase === 'connecting') return 'Araca bağlanılıyor'
  if (phase === 'not_answering') return 'Araç yanıt vermiyor, yeniden deneniyor'
  if (bluetoothUnavailable) return 'Bluetooth kullanılamıyor'
  return null
}
