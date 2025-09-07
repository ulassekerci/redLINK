import { useBLEStore } from '../../store/ble'
import { crc16 } from '../../utils/crc'
import { Packet } from './packet'

const deviceName = 'redBLE'
const uartServiceUUID = '6e400001-b5a3-f393-e0a9-e50e24dcca9e'
const rxCharacteristicUUID = '6e400002-b5a3-f393-e0a9-e50e24dcca9e'
const txCharacteristicUUID = '6e400003-b5a3-f393-e0a9-e50e24dcca9e'

let device: BluetoothDevice | undefined
let gattServer: BluetoothRemoteGATTServer | undefined
let rxCharacteristic: BluetoothRemoteGATTCharacteristic | undefined
let txCharacteristic: BluetoothRemoteGATTCharacteristic | undefined

export const connect = async () => {
  console.log('Requesting ble')
  device = await navigator.bluetooth.requestDevice({
    filters: [{ namePrefix: deviceName }],
    optionalServices: [uartServiceUUID],
  })

  device.addEventListener('gattserverdisconnected', handleDisconnected)

  gattServer = await device.gatt?.connect()
  if (!gattServer) return console.log("Couldn't connect")

  const gattService = await gattServer.getPrimaryService(uartServiceUUID)
  console.log('Discovered GATT service')

  rxCharacteristic = await gattService.getCharacteristic(rxCharacteristicUUID)
  txCharacteristic = await gattService.getCharacteristic(txCharacteristicUUID)
  console.log('Discovered RX and TX characteristics')

  txCharacteristic.addEventListener('characteristicvaluechanged', handleTX)
  await txCharacteristic.startNotifications()
  console.log('Notifications Started.')
}

export const isConnected = () => {
  return !!(gattServer?.connected && rxCharacteristic && txCharacteristic)
}

const handleTX = (event: Event) => {
  const value = (event.target as BluetoothRemoteGATTCharacteristic).value
  if (!value) return
  const response = new Packet(value.buffer, device)
  response.parse()
}

export const requestData = async (command: number) => {
  if (!rxCharacteristic) return
  const start = 2
  const lenght = 1
  const end = 3

  const crc = crc16(new Uint8Array([command]))
  const request = new Uint8Array([start, lenght, command, crc.msb, crc.lsb, end])
  await rxCharacteristic.writeValue(request)
}

export const requestLoop = async () => {
  if (!isConnected()) return
  try {
    await requestData(4)
    await requestData(32)
  } catch (error) {
    console.log(error)
  }
  setTimeout(requestLoop, 10)
}

export const disconnect = () => {
  if (!device) return
  if (txCharacteristic) txCharacteristic.removeEventListener('characteristicvaluechanged', handleTX)
  if (gattServer?.connected) gattServer.disconnect()
  device.removeEventListener('gattserverdisconnected', handleDisconnected)
  device = undefined
  gattServer = undefined
  rxCharacteristic = undefined
  txCharacteristic = undefined
  console.log('BLE disconnected')
}

const handleDisconnected = () => {
  console.log('BLE device disconnected')
  disconnect()
  useBLEStore.getState().disconnectBLE()
}
