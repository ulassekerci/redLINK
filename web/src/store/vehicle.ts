import { create } from 'zustand'

export interface VehicleState {
  temp: { mosfet: number }
  current: { motor: number; battery: number }
  dutyCycle: number
  erpm: number
  voltage: number
  wattHours: { consumed: number; charged: number }
  tachometer: { value: number; abs: number }
  faultCode: number
}

export const useVehicleStore = create<VehicleState>()(() => ({
  temp: { mosfet: 0.0 },
  current: { motor: 0.0, battery: 0.0 },
  dutyCycle: 0.0,
  erpm: 0.0,
  voltage: 0.0,
  wattHours: { consumed: 0.0, charged: 0.0 },
  tachometer: { value: 0.0, abs: 0.0 },
  faultCode: 0,
}))
