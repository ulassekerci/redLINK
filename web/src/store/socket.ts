import { create } from 'zustand'
import { socket } from '../services/socket'
import type { Source } from '../services/socket/interfaces'

interface SocketState {
  sources: Source[]
  connectedSourceID: string | null

  connect: (sourceID: string) => void
}

export const useSocketStore = create<SocketState>((set, get) => {
  socket.on('sources', (sources: Source[]) => {
    // if there is only one active source, connect it
    if (sources.length === 1) get().connect(sources[0].id)
    // check if connected source disconnected
    const oldSource = sources.find((source) => source.id === get().connectedSourceID)
    set({ sources, connectedSourceID: oldSource?.id ?? null })
  })

  return {
    sources: [],
    connectedSourceID: null,

    connect: (newSourceID: string) => {
      socket.emit('registerClient', newSourceID)
      set({ connectedSourceID: newSourceID })
    },
  }
})
