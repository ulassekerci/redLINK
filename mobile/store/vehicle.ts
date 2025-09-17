import { create } from 'zustand'
import { LocationData } from '../services/location/interfaces'

export interface VehicleState {
  temp: { mosfet: number }
  current: { motor: number; battery: number }
  dutyCycle: number
  erpm: number
  voltage: number
  wattHours: { consumed: number; charged: number }
  tachometer: { value: number; abs: number }
  adc: { level1: number; voltage1: number; level2: number; voltage2: number }
  faultCode: number
  location: LocationData | null

  clear: () => void
}

export const useVehicleStore = create<VehicleState>()((set) => ({
  temp: { mosfet: 0 },
  current: { motor: 0, battery: 0 },
  dutyCycle: 0,
  erpm: 0,
  voltage: 0,
  wattHours: { consumed: 0, charged: 0 },
  tachometer: { value: 0, abs: 0 },
  adc: { level1: 0, voltage1: 0, level2: 0, voltage2: 0 },
  faultCode: 0,
  location: null,

  clear: () => {
    set({
      temp: { mosfet: 0 },
      current: { motor: 0, battery: 0 },
      dutyCycle: 0,
      erpm: 0,
      voltage: 0,
      wattHours: { consumed: 0, charged: 0 },
      tachometer: { value: 0, abs: 0 },
      adc: { level1: 0, voltage1: 0, level2: 0, voltage2: 0 },
      faultCode: 0,
      location: null,
    })
  },
}))
