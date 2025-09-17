import { io } from 'socket.io-client'
import { useVehicleStore } from '../../store/vehicle'

const socket = io(process.env.EXPO_PUBLIC_SOCKET_URL)

socket.on('connect', () => {
  // TODO: get user input
  socket.emit('registerSource', 'Ulaş')
})

export const uploadData = () => {
  const vehicleData = useVehicleStore.getState()
  socket.emit('sourceData', vehicleData)
}
