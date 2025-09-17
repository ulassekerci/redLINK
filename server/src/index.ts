import { Server } from 'socket.io'

const io = new Server(3000, {
  cors: { origin: '*' },
})

const sources = new Map<string, string>() // id, name

io.on('connection', (socket) => {
  broadcastSources()

  socket.on('registerSource', (name: string) => {
    sources.set(socket.id, name)
    broadcastSources()
  })

  socket.on('sourceData', (payload) => {
    io.to(`clients:${socket.id}`).emit('data', payload)
  })

  socket.on('disconnect', () => {
    const isDeleted = sources.delete(socket.id)
    if (isDeleted) broadcastSources()
  })

  socket.on('registerClient', (sourceID) => {
    socket.join(`clients:${sourceID}`)
  })
})

const broadcastSources = () => {
  const sourcesArray = Array.from(sources, ([id, name]) => ({ id, name }))
  io.emit('sources', sourcesArray)
}
