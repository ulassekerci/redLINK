import { create } from 'zustand'
import type { AdcSample, BoardSample, GpsFix } from '../../protocol'

// The latest of each sample, under the field names of the desktop spec, 2.3.
// Each is null until the first arrives.
export interface VehicleState {
  board: BoardSample | null
  adc: AdcSample | null
  gps: GpsFix | null
}

export const useVehicleStore = create<VehicleState>()(() => ({
  board: null,
  adc: null,
  gps: null,
}))
