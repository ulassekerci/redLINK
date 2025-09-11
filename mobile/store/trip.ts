import { DateTime } from 'luxon'
import { create } from 'zustand'
import { useVehicleStore } from './vehicle'
import { calculateDistance } from '../utils/erpm'

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
    const vehicleState = useVehicleStore.getState()
    set({
      distanceBeforeTrip: calculateDistance(vehicleState.tachometer.abs),
      whConsumeBeforeTrip: vehicleState.wattHours.consumed,
      whChargeBeforeTrip: vehicleState.wattHours.charged,
      timeStarted: DateTime.now(),
    })
  },
}))
