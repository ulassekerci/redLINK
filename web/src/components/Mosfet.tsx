import { twMerge } from 'tailwind-merge'
import DRV8301 from '../assets/drv8301.svg?react'

export const Mosfet = ({ voltage, current, temp }: { voltage: number; current: number; temp: number }) => {
  return (
    <div className='flex justify-center items-center gap-[2px] w-80'>
      <div className='text-white text-lg font-medium flex items-center gap-3 ml-3 [font-variant-numeric:tabular-nums]'>
        <span className='w-14 text-right'>{current.toFixed(1)}A</span>
        <span>{voltage.toFixed(1)}V</span>
        <span className={twMerge(temp > 50 && 'text-yellow-400', temp > 60 && 'text-red-600')}>
          {Math.round(temp)}°C
        </span>
        <DRV8301 className='w-9 opacity-90' />
      </div>
    </div>
  )
}
