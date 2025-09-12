import { io } from 'socket.io-client'
import { useVehicleStore } from '../../store/vehicle'

const socket = io('http://172.20.10.2:3000', {
  extraHeaders: {
    // TODO: get name input
    'x-source-name': 'redLink Development',
  },
})

export const uploadData = () => {
  const vehicleData = useVehicleStore.getState()
  socket.emit('source', vehicleData)
}
