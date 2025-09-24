import { useVehicleStore } from '../../store/vehicle'
import { checkCRC } from '../../utils/crc'
import { parseGetDecodedADC } from './commands/get-adc'
import { parseGetValues } from './commands/get-values'
import { Payload } from './payload'

export class Packet {
  payload: Payload

  constructor(buffer: ArrayBufferLike, device?: BluetoothDevice) {
    const response = new Uint8Array(buffer)
    if (response.length < 6) throw new Error('Response is too short')

    const headerLenght = response[0]
    const payloadBytes = response.subarray(headerLenght, response.length - 3)

    const msb = response[response.length - 3]
    const lsb = response[response.length - 2]
    const isPayloadValid = checkCRC(payloadBytes, msb, lsb)
    const isMockDevice = device?.name?.includes('Mock')
    if (!isPayloadValid && !isMockDevice) throw new Error('CRC check failed')
    this.payload = new Payload(payloadBytes.slice().buffer)
  }

  parse() {
    switch (this.payload.command) {
      // COMM_GET_VALUES
      case 4:
        const parsedValues = parseGetValues(this.payload)
        useVehicleStore.setState({ ...parsedValues, timestamp: Date.now() })
        break

      // COMM_GET_DECODED_ADC
      case 32:
        const parsedADC = parseGetDecodedADC(this.payload)
        useVehicleStore.setState({ adc: parsedADC, timestamp: Date.now() })
        break

      default:
        console.log(`Command ${this.payload.command} not supported`)
        break
    }
  }
}
