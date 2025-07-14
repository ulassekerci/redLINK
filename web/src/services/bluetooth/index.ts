import { crc16 } from '../../utils/crc'
import { Packet } from './packet'

const deviceName = 'redBLE'
const uartServiceUUID = '6e400001-b5a3-f393-e0a9-e50e24dcca9e'
const rxCharacteristicUUID = '6e400002-b5a3-f393-e0a9-e50e24dcca9e'
const txCharacteristicUUID = '6e400003-b5a3-f393-e0a9-e50e24dcca9e'

let gattServer: BluetoothRemoteGATTServer | undefined
let rxCharacteristic: BluetoothRemoteGATTCharacteristic
let txCharacteristic: BluetoothRemoteGATTCharacteristic

export const connectBLE = async () => {
  console.log('Requesting ble')
  const device = await navigator.bluetooth.requestDevice({
    filters: [{ name: deviceName }],
    optionalServices: [uartServiceUUID],
  })

  gattServer = await device.gatt?.connect()
  if (!gattServer) return console.log("Couldn't connect")

  const gattService = await gattServer.getPrimaryService(uartServiceUUID)
  console.log('Discovered GATT service')

  rxCharacteristic = await gattService.getCharacteristic(rxCharacteristicUUID)
  txCharacteristic = await gattService.getCharacteristic(txCharacteristicUUID)
  console.log('Discovered RX and TX characteristics')

  txCharacteristic.addEventListener('characteristicvaluechanged', handleTX)
  txCharacteristic.startNotifications()
  console.log('Notifications Started.')
}

export const isConnected = () => {
  return !!(gattServer && rxCharacteristic)
}

const handleTX = (event: Event) => {
  const value = (event.target as BluetoothRemoteGATTCharacteristic).value
  if (!value) return

  const response = new Packet(value.buffer)
  response.parse()
}

export const requestValues = async () => {
  const start = 2
  const lenght = 1
  const command = 4
  const end = 3

  const crc = crc16(new Uint8Array([command]))
  const request = new Uint8Array([start, lenght, command, crc.msb, crc.lsb, end])
  await rxCharacteristic.writeValue(request)
}
