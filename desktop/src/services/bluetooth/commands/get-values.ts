import type { Payload } from '../payload'

export function parseGetValues(payload: Payload) {
  const response = {
    temp: {
      mosfet: 0.0,
      motor: 0.0,
    },
    current: {
      motor: 0.0,
      battery: 0.0,
    },
    id: 0.0,
    iq: 0.0,
    dutyCycle: 0.0,
    erpm: 0.0,
    voltage: 0.0,
    ampHours: {
      consumed: 0.0,
      charged: 0.0,
    },
    wattHours: {
      consumed: 0.0,
      charged: 0.0,
    },
    tachometer: {
      value: 0.0,
      abs: 0.0,
    },
    faultCode: 0,
  }

  response.temp.mosfet = payload.readDouble16(1e1)
  response.temp.motor = payload.readDouble16(1e1)
  response.current.motor = payload.readDouble32(1e2)
  response.current.battery = payload.readDouble32(1e2)
  response.id = payload.readDouble32(1e2)
  response.iq = payload.readDouble32(1e2)
  response.dutyCycle = payload.readDouble16(1e3)
  response.erpm = payload.readDouble32(1)
  response.voltage = payload.readDouble16(1e1)
  response.ampHours.consumed = payload.readDouble32(1e4)
  response.ampHours.charged = payload.readDouble32(1e4)
  response.wattHours.consumed = payload.readDouble32(1e4)
  response.wattHours.charged = payload.readDouble32(1e4)
  response.tachometer.value = payload.readInt32()
  response.tachometer.abs = payload.readInt32()
  response.faultCode = payload.readInt8()

  return response
}
