import { DateTime } from 'luxon'
import { useTripStore } from '../store/trip'
import { useVehicleData } from './useVehicleData'
import { useEffect, useState } from 'react'

export const useTripData = () => {
  const data = useVehicleData()
  const trip = useTripStore()
  const [_, setNow] = useState<number>(0)

  const distance = data.distance - trip.distanceBeforeTrip
  const avgSpeed = getAvgSpeed(distance, trip.timeStarted)
  const whCharge = data.wattHours.charged - trip.whChargeBeforeTrip
  const whConsume = data.wattHours.consumed - trip.whConsumeBeforeTrip
  const consumption = calculateConsumption(distance, whConsume, whCharge)

  const distanceString = distance + ' m'
  const timeString = getTimeString(trip.timeStarted)
  const avgSpeedString = avgSpeed + ' km/h'
  const consumptionString = consumption + ' km/kWh'

  const handleSpace = (e: KeyboardEvent) => {
    if (e.code !== 'Space') return
    trip.newTrip()
  }

  useEffect(() => {
    addEventListener('keypress', handleSpace)
    const timer = setInterval(() => setNow(Date.now()), 1000)
    return () => {
      removeEventListener('keydown', handleSpace)
      clearInterval(timer)
    }
  }, [])

  return { distanceString, timeString, avgSpeedString, consumptionString }
}

const getTimeString = (tripStart: DateTime | null) => {
  if (!tripStart) return '00:00:00'
  const diff = DateTime.now().diff(tripStart, ['hours', 'minutes', 'seconds'])
  return diff.toFormat('hh:mm:ss')
}

const getAvgSpeed = (distance: number, tripStart: DateTime | null) => {
  if (!tripStart) return 0
  const diffHr = DateTime.now().diff(tripStart, 'hours').hours
  if (diffHr === 0) return 0 // prevent divison by zero
  const distanceKm = distance / 1000
  const speed = distanceKm / diffHr
  return Math.round(speed)
}

const calculateConsumption = (distance: number, whConsume: number, whCharge: number) => {
  const whAbs = whConsume - whCharge
  if (whAbs === 0) return 0
  return Math.round(distance / whAbs)
}
