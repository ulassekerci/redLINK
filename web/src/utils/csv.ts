import { DateTime } from 'luxon'
import { useVehicleStore, type VehicleState } from '../store/vehicle'
import { calculateDistance, calculateSpeed } from './erpm'

export const vehicleLog: { timestamp: number; data: VehicleState }[] = []

const updateLog = (state: VehicleState) => {
  vehicleLog.push({ timestamp: Date.now(), data: state })
}

useVehicleStore.subscribe(updateLog)

export const clearLog = () => (vehicleLog.length = 0)

export const downloadCSV = () => {
  if (vehicleLog.length === 0) return

  const flatten = (row: VehicleState & { timestamp: number }) => ({
    timestamp: row.timestamp,
    speed_kmh: calculateSpeed(row.erpm),
    power_w: row.voltage * row.current.battery,
    distance_m: calculateDistance(row.tachometer.abs),
    current_motor: row.current.motor,
    current_battery: row.current.battery,
    dutyCycle: row.dutyCycle,
    temp_mosfet: row.temp.mosfet,
    erpm: row.erpm,
    voltage: row.voltage,
    wattHours_consumed: row.wattHours.consumed,
    wattHours_charged: row.wattHours.charged,
    tachometer_value: row.tachometer.value,
    tachometer_abs: row.tachometer.abs,
    adc_level1: row.adc.level1,
    adc_level2: row.adc.level2,
    faultCode: row.faultCode,
    ...(row.location && {
      gps_lat: row.location.coords.latitude,
      gps_lng: row.location.coords.longitude,
      gps_accuracy: row.location.coords.accuracy.toFixed(2),
      gps_timestamp: row.location.timestamp.toFixed(0),
    }),
  })

  const flatData = vehicleLog.map((entry) => flatten({ ...entry.data, timestamp: entry.timestamp }))

  // Get CSV headers
  const headers = Object.keys(flatData[0])

  // Build CSV string
  const csvRows = [
    headers.join(','), // header row
    ...flatData.map((row) => headers.map((h) => row[h as keyof typeof row]).join(',')),
  ]
  const csvString = csvRows.join('\n')

  // Create Blob and trigger download
  const blob = new Blob([csvString], { type: 'text/csv' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `redLINK_Log_${DateTime.now().toFormat('LLLdd_HHmm')}.csv`
  a.click()
  URL.revokeObjectURL(url)
}
