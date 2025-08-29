import { useVehicleStore } from '../../store/vehicle'
import { checkCRC } from '../../utils/crc'
import { parseGetValues } from './commands/get-values'
import { Payload } from './payload'

export class Packet {
  payload: Payload

  constructor(buffer: ArrayBufferLike) {
    const response = new Uint8Array(buffer)
    if (response.length < 6) throw new Error('Response is too short')

    const headerLenght = response[0]
    const payloadBytes = response.subarray(headerLenght, response.length - 3)

    const msb = response[response.length - 3]
    const lsb = response[response.length - 2]
    const isPayloadValid = checkCRC(payloadBytes, msb, lsb)
    if (!isPayloadValid) throw new Error('CRC check failed')

    this.payload = new Payload(payloadBytes.slice().buffer)
  }

  parse() {
    switch (this.payload.command) {
      // COMM_GET_VALUES
      case 4:
        const data = parseGetValues(this.payload)
        useVehicleStore.setState(data)
        // TODO: share this data over socket.io
        break

      default:
        console.log(`Command ${this.payload.command} not supported`)
        break
    }
  }
}
