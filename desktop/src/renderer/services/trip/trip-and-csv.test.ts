import { beforeEach, describe, expect, test } from 'vitest'
import type { AdcSample, BoardSample, GpsFix } from '../../../protocol'
import { useVehicleStore } from '../../store/vehicle'
import { tripCsv, useTripStore } from '../../store/trip'
import { csvColumns, csvFileName, toCsv } from './csv'
import { firstTrip, followCounters, noCounters, readCounters, startTrip, tripRows, type Counters } from './trip'

const board = (fields: Partial<BoardSample> = {}): BoardSample => ({
  received_at: Date.UTC(2026, 8, 26, 11, 22, 9, 350),
  speed_m_s: 8.5,
  distance_m: 1200.5,
  distance_abs_m: 1250.25,
  erpm: 4200,
  duty_cycle: 0.42,
  battery_voltage_v: 48.3,
  battery_current_a: 4.17,
  motor_current_a: 6.25,
  battery_level: 0.873,
  energy_used_wh: 12.3456,
  energy_charged_wh: 0.5,
  charge_used_ah: 0.2567,
  charge_charged_ah: 0.0102,
  mosfet_temp_c: 35.4,
  motor_temp_c: 41.2,
  fault_code: 0,
  odometer_m: 52341,
  board_uptime_ms: 123456,
  ...fields,
})

const adc: AdcSample = {
  received_at: 0,
  adc_level1: 0.25,
  adc_voltage1: 0.8,
  adc_level2: 0.5,
  adc_voltage2: 1.6,
}

const gps: GpsFix = {
  received_at: 0,
  gps_lat_deg: 39.8912345,
  gps_lon_deg: 32.7798765,
  gps_alt_m: 912.4,
  gps_speed_m_s: 8.3,
  gps_heading_deg: 271.05,
  gps_accuracy_m: 3.2,
  gps_fix_time_utc: Date.UTC(2026, 8, 26, 11, 22, 8, 0),
}

const HOUR = 3_600_000

const counters = (distance_abs_m: number, energy_used_wh: number, energy_charged_wh: number): Counters => ({
  distance_abs_m,
  energy_used_wh,
  energy_charged_wh,
})

describe('trip-and-csv: the trip meter', () => {
  test('before the first new trip the baseline is zero and no time has passed', () => {
    expect(tripRows(firstTrip, counters(500, 10, 0), 5000)).toEqual({
      distance_m: 500,
      elapsed_ms: 0,
      avg_speed_kmh: 0,
      consumption_km_kwh: 50,
    })
  })

  test('a new trip sets the baseline to the current values and the start time to now', () => {
    const trip = startTrip(counters(1000, 20, 2), 1000)
    expect(trip).toEqual({ baseline: counters(1000, 20, 2), startedAt: 1000 })
    expect(tripRows(trip, counters(1000, 20, 2), 1000)).toEqual({
      distance_m: 0,
      elapsed_ms: 0,
      avg_speed_kmh: 0,
      consumption_km_kwh: 0,
    })
  })

  test('the rows are the current values minus the baseline', () => {
    const trip = startTrip(counters(1000, 20, 2), 0)
    const rows = tripRows(trip, counters(31_000, 120, 12), HOUR)
    // 30 km in an hour, on 100 Wh used less 10 Wh charged.
    expect(rows.distance_m).toBe(30_000)
    expect(rows.elapsed_ms).toBe(HOUR)
    expect(rows.avg_speed_kmh).toBeCloseTo(30)
    expect(rows.consumption_km_kwh).toBeCloseTo(30_000 / 90)
  })

  test('the current values are read from each board sample', () => {
    expect(readCounters(noCounters, board({ distance_abs_m: 5, energy_used_wh: 6, energy_charged_wh: 7 }))).toEqual(
      counters(5, 6, 7)
    )
  })

  test('a value missing from a short reply keeps its last value', () => {
    const short = board({ energy_used_wh: 30 })
    delete short.distance_abs_m
    delete short.energy_charged_wh
    expect(readCounters(counters(1000, 20, 2), short)).toEqual(counters(1000, 30, 2))
  })

  test('a value falling below its baseline moves that baseline to it', () => {
    const trip = startTrip(counters(1000, 20, 2), 0)
    // The board was power-cycled: its counters start again from near zero.
    expect(followCounters(trip, counters(3, 0.1, 2.5))).toEqual({ baseline: counters(3, 0.1, 2), startedAt: 0 })
  })

  test('the trip is unchanged while no value falls below its baseline', () => {
    const trip = startTrip(counters(1000, 20, 2), 0)
    expect(followCounters(trip, counters(2000, 20, 2))).toBe(trip)
  })

  test('no row goes negative', () => {
    const trip = startTrip(counters(1000, 20, 2), 1000)
    const lower = counters(3, 0.1, 0)
    for (const value of Object.values(tripRows(trip, lower, 0))) expect(value).toBeGreaterThanOrEqual(0)
    const followed = followCounters(trip, lower)
    for (const value of Object.values(tripRows(followed, lower, 2000))) expect(value).toBeGreaterThanOrEqual(0)
  })

  test('more energy charged than used reads as no consumption', () => {
    const rows = tripRows(startTrip(noCounters, 0), counters(100, 1, 3), HOUR)
    expect(rows.consumption_km_kwh).toBe(0)
  })
})

describe('trip-and-csv: the CSV export', () => {
  test("the columns are the log of record's, without elapsed_s", () => {
    expect(csvColumns).toEqual([
      'time_utc',
      'speed_m_s',
      'distance_m',
      'distance_abs_m',
      'erpm',
      'duty_cycle',
      'battery_voltage_v',
      'battery_current_a',
      'motor_current_a',
      'power_w',
      'battery_level',
      'energy_used_wh',
      'energy_charged_wh',
      'charge_used_ah',
      'charge_charged_ah',
      'mosfet_temp_c',
      'motor_temp_c',
      'fault_code',
      'adc_level1',
      'adc_level2',
      'odometer_m',
      'board_uptime_ms',
      'gps_fix_time_utc',
      'gps_lat_deg',
      'gps_lon_deg',
      'gps_alt_m',
      'gps_speed_m_s',
      'gps_heading_deg',
      'gps_accuracy_m',
    ])
  })

  test('the first line is the column names and each row is one board sample', () => {
    const lines = toCsv([{ board: board(), adc, gps }]).trimEnd().split('\n')
    expect(lines[0]).toBe(csvColumns.join(','))
    expect(lines[1]).toBe(
      [
        '2026-09-26T11:22:09.350Z',
        '8.5',
        '1200.5',
        '1250.25',
        '4200',
        '0.42',
        '48.3',
        '4.17',
        '6.25',
        '201.41',
        '0.873',
        '12.3456',
        '0.5',
        '0.2567',
        '0.0102',
        '35.4',
        '41.2',
        '0',
        '0.25',
        '0.5',
        '52341',
        '123456',
        '2026-09-26T11:22:08.000Z',
        '39.8912345',
        '32.7798765',
        '912.40',
        '8.30',
        '271.05',
        '3.2',
      ].join(',')
    )
  })

  test('GPS cells are empty with no fix', () => {
    const [, row] = toCsv([{ board: board(), adc, gps: null }]).trimEnd().split('\n')
    const cells = row.split(',')
    for (const column of csvColumns.filter((c) => c.startsWith('gps_'))) {
      expect(cells[csvColumns.indexOf(column)]).toBe('')
    }
    expect(cells[csvColumns.indexOf('adc_level1')]).toBe('0.25')
  })

  test('a field missing from a short reply, and power without its inputs, are empty cells', () => {
    const short = board()
    delete short.battery_current_a
    delete short.board_uptime_ms
    const [, row] = toCsv([{ board: short, adc: null, gps: null }]).trimEnd().split('\n')
    const cells = row.split(',')
    expect(cells).toHaveLength(csvColumns.length)
    for (const column of ['battery_current_a', 'power_w', 'board_uptime_ms', 'adc_level1', 'adc_level2']) {
      expect(cells[csvColumns.indexOf(column)]).toBe('')
    }
  })

  test("the default file name is redLINK_ and the laptop's local time", () => {
    expect(csvFileName(new Date(2026, 8, 26, 14, 22, 9).getTime())).toBe('redLINK_2026-09-26_14-22-09.csv')
  })
})

describe('trip-and-csv: rows held for the current trip', () => {
  beforeEach(() => {
    useVehicleStore.setState({ board: null, adc: null, gps: null })
    useTripStore.setState({ counters: noCounters })
    useTripStore.getState().newTrip()
  })

  test('one row per board sample, with the latest ADC sample and GPS fix beside it', () => {
    useVehicleStore.setState({ adc })
    useVehicleStore.setState({ board: board({ erpm: 1 }) })
    useVehicleStore.setState({ gps })
    useVehicleStore.setState({ adc: { ...adc, adc_level1: 0.75 } })
    useVehicleStore.setState({ board: board({ erpm: 2 }) })

    const lines = tripCsv()!.trimEnd().split('\n')
    expect(lines).toHaveLength(3)
    const cell = (line: string, column: string) => line.split(',')[csvColumns.indexOf(column)]
    expect(cell(lines[1], 'erpm')).toBe('1')
    expect(cell(lines[1], 'adc_level1')).toBe('0.25')
    expect(cell(lines[1], 'gps_lat_deg')).toBe('')
    expect(cell(lines[2], 'erpm')).toBe('2')
    expect(cell(lines[2], 'adc_level1')).toBe('0.75')
    expect(cell(lines[2], 'gps_lat_deg')).toBe('39.8912345')
  })

  test('with no rows there is nothing to save', () => {
    useVehicleStore.setState({ adc, gps })
    expect(tripCsv()).toBeNull()
  })

  test('a new trip sets the baseline and clears the rows', () => {
    useVehicleStore.setState({ board: board({ distance_abs_m: 1000, energy_used_wh: 20, energy_charged_wh: 2 }) })
    expect(tripCsv()).not.toBeNull()

    useTripStore.getState().newTrip()
    expect(tripCsv()).toBeNull()
    expect(useTripStore.getState().trip.baseline).toEqual(counters(1000, 20, 2))
  })

  test('a new trip on a short reply keeps the last value of a missing counter', () => {
    useVehicleStore.setState({ board: board({ distance_abs_m: 1000, energy_used_wh: 20, energy_charged_wh: 2 }) })
    const short = board({ energy_used_wh: 21 })
    delete short.distance_abs_m
    useVehicleStore.setState({ board: short })
    useTripStore.getState().newTrip()
    expect(useTripStore.getState().trip.baseline).toEqual(counters(1000, 21, 0.5))
  })

  test('a board sample below the baseline moves it', () => {
    useVehicleStore.setState({ board: board({ distance_abs_m: 1000, energy_used_wh: 20, energy_charged_wh: 2 }) })
    useTripStore.getState().newTrip()
    useVehicleStore.setState({ board: board({ distance_abs_m: 3, energy_used_wh: 0.1, energy_charged_wh: 0 }) })
    expect(useTripStore.getState().trip.baseline).toEqual(counters(3, 0.1, 0))
  })
})
