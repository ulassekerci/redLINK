import type { Transport, TransportListener } from './polling'

// The Nordic UART service: we write to RX and listen on TX.
const UART_SERVICE = '6e400001-b5a3-f393-e0a9-e50e24dcca9e'
const RX_CHARACTERISTIC = '6e400002-b5a3-f393-e0a9-e50e24dcca9e'
const TX_CHARACTERISTIC = '6e400003-b5a3-f393-e0a9-e50e24dcca9e'

// Starts a scan for every board offering the Nordic UART service, with no
// name filter. Main draws the device list and answers with the pick; a cancel
// rejects.
export const requestBoard = () => navigator.bluetooth.requestDevice({ filters: [{ services: [UART_SERVICE] }] })

// The transport over Web Bluetooth to a device picked once. Each connect is a
// new GATT connection to that same device, with no new scan.
export class WebBluetoothTransport implements Transport {
  private rx: BluetoothRemoteGATTCharacteristic | null = null
  private tx: BluetoothRemoteGATTCharacteristic | null = null
  private listener: TransportListener | null = null

  constructor(private device: BluetoothDevice) {}

  async connect(listener: TransportListener) {
    this.detach()
    try {
      const server = await this.device.gatt!.connect()
      const service = await server.getPrimaryService(UART_SERVICE)
      this.rx = await service.getCharacteristic(RX_CHARACTERISTIC)
      this.tx = await service.getCharacteristic(TX_CHARACTERISTIC)
      this.listener = listener
      this.tx.addEventListener('characteristicvaluechanged', this.onValue)
      this.device.addEventListener('gattserverdisconnected', this.onDisconnected)
      await this.tx.startNotifications()
    } catch (error) {
      this.close()
      throw error
    }
  }

  async write(bytes: Uint8Array) {
    if (!this.rx) throw new Error('Not connected')
    // A copy, so the value is a plain ArrayBuffer-backed array.
    const value = new Uint8Array(bytes)
    if (this.rx.properties.writeWithoutResponse) await this.rx.writeValueWithoutResponse(value)
    else await this.rx.writeValueWithResponse(value)
  }

  close() {
    this.detach()
    if (this.device.gatt?.connected) this.device.gatt.disconnect()
  }

  private detach() {
    this.tx?.removeEventListener('characteristicvaluechanged', this.onValue)
    this.device.removeEventListener('gattserverdisconnected', this.onDisconnected)
    this.rx = null
    this.tx = null
    this.listener = null
  }

  private onValue = (event: Event) => {
    const value = (event.target as BluetoothRemoteGATTCharacteristic).value
    if (value) this.listener?.bytes(new Uint8Array(value.buffer.slice(value.byteOffset, value.byteOffset + value.byteLength)))
  }

  private onDisconnected = () => {
    const listener = this.listener
    this.detach()
    listener?.dropped()
  }
}
