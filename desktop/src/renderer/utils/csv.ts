import { DateTime } from 'luxon'
import { useVehicleStore, type VehicleState } from '../store/vehicle'

// One row per board sample, with the latest ADC sample and GPS fix beside it.
export const vehicleLog: VehicleState[] = []

useVehicleStore.subscribe((state, previous) => {
  if (state.board && state.board !== previous.board) vehicleLog.push(state)
})

export const clearLog = () => (vehicleLog.length = 0)

export const downloadCSV = () => {
  if (vehicleLog.length === 0) return

  // flatten the state object
  const csvData = vehicleLog.map(({ board, adc, gps }) => ({
    time_utc: board?.received_at,
    speed_m_s: board?.speed_m_s,
    power_w: (board?.battery_voltage_v ?? 0) * (board?.battery_current_a ?? 0),
    distance_abs_m: board?.distance_abs_m,
    motor_current_a: board?.motor_current_a,
    battery_current_a: board?.battery_current_a,
    duty_cycle: board?.duty_cycle,
    mosfet_temp_c: board?.mosfet_temp_c,
    erpm: board?.erpm,
    battery_voltage_v: board?.battery_voltage_v,
    energy_used_wh: board?.energy_used_wh,
    energy_charged_wh: board?.energy_charged_wh,
    adc_level1: adc?.adc_level1,
    adc_level2: adc?.adc_level2,
    fault_code: board?.fault_code,
    gps_lat_deg: gps?.gps_lat_deg,
    gps_lon_deg: gps?.gps_lon_deg,
    gps_accuracy_m: gps?.gps_accuracy_m,
    gps_fix_time_utc: gps?.gps_fix_time_utc,
  }))

  // Get CSV headers
  const headers = Object.keys(csvData[0])

  // Build CSV string; a missing value is an empty cell
  const csvRows = [
    headers.join(','), // header row
    ...csvData.map((row) => headers.map((h) => row[h as keyof typeof row] ?? '').join(',')),
  ]
  const csvString = csvRows.join('\n')

  // Create blob and download
  const blob = new Blob([csvString], { type: 'text/csv' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `redLINK_Log_${DateTime.now().toFormat('LLLdd_HHmm')}.csv`
  a.click()
  URL.revokeObjectURL(url)
}
