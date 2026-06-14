import { BleError, Characteristic, Device } from 'react-native-ble-plx'
import { crc16 } from '../../utils/crc'
import * as base64 from 'base64-js'
import { Packet } from './packet'
import { rxCharacteristicUUID, uartServiceUUID } from './uuid'

interface PendingRequest {
  command: number
  device: Device
  promise: {
    resolve: (value: Packet) => void
    reject: (reason?: Error) => void
  }
  timeoutId: ReturnType<typeof setTimeout>
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

    return new Promise<Packet>(async (resolve, reject) => {
      const timeoutId = setTimeout(() => {
        if (this.pendingRequest && this.pendingRequest.timeoutId === timeoutId) {
          this.clear()
          reject(new Error('UART response timeout'))
        }
      }, 250)

      this.pendingRequest = {
        command,
        device,
        promise: {
          resolve,
          reject,
        },
        timeoutId,
      }

      try {
        await device.writeCharacteristicWithResponseForService(uartServiceUUID, rxCharacteristicUUID, requestBase64)
      } catch (error) {
        if (this.pendingRequest && this.pendingRequest.timeoutId === timeoutId) {
          this.clear()
          reject(error as Error)
        }
      }
    })
  }

  handleTX(error: BleError | null, characteristic: Characteristic | null, device: Device) {
    if (error) {
      if (this.pendingRequest) {
        const { promise } = this.pendingRequest
        this.clear()
        promise.reject(error)
      }
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
    clearTimeout(this.pendingRequest.timeoutId)
    this.pendingRequest = null
  }
}

export const UARTClient = new UART()
