import { create } from 'zustand'

export const useTelemetryStore = create()((set) => ({
  data: {
    temp: { mosfet: 0.0, motor: 0.0 },
    current: { motor: 0.0, battery: 0.0 },
    id: 0.0,
    iq: 0.0,
    dutyCycle: 0.0,
    erpm: 0.0,
    voltage: 0.0,
    ampHours: { consumed: 0.0, charged: 0.0 },
    wattHours: { consumed: 0.0, charged: 0.0 },
    tachometer: { value: 0.0, abs: 0.0 },
    faultCode: 0,
  },
  timestamp: 0,
}))
