import { crc16 } from './crc'

const SHORT_START = 0x02
const LONG_START = 0x03
const STOP = 0x03

export function encodeFrame(payload: Uint8Array) {
  const long = payload.length > 0xff
  const header = long ? [LONG_START, payload.length >> 8, payload.length & 0xff] : [SHORT_START, payload.length]
  const crc = crc16(payload)
  return Uint8Array.from([...header, ...payload, crc >> 8, crc & 0xff, STOP])
}

// Turns a byte stream into frame payloads. A read may hold part of a frame or
// several frames, so bytes are buffered until a frame is complete. On a bad
// start byte, length, CRC or stop byte the decoder skips one byte and scans on.
// The stop byte is also the long start byte, so a skip can land on what reads
// as a long frame; the decoder then waits for its length before scanning on.
export class FrameDecoder {
  private buffer = new Uint8Array(0)

  push(bytes: Uint8Array) {
    const buffer = new Uint8Array(this.buffer.length + bytes.length)
    buffer.set(this.buffer)
    buffer.set(bytes, this.buffer.length)

    const payloads: Uint8Array[] = []
    let offset = 0
    while (offset < buffer.length) {
      const result = readFrame(buffer, offset)
      if (result === 'incomplete') break
      if (result === 'invalid') {
        offset += 1
        continue
      }
      payloads.push(result.payload)
      offset = result.end
    }

    this.buffer = buffer.slice(offset)
    return payloads
  }
}

function readFrame(buffer: Uint8Array, offset: number) {
  let length: number
  let payloadStart: number
  if (buffer[offset] === SHORT_START) {
    if (buffer.length < offset + 2) return 'incomplete'
    length = buffer[offset + 1]
    payloadStart = offset + 2
  } else if (buffer[offset] === LONG_START) {
    if (buffer.length < offset + 3) return 'incomplete'
    length = (buffer[offset + 1] << 8) | buffer[offset + 2]
    if (length <= 0xff) return 'invalid'
    payloadStart = offset + 3
  } else {
    return 'invalid'
  }
  if (length === 0) return 'invalid'

  const payloadEnd = payloadStart + length
  const end = payloadEnd + 3
  if (buffer.length < end) return 'incomplete'
  if (buffer[end - 1] !== STOP) return 'invalid'

  const payload = buffer.slice(payloadStart, payloadEnd)
  const crc = (buffer[payloadEnd] << 8) | buffer[payloadEnd + 1]
  if (crc !== crc16(payload)) return 'invalid'
  return { payload, end }
}
