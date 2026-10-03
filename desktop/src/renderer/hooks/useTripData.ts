import { Duration } from 'luxon'
import { useEffect, useState } from 'react'
import { useTripStore } from '../store/trip'
import { tripRows } from '../services/trip/trip'

// The trip meter's rows as shown, refreshed at least once a second for the
// time row.
export const useTripData = () => {
  const { trip, counters } = useTripStore()
  const [, forceRender] = useState(0)

  useEffect(() => {
    const timer = setInterval(() => forceRender(Date.now()), 1000)
    return () => clearInterval(timer)
  }, [])

  const rows = tripRows(trip, counters, Date.now())

  return {
    distanceString: Math.round(rows.distance_m) + ' m',
    timeString: Duration.fromMillis(rows.elapsed_ms).toFormat('hh:mm:ss'),
    avgSpeedString: rows.avg_speed_kmh.toFixed(1) + ' km/h',
    consumptionString: Math.round(rows.consumption_km_kwh) + ' km/kWh',
  }
}
