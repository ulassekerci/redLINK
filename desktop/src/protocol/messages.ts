import { encodeFrame } from './framing'
import { isToken } from './team-code'

export const COMM_FW_VERSION = 0
export const COMM_GET_DECODED_ADC = 32
export const COMM_CUSTOM_APP_DATA = 36
export const COMM_GET_VALUES_SETUP = 47

const GPS = 1
const LOBBY_REQUEST = 2
const HEARTBEAT = 3
const STATUS = 4

// Field names are snake_case on purpose: they are the log of record's column
// names, and the names protocol/vectors.json uses.

// Every field of a COMM_GET_VALUES_SETUP reply. A short reply is missing its
// tail, so each field is present only when the reply reached it.
export type ValuesSetup = {
  type: 'values_setup'
  mosfet_temp_c?: number
  motor_temp_c?: number
  motor_current_a?: number
  battery_current_a?: number
  duty_cycle?: number
  erpm?: number
  speed_m_s?: number
  battery_voltage_v?: number
  battery_level?: number
  charge_used_ah?: number
  charge_charged_ah?: number
  energy_used_wh?: number
  energy_charged_wh?: number
  distance_m?: number
  distance_abs_m?: number
  position?: number
  fault_code?: number
  board_id?: number
  board_count?: number
  battery_capacity_wh?: number
  odometer_m?: number
  board_uptime_ms?: number
}

export type DecodedAdc = {
  type: 'decoded_adc'
  adc_level1: number
  adc_voltage1: number
  adc_level2: number
  adc_voltage2: number
}

export type FwVersion = { type: 'fw_version'; fw_major: number; fw_minor: number }

// A field the phone's fix lacks arrives as 0. The fix time is Unix milliseconds.
export type Gps = {
  type: 'gps'
  gps_lat_deg: number
  gps_lon_deg: number
  gps_alt_m: number
  gps_speed_m_s: number
  gps_heading_deg: number
  gps_accuracy_m: number
  gps_fix_time_utc: number
}

export type LobbyRequest = { type: 'lobby_request'; token: string }

export type Heartbeat = { type: 'heartbeat' }

// board_state: 0 answering, 1 unreachable.
export type Status = { type: 'status'; protocol_version: number; board_state: number }

export type DecodedMessage = ValuesSetup | DecodedAdc | FwVersion | Gps | LobbyRequest | Heartbeat | Status

export type EncodableMessage =
  | { type: 'fw_version_request' }
  | { type: 'decoded_adc_request' }
  | { type: 'values_setup_request' }
  | Gps
  | LobbyRequest
  | Heartbeat
  | Status

type Encoding = 'i8' | 'u8' | 'i16' | 'i32' | 'u32'
const sizes: Record<Encoding, number> = { i8: 1, u8: 1, i16: 2, i32: 4, u32: 4 }

const valuesSetupFields: [keyof Omit<ValuesSetup, 'type'>, Encoding, number][] = [
  ['mosfet_temp_c', 'i16', 10],
  ['motor_temp_c', 'i16', 10],
  ['motor_current_a', 'i32', 100],
  ['battery_current_a', 'i32', 100],
  ['duty_cycle', 'i16', 1000],
  ['erpm', 'i32', 1],
  ['speed_m_s', 'i32', 1000],
  ['battery_voltage_v', 'i16', 10],
  ['battery_level', 'i16', 1000],
  ['charge_used_ah', 'i32', 10000],
  ['charge_charged_ah', 'i32', 10000],
  ['energy_used_wh', 'i32', 10000],
  ['energy_charged_wh', 'i32', 10000],
  ['distance_m', 'i32', 1000],
  ['distance_abs_m', 'i32', 1000],
  ['position', 'i32', 1000000],
  ['fault_code', 'i8', 1],
  ['board_id', 'u8', 1],
  ['board_count', 'u8', 1],
  ['battery_capacity_wh', 'i32', 1000],
  ['odometer_m', 'u32', 1],
  ['board_uptime_ms', 'u32', 1],
]

function read(view: DataView, offset: number, encoding: Encoding) {
  switch (encoding) {
    case 'i8':
      return view.getInt8(offset)
    case 'u8':
      return view.getUint8(offset)
    case 'i16':
      return view.getInt16(offset)
    case 'i32':
      return view.getInt32(offset)
    case 'u32':
      return view.getUint32(offset)
  }
}

// Decodes a frame's payload. Returns null for anything this protocol does not
// handle: an unknown command ID, or a reply too short to carry its fields.
// Bytes after the last field read are ignored.
export function decodeMessage(payload: Uint8Array): DecodedMessage | null {
  const view = new DataView(payload.buffer, payload.byteOffset, payload.byteLength)
  switch (payload[0]) {
    case COMM_GET_VALUES_SETUP:
      return decodeValuesSetup(view)
    case COMM_GET_DECODED_ADC:
      if (view.byteLength < 17) return null
      return {
        type: 'decoded_adc',
        adc_level1: view.getInt32(1) / 1e6,
        adc_voltage1: view.getInt32(5) / 1e6,
        adc_level2: view.getInt32(9) / 1e6,
        adc_voltage2: view.getInt32(13) / 1e6,
      }
    case COMM_FW_VERSION:
      if (view.byteLength < 3) return null
      return { type: 'fw_version', fw_major: view.getUint8(1), fw_minor: view.getUint8(2) }
    case COMM_CUSTOM_APP_DATA:
      return decodeCustomAppData(view)
    default:
      return null
  }
}

// Our messages: [36, type, ...content].
function decodeCustomAppData(view: DataView): DecodedMessage | null {
  if (view.byteLength < 2) return null
  const content = view.byteLength - 2
  switch (view.getUint8(1)) {
    case GPS:
      if (content < 26) return null
      return {
        type: 'gps',
        gps_lat_deg: view.getInt32(2) / 1e7,
        gps_lon_deg: view.getInt32(6) / 1e7,
        gps_alt_m: view.getInt32(10) / 100,
        gps_speed_m_s: view.getUint16(14) / 100,
        gps_heading_deg: view.getUint16(16) / 100,
        gps_accuracy_m: view.getUint16(18) / 10,
        gps_fix_time_utc: Number(view.getBigUint64(20)),
      }
    case LOBBY_REQUEST: {
      if (content < 8) return null
      const token = String.fromCharCode(...new Uint8Array(view.buffer, view.byteOffset + 2, 8))
      return isToken(token) ? { type: 'lobby_request', token } : null
    }
    case HEARTBEAT:
      return { type: 'heartbeat' }
    case STATUS:
      if (content < 2) return null
      return { type: 'status', protocol_version: view.getUint8(2), board_state: view.getUint8(3) }
    default:
      return null
  }
}

// Scales a value to its wire integer, refusing one the field cannot carry
// rather than letting it wrap.
function scaled(value: number, scale: number, encoding: 'i32' | 'u16', field: string) {
  const integer = Math.round(value * scale)
  const [min, max] = encoding === 'i32' ? [-0x80000000, 0x7fffffff] : [0, 0xffff]
  if (!(integer >= min && integer <= max)) throw new RangeError(`${field} out of range: ${value}`)
  return integer
}

// A fix whose accuracy is past what its field carries is not a fix: this
// throws for it, and the bridge sends nothing.
function encodeGps(message: Gps) {
  const view = new DataView(new ArrayBuffer(28))
  view.setUint8(0, COMM_CUSTOM_APP_DATA)
  view.setUint8(1, GPS)
  view.setInt32(2, scaled(message.gps_lat_deg, 1e7, 'i32', 'gps_lat_deg'))
  view.setInt32(6, scaled(message.gps_lon_deg, 1e7, 'i32', 'gps_lon_deg'))
  view.setInt32(10, scaled(message.gps_alt_m, 100, 'i32', 'gps_alt_m'))
  view.setUint16(14, scaled(message.gps_speed_m_s, 100, 'u16', 'gps_speed_m_s'))
  view.setUint16(16, scaled(message.gps_heading_deg, 100, 'u16', 'gps_heading_deg'))
  view.setUint16(18, scaled(message.gps_accuracy_m, 10, 'u16', 'gps_accuracy_m'))
  view.setBigUint64(20, BigInt(message.gps_fix_time_utc))
  return new Uint8Array(view.buffer)
}

function decodeValuesSetup(view: DataView) {
  const message: ValuesSetup = { type: 'values_setup' }
  let offset = 1
  for (const [name, encoding, scale] of valuesSetupFields) {
    if (offset + sizes[encoding] > view.byteLength) break
    message[name] = read(view, offset, encoding) / scale
    offset += sizes[encoding]
  }
  return message
}

// Encodes a message as a whole frame, ready to write.
export function encodeMessage(message: EncodableMessage) {
  switch (message.type) {
    case 'fw_version_request':
      return encodeFrame(Uint8Array.of(COMM_FW_VERSION))
    case 'decoded_adc_request':
      return encodeFrame(Uint8Array.of(COMM_GET_DECODED_ADC))
    case 'values_setup_request':
      return encodeFrame(Uint8Array.of(COMM_GET_VALUES_SETUP))
    case 'gps':
      return encodeFrame(encodeGps(message))
    case 'lobby_request': {
      if (!isToken(message.token)) throw new Error(`Not a token: ${message.token}`)
      const token = Array.from(message.token, (char) => char.charCodeAt(0))
      return encodeFrame(Uint8Array.of(COMM_CUSTOM_APP_DATA, LOBBY_REQUEST, ...token))
    }
    case 'heartbeat':
      return encodeFrame(Uint8Array.of(COMM_CUSTOM_APP_DATA, HEARTBEAT))
    case 'status':
      return encodeFrame(Uint8Array.of(COMM_CUSTOM_APP_DATA, STATUS, message.protocol_version, message.board_state))
  }
}
