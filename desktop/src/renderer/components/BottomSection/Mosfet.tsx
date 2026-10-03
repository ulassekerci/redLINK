import { twMerge } from 'tailwind-merge'
import DRV8301 from '../../assets/drv8301.svg?react'
import { useVehicleData } from '../../hooks/useVehicleData'

export const Mosfet = () => {
  const { motor_current_a, motor_voltage_v, mosfet_temp_c } = useVehicleData()

  return (
    <div className='flex justify-center items-center gap-[2px] w-80'>
      <div className='text-lg font-medium flex items-center gap-3 ml-3 [font-variant-numeric:tabular-nums]'>
        <span className='w-14 text-right'>{motor_current_a.toFixed(1)}A</span>
        <span>{motor_voltage_v.toFixed(1)}V</span>
        <span className={twMerge(mosfet_temp_c > 50 && 'text-yellow-400', mosfet_temp_c > 60 && 'text-red-600')}>
          {mosfet_temp_c.toFixed(0)}°C
        </span>
        <DRV8301 />
      </div>
    </div>
  )
}
