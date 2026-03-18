import { BleError, Characteristic, Device } from 'react-native-ble-plx'
import { crc16 } from '../../utils/crc'
import * as base64 from 'base64-js'
import { Packet } from './packet'
import { rxCharacteristicUUID, uartServiceUUID } from './uuid'

interface PendingRequest {
  command: number
  promise: {
    resolve: (value: Packet) => void
    reject: (reason?: Error) => void
    timeout: ReturnType<typeof setTimeout>
  }
}

class UART {
  pendingRequest: PendingRequest | null = null

  async send(command: number, device: Device) {
    if (this.pendingRequest) throw new Error('UART busy')
    const start = 2 // start byte is always 2
    const length = 1 // 1 is used for short commands
    const crc = crc16(new Uint8Array([command]))
    const end = 3 // end byte is always 3
    const bytes = new Uint8Array([start, length, command, crc.msb, crc.lsb, end])
    const requestBase64 = base64.fromByteArray(bytes)
    let currentRequest: PendingRequest
    const promise = new Promise<Packet>((resolve, reject) => {
      this.pendingRequest = {
        command,
        promise: {
          resolve,
          reject,
          timeout: setTimeout(() => this.timeout(reject), 250),
        },
      }
      currentRequest = this.pendingRequest // capture locally
    })
    try {
      await device.writeCharacteristicWithResponseForService(uartServiceUUID, rxCharacteristicUUID, requestBase64)
      return promise
    } catch (error) {
      currentRequest!.promise.reject(error as Error)
      this.clear()
      throw error
    }
  }

  timeout(reject: (reason: Error) => void) {
    reject(new Error('UART response timeout'))
    this.clear()
  }

  handleTX(error: BleError | null, characteristic: Characteristic | null, device: Device) {
    if (error) {
      this.pendingRequest?.promise.reject(error)
      this.clear()
      return
    }

    if (!characteristic?.value) return
    if (!this.pendingRequest) return

    const response = new Packet(base64.toByteArray(characteristic.value).buffer, device)
    if (response.payload.command !== this.pendingRequest.command) return

    if (this.pendingRequest.promise) {
      this.pendingRequest.promise.resolve(response)
      this.clear()
    }
  }

  clear() {
    if (!this.pendingRequest) return
    clearTimeout(this.pendingRequest.promise.timeout)
    this.pendingRequest = null
  }
}

export const UARTClient = new UART()
