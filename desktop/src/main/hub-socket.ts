import { connect } from 'node:net'
import type { HubSocketFactory } from './hub-client'

// The hub client's sockets in the app: plain TCP on Node's net module.
export const connectToHub: HubSocketFactory = (host, port, events) => {
  const socket = connect({ host, port })
  let closedHere = false

  // Each write must leave as its own segment: the hub drops what arrives with
  // the login line.
  socket.setNoDelay(true)
  socket.on('connect', events.connected)
  socket.on('data', events.data)
  // A failed connection raises an error and then closes.
  socket.on('error', () => {})
  socket.on('close', () => {
    if (!closedHere) events.closed()
  })

  return {
    write: (bytes) => {
      if (socket.writable) socket.write(bytes)
    },
    close: () => {
      closedHere = true
      // What was written is sent first; a socket still connecting has nothing
      // to send and is dropped at once.
      if (socket.connecting) socket.destroy()
      else socket.destroySoon()
    },
  }
}
