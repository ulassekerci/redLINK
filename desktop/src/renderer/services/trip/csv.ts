import { DateTime } from 'luxon'
import type { AdcSample, BoardSample, GpsFix } from '../../../protocol'

// One row per board sample, with the latest ADC sample and GPS fix beside it.
// The GPS fix is null with no fix, and always on a direct link.
export interface CsvRow {
  board: BoardSample
  adc: AdcSample | null
  gps: GpsFix | null
}

type Cell = (row: CsvRow) => number | string | undefined

const time = (ms: number | undefined) => (ms === undefined ? undefined : new Date(ms).toISOString())
const fixed = (value: number | undefined, decimals: number) => value?.toFixed(decimals)

// The log of record's columns, names, units and order (Android spec, 2.8),
// with the desktop spec's differences (2.10): time_utc is the arrival time of
// the board sample, elapsed_s is left out, and power_w is computed here.
const columns = {
  time_utc: ({ board }) => time(board.received_at),
  speed_m_s: ({ board }) => board.speed_m_s,
  distance_m: ({ board }) => board.distance_m,
  distance_abs_m: ({ board }) => board.distance_abs_m,
  erpm: ({ board }) => board.erpm,
  duty_cycle: ({ board }) => board.duty_cycle,
  battery_voltage_v: ({ board }) => board.battery_voltage_v,
  battery_current_a: ({ board }) => board.battery_current_a,
  motor_current_a: ({ board }) => board.motor_current_a,
  power_w: ({ board: { battery_voltage_v: v, battery_current_a: i } }) =>
    v === undefined || i === undefined ? undefined : fixed(v * i, 2),
  battery_level: ({ board }) => board.battery_level,
  energy_used_wh: ({ board }) => board.energy_used_wh,
  energy_charged_wh: ({ board }) => board.energy_charged_wh,
  charge_used_ah: ({ board }) => board.charge_used_ah,
  charge_charged_ah: ({ board }) => board.charge_charged_ah,
  mosfet_temp_c: ({ board }) => board.mosfet_temp_c,
  motor_temp_c: ({ board }) => board.motor_temp_c,
  fault_code: ({ board }) => board.fault_code,
  adc_level1: ({ adc }) => adc?.adc_level1,
  adc_level2: ({ adc }) => adc?.adc_level2,
  odometer_m: ({ board }) => board.odometer_m,
  board_uptime_ms: ({ board }) => board.board_uptime_ms,
  gps_fix_time_utc: ({ gps }) => time(gps?.gps_fix_time_utc),
  gps_lat_deg: ({ gps }) => fixed(gps?.gps_lat_deg, 7),
  gps_lon_deg: ({ gps }) => fixed(gps?.gps_lon_deg, 7),
  gps_alt_m: ({ gps }) => fixed(gps?.gps_alt_m, 2),
  gps_speed_m_s: ({ gps }) => fixed(gps?.gps_speed_m_s, 2),
  gps_heading_deg: ({ gps }) => fixed(gps?.gps_heading_deg, 2),
  gps_accuracy_m: ({ gps }) => fixed(gps?.gps_accuracy_m, 1),
} satisfies Record<string, Cell>

const columnNames = Object.keys(columns) as (keyof typeof columns)[]
export const csvColumns: readonly string[] = columnNames

// Comma-separated with decimal points, the column names as the first line, and
// an empty cell for a missing value.
export function toCsv(rows: CsvRow[]) {
  const lines = [columnNames.join(',')]
  for (const row of rows) lines.push(columnNames.map((column) => columns[column](row) ?? '').join(','))
  return lines.join('\n') + '\n'
}

// redLINK_2026-09-26_14-22-09.csv, in the laptop's local time.
export const csvFileName = (now: number) => `redLINK_${DateTime.fromMillis(now).toFormat('yyyy-LL-dd_HH-mm-ss')}.csv`
