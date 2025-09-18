import { io } from 'socket.io-client'
import { useVehicleStore } from '../../store/vehicle'
import { useDeviceStore } from '../../store/device'

const socket = io(process.env.EXPO_PUBLIC_SOCKET_URL)

export const uploadData = () => {
  const vehicleData = useVehicleStore.getState()
  socket.emit('sourceData', vehicleData)
}

export const registerName = async () => {
  const deviceName = useDeviceStore.getState().deviceName
  socket.emit('registerSource', deviceName ?? 'redLINK')
}

socket.on('connect', registerName)
useDeviceStore.subscribe((state, prev) => {
  if (state.deviceName !== prev.deviceName) registerName()
})
