import { DateTime } from 'luxon'
import { create } from 'zustand'
import { useVehicleStore } from './vehicle'
import { clearLog } from '../utils/csv'

export interface TripState {
  distanceBeforeTrip: number
  whChargeBeforeTrip: number
  whConsumeBeforeTrip: number
  timeStarted: DateTime | null
  newTrip: () => void
}

export const useTripStore = create<TripState>()((set) => ({
  distanceBeforeTrip: 0,
  whConsumeBeforeTrip: 0,
  whChargeBeforeTrip: 0,
  timeStarted: null,

  newTrip: () => {
    const { board } = useVehicleStore.getState()
    set({
      distanceBeforeTrip: board?.distance_abs_m ?? 0,
      whConsumeBeforeTrip: board?.energy_used_wh ?? 0,
      whChargeBeforeTrip: board?.energy_charged_wh ?? 0,
      timeStarted: DateTime.now(),
    })
    clearLog()
  },
}))
