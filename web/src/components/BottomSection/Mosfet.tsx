import { twMerge } from 'tailwind-merge'
import DRV8301 from '../../assets/drv8301.svg?react'
import { useVehicleData } from '../../hooks/useVehicleData'

export const Mosfet = () => {
  const { voltage, current, temp } = useVehicleData()

  return (
    <div className='flex justify-center items-center gap-[2px] w-80'>
      <div className='text-lg font-medium flex items-center gap-3 ml-3 [font-variant-numeric:tabular-nums]'>
        <span className='w-14 text-right'>{current.motor.toFixed(1)}A</span>
        <span>{voltage.motor.toFixed(1)}V</span>
        <span className={twMerge(temp.mosfet > 50 && 'text-yellow-400', temp.mosfet > 60 && 'text-red-600')}>
          {temp.mosfet.toFixed(0)}°C
        </span>
        <DRV8301 />
      </div>
    </div>
  )
}
