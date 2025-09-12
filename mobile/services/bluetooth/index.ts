import { BleError, Characteristic, Device } from 'react-native-ble-plx'
import { BLEManager } from './manager'
import { fromByteArray, toByteArray } from 'base64-js'
import { crc16 } from '../../utils/crc'
import { Packet } from './packet'
import { uploadData } from '../socket'

export const uartServiceUUID = '6e400001-b5a3-f393-e0a9-e50e24dcca9e'
export const rxCharacteristicUUID = '6e400002-b5a3-f393-e0a9-e50e24dcca9e'
export const txCharacteristicUUID = '6e400003-b5a3-f393-e0a9-e50e24dcca9e'

export const startScan = (scanCallback: (error: BleError | null, device: Device | null) => void) => {
  return BLEManager.startDeviceScan([uartServiceUUID], null, scanCallback)
}

export const stopScan = () => BLEManager.stopDeviceScan()

export const connectToDevice = (id: string) => BLEManager.connectToDevice(id)

export const disconnectFromDevice = (id: string) => BLEManager.cancelDeviceConnection(id)

export const startStreamingData = async (device: Device) => {
  if (!device) return
  // TODO: clear this subscription when disconnected
  device.monitorCharacteristicForService(uartServiceUUID, txCharacteristicUUID, (error, characteristic) =>
    handleTX(error, characteristic, device)
  )
  requestLoop(device)
}

const requestLoop = async (device: Device) => {
  const isConnected = await device.isConnected()
  if (!isConnected) return
  try {
    await requestData(4, device)
    await requestData(32, device)
  } catch (error) {
    console.log('request error', error)
  }
  setTimeout(() => requestLoop(device), 10)
}

const requestData = async (command: number, device: Device) => {
  const start = 2
  const lenght = 1
  const end = 3

  const crc = crc16(new Uint8Array([command]))
  const request = new Uint8Array([start, lenght, command, crc.msb, crc.lsb, end])
  const requestBase64 = fromByteArray(request)
  await device.writeCharacteristicWithResponseForService(uartServiceUUID, rxCharacteristicUUID, requestBase64)
}

export const handleTX = (error: BleError | null, characteristic: Characteristic | null, device: Device) => {
  if (error) return console.log(error)
  if (!characteristic?.value) return
  const response = new Packet(toByteArray(characteristic.value).buffer, device)
  response.consume()
  uploadData()
}
