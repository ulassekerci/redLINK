import { useEffect } from 'react'
import { saveTripCsv, useTripStore } from '../store/trip'

const isMac = navigator.userAgent.includes('Mac')

const inTextField = (target: EventTarget | null) =>
  target instanceof HTMLElement && (target.isContentEditable || target.matches('input, textarea, select'))

// On every screen: Space starts a new trip, except while a text field has the
// focus, and Cmd+S on macOS or Ctrl+S on Windows saves the trip's CSV.
export const useTripKeys = () => {
  useEffect(() => {
    const handleKey = (e: KeyboardEvent) => {
      if (e.code === 'Space' && !inTextField(e.target)) {
        // Or a focused button would be pressed as well.
        e.preventDefault()
        useTripStore.getState().newTrip()
      } else if (e.key.toLowerCase() === 's' && (isMac ? e.metaKey : e.ctrlKey)) {
        e.preventDefault()
        saveTripCsv()
      }
    }
    addEventListener('keydown', handleKey)
    return () => removeEventListener('keydown', handleKey)
  }, [])
}
