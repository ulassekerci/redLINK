import { create } from 'zustand'
import { socket } from '../services/socket'
import { useVehicleStore } from './vehicle'

interface SocketState {
  sources: string[]
  connectedSource: string | null
  connectToSource: (newSource: string) => void
}

export const useSocketStore = create<SocketState>((set, get) => {
  let updateCount = 0

  socket.on('sources', (sources: string[]) => {
    if (!updateCount && sources.length === 1) get().connectToSource(sources[0])
    set((state) => {
      updateCount += 1
      const oldSource = sources.find((source) => source === state.connectedSource)
      return {
        sources,
        connectedSource: oldSource ?? null,
      }
    })
  })

  return {
    sources: [],
    connectedSource: null,

    connectToSource: (newSource: string) => {
      if (newSource === get().connectedSource) return
      set((state) => {
        const oldSource = state.connectedSource
        if (oldSource) socket.off(oldSource)
        socket.on(newSource, (data) => useVehicleStore.setState(data))
        return { connectedSource: newSource }
      })
    },
  }
})
