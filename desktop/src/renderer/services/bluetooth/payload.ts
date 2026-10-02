export class Payload {
  buffer: ArrayBuffer
  index: number

  constructor(buffer: ArrayBuffer) {
    this.buffer = buffer
    this.index = 1 // skip command byte
  }

  get command() {
    return new DataView(this.buffer).getInt8(0)
  }

  get size() {
    return this.buffer.byteLength
  }

  get array() {
    return new Uint8Array(this.buffer)
  }

  readString() {
    let value = ''
    let charCode
    let char = ''

    while (charCode !== 0x00 && this.index < this.buffer.byteLength) {
      value = `${value}${char}`
      charCode = this.readUInt8()
      char = String.fromCharCode(charCode)
    }

    return value
  }

  readUInt8() {
    const value = new DataView(this.buffer).getUint8(this.index)
    this.index += 1
    return value
  }

  readUInt16() {
    const value = new DataView(this.buffer).getUint16(this.index)
    this.index += 2
    return value
  }

  readUInt32() {
    const value = new DataView(this.buffer).getUint32(this.index)
    this.index += 4
    return value
  }

  readInt8() {
    const value = new DataView(this.buffer).getInt8(this.index)
    this.index += 1
    return value
  }

  readInt16() {
    const value = new DataView(this.buffer).getInt16(this.index)
    this.index += 2
    return value
  }

  readInt32() {
    const value = new DataView(this.buffer).getInt32(this.index)
    this.index += 4
    return value
  }

  readDouble16(scale: number) {
    return this.readInt16() / scale
  }

  readDouble32(scale: number) {
    return this.readInt32() / scale
  }
}
