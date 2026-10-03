import { useVehicleStore } from '../store/vehicle'

// What the dashboard shows. A field not received yet, or missing from a short
// reply, reads zero here; the store keeps it absent.
export const useVehicleData = () => {
  const { board, adc, gps } = useVehicleStore()

  const batteryVoltage = board?.battery_voltage_v ?? 0
  const batteryCurrent = board?.battery_current_a ?? 0
  const dutyCycle = board?.duty_cycle ?? 0
  const energyUsed = board?.energy_used_wh ?? 0
  const energyCharged = board?.energy_charged_wh ?? 0

  return {
    speed_kmh: (board?.speed_m_s ?? 0) * 3.6,
    power_w: batteryVoltage * batteryCurrent,
    motor_voltage_v: batteryVoltage * dutyCycle,
    net_energy_wh: energyUsed - energyCharged,
    duty_cycle: dutyCycle,
    distance_abs_m: board?.distance_abs_m ?? 0,
    energy_used_wh: energyUsed,
    energy_charged_wh: energyCharged,
    battery_voltage_v: batteryVoltage,
    battery_current_a: batteryCurrent,
    motor_current_a: board?.motor_current_a ?? 0,
    mosfet_temp_c: board?.mosfet_temp_c ?? 0,
    fault_code: board?.fault_code ?? 0,
    adc_level1: adc?.adc_level1 ?? 0,
    gps,
  }
}
