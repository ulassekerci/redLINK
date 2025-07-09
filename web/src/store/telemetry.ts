import { create } from 'zustand'

export interface TelemetryState {
  temp: { mosfet: number; motor: number }
  current: { motor: number; battery: number }
  id: number
  iq: number
  dutyCycle: number
  erpm: number
  voltage: number
  ampHours: { consumed: number; charged: number }
  wattHours: { consumed: number; charged: number }
  tachometer: { value: number; abs: number }
  faultCode: number
}

export const useTelemetryStore = create<TelemetryState>()(() => ({
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
}))
