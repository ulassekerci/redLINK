import { Device, Subscription } from 'react-native-ble-plx'
import { BLEManager } from './manager'
import { UARTClient } from './uart'
import { uploadData } from '../socket'
import { BLEScanner } from './scanner'

class BLEClient {
  readonly serviceUUID = '6e400001-b5a3-f393-e0a9-e50e24dcca9e'
  readonly rxCharacteristicUUID = '6e400002-b5a3-f393-e0a9-e50e24dcca9e'
  readonly txCharacteristicUUID = '6e400003-b5a3-f393-e0a9-e50e24dcca9e'

  txSubscription: Subscription | null = null

  get scanner() {
    return BLEScanner
  }

  connect = (deviceID: string) => {
    return BLEManager.connectToDevice(deviceID)
  }

  disconnect = async (deviceID: string) => {
    await BLEManager.cancelDeviceConnection(deviceID)
    this.txSubscription?.remove()
  }

  startStream(device: Device) {
    if (!device) return null
    this.txSubscription = device.monitorCharacteristicForService(
      this.serviceUUID,
      this.txCharacteristicUUID,
      (error, characteristic) => UARTClient.handleTX(error, characteristic, device),
    )
    this.requestLoop(device)
  }

  async requestLoop(device: Device) {
    const isConnected = await device.isConnected()
    if (!isConnected) return
    const before = performance.now()
    try {
      await this.requestData(4, device)
      await this.requestData(32, device)
      uploadData()
    } catch (error) {
      return
    }
    const after = performance.now()
    const duration = after - before
    const timeLeft = 50 - duration
    setTimeout(() => this.requestLoop(device), Math.max(timeLeft, 0))
  }

  async requestData(command: number, device: Device) {
    const packet = await UARTClient.send(command, device)
    packet.consume()
  }
}

export const BLE = new BLEClient()
