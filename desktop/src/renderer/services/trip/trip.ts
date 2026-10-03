import type { BoardSample } from '../../../protocol'

// The board's own counters a trip is measured from.
export interface Counters {
  distance_abs_m: number
  energy_used_wh: number
  energy_charged_wh: number
}

// A trip is a baseline of the counters plus a start time (Unix milliseconds);
// the rows are the current values minus the baseline. Before the first new
// trip the baseline is zero and there is no start time.
export interface Trip {
  baseline: Counters
  startedAt: number | null
}

export interface TripRows {
  distance_m: number
  elapsed_ms: number
  avg_speed_kmh: number
  // km/kWh, which is the same number as m/Wh.
  consumption_km_kwh: number
}

const counterNames: (keyof Counters)[] = ['distance_abs_m', 'energy_used_wh', 'energy_charged_wh']

// Before the first board sample every counter reads zero.
export const noCounters: Counters = { distance_abs_m: 0, energy_used_wh: 0, energy_charged_wh: 0 }

export const firstTrip: Trip = { baseline: noCounters, startedAt: null }

// The counters a board sample carries. A counter missing from a short reply
// keeps its previous value, so neither a new trip nor a row reads it as zero.
export function readCounters(previous: Counters, board: BoardSample): Counters {
  return {
    distance_abs_m: board.distance_abs_m ?? previous.distance_abs_m,
    energy_used_wh: board.energy_used_wh ?? previous.energy_used_wh,
    energy_charged_wh: board.energy_charged_wh ?? previous.energy_charged_wh,
  }
}

export function startTrip(current: Counters, now: number): Trip {
  return { baseline: current, startedAt: now }
}

// When the board is power-cycled its counters start again from zero. A counter
// that falls below its baseline moves the baseline down to it, so the trip
// loses what the board had counted since, and no row goes negative. Returns
// the same trip when nothing moved.
export function followCounters(trip: Trip, current: Counters): Trip {
  let baseline = trip.baseline
  for (const name of counterNames) {
    if (current[name] < baseline[name]) baseline = { ...baseline, [name]: current[name] }
  }
  return baseline === trip.baseline ? trip : { ...trip, baseline }
}

export function tripRows(trip: Trip, current: Counters, now: number): TripRows {
  const since = (name: keyof Counters) => Math.max(0, current[name] - trip.baseline[name])

  const distance = since('distance_abs_m')
  const netEnergy = since('energy_used_wh') - since('energy_charged_wh')
  const elapsed = trip.startedAt === null ? 0 : Math.max(0, now - trip.startedAt)

  return {
    distance_m: distance,
    elapsed_ms: elapsed,
    avg_speed_kmh: elapsed > 0 ? distance / 1000 / (elapsed / 3_600_000) : 0,
    consumption_km_kwh: netEnergy > 0 ? distance / netEnergy : 0,
  }
}
