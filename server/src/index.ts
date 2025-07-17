import { serve } from '@hono/node-server'
import { Server as ioServer } from 'socket.io'
import { Hono } from 'hono'

const app = new Hono()
const server = serve({ fetch: app.fetch, port: 3000 })
const io = new ioServer(server)

app.get('/', (c) => {
  return c.text('Hello!')
})

io.on('connection', (socket) => {
  console.log(`${socket.id} connected`)
  socket.on('data', (arg) => {
    console.log(arg)
  })
})

io.on('connection', (socket) => {
  socket.on('disconnect', (reason) => {
    console.log(`${socket.id} disconnected - reason: ${reason}`)
  })
})
