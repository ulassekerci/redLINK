import { io } from 'socket.io-client'
import { useVehicleStore } from '../../store/vehicle'

export const socket = io('http://localhost:3000')

socket.on('data', (payload) => {
  useVehicleStore.setState(payload)
})
