import { create } from 'zustand'
import { useVehicleStore } from './vehicle'
import { firstTrip, followCounters, noCounters, readCounters, startTrip, type Counters, type Trip } from '../services/trip/trip'
import { csvFileName, toCsv, type CsvRow } from '../services/trip/csv'

// This laptop's own trip, in memory only: lost when the app quits, and kept
// across phone lost, rejoining and a switch between the hub and a direct link.
interface TripState {
  trip: Trip
  // The board's latest counters, each kept through short replies that lack it.
  counters: Counters
  // Sets the baseline to the current values and the start time to now, and
  // clears the CSV rows.
  newTrip: () => void
}

// The CSV rows of the current trip. Not part of the store's state, so 20
// samples a second do not re-render the trip meter.
let csvRows: CsvRow[] = []

export const useTripStore = create<TripState>()((set, get) => ({
  trip: firstTrip,
  counters: noCounters,
  newTrip: () => {
    csvRows = []
    set({ trip: startTrip(get().counters, Date.now()) })
  },
}))

useVehicleStore.subscribe(({ board, adc, gps }, previous) => {
  if (!board || board === previous.board) return
  csvRows.push({ board, adc, gps })
  const state = useTripStore.getState()
  const counters = readCounters(state.counters, board)
  useTripStore.setState({ counters, trip: followCounters(state.trip, counters) })
})

// The current trip as CSV, or null with no rows.
export const tripCsv = () => (csvRows.length === 0 ? null : toCsv(csvRows))

// Opens the save dialog through main. With no rows nothing happens, and
// saving does not clear the rows.
export const saveTripCsv = () => {
  const csv = tripCsv()
  if (csv !== null) window.redlink.saveCsv(csvFileName(Date.now()), csv).catch(console.error)
}
