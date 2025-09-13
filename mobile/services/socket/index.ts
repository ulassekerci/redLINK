import { io } from 'socket.io-client'
import { useVehicleStore } from '../../store/vehicle'

const socket = io('http://172.20.10.2:3000')

socket.on('connect', () => {
  // TODO: get user input
  socket.emit('registerSource', 'Ulaş')
})

export const uploadData = () => {
  const vehicleData = useVehicleStore.getState()
  socket.emit('sourceData', vehicleData)
}
