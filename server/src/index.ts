import { RemoteSocket, Server, Socket } from 'socket.io'

const io = new Server(3000, {
  cors: { origin: '*' },
})

io.on('connection', (socket) => {
  const socketName = getName(socket)
  if (!socketName) handleClient(socket)
  else handleSource(socket, socketName)
})

const handleClient = (socket: Socket) => {
  socket.on('hello', updateSources)
}

const handleSource = async (socket: Socket, name: string) => {
  updateSources()
  socket.on('source', (data) => {
    socket.broadcast.emit(name, data)
  })
  socket.on('disconnect', updateSources)
}

const getName = (socket: Socket | RemoteSocket<any, any>) => {
  const nameHeader = socket.handshake.headers['x-source-name']
  if (nameHeader) return String(nameHeader)
  else return null
}

const updateSources = async () => {
  const sockets = await io.fetchSockets()
  const sources = sockets.map(getName).filter(Boolean)
  io.emit('sources', sources)
}
