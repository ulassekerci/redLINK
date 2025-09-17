import { useEffect } from 'react'
import { useVehicleStore } from '../store/vehicle'
import { calculateDistance, calculateSpeed } from '../utils/erpm'
import { downloadCSV } from '../utils/csv'

export const useVehicleData = () => {
  const bleData = useVehicleStore()

  useEffect(() => {
    addEventListener('keydown', handleSave)
    return () => removeEventListener('keydown', handleSave)
  }, [])

  const handleSave = (e: KeyboardEvent) => {
    if ((e.ctrlKey || e.metaKey) && e.key === 's') {
      e.preventDefault()
      downloadCSV()
    }
  }

  return {
    ...bleData,
    speed: calculateSpeed(bleData.erpm),
    power: bleData.voltage * bleData.current.battery,
    distance: calculateDistance(bleData.tachometer.abs),
    voltage: {
      battery: bleData.voltage,
      motor: bleData.voltage * bleData.dutyCycle,
    },
    wattHours: {
      ...bleData.wattHours,
      abs: bleData.wattHours.consumed - bleData.wattHours.charged,
    },
  }
}
