import { useVehicleStore } from '../store/vehicle'
import { calculateDistance, calculateSpeed } from '../utils/erpm'

export const useVehicleData = () => {
  const bleData = useVehicleStore()

  return {
    ...bleData,
    speed: calculateSpeed(bleData.erpm),
    power: Math.round(bleData.voltage * bleData.current.battery),
    distance: calculateDistance(bleData.tachometer.abs),
    voltage: { battery: bleData.voltage, motor: bleData.voltage * bleData.dutyCycle },
  }
}
