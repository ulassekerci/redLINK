import { getAspilsanPercentage } from '../utils/aspilsan'

export const Battery = ({ voltage, current }: { voltage: number; current: number }) => {
  const percentage = getAspilsanPercentage(voltage / 12)
  const batteryColor = percentage > 20 ? 'rgba(255,241,242,0.9)' : percentage > 10 ? '#fcc800' : '#e7000b'

  return (
    <div className='flex justify-center items-center gap-[2px] w-80'>
      <div className='flex items-center w-12 h-6 border-1 border-white/50 rounded-md'>
        <div
          className='h-[19px] w-11 rounded mx-px'
          style={{
            background: `linear-gradient(90deg,${batteryColor} 0%,${batteryColor} ${percentage}%,transparent ${percentage}.1%)`,
          }}
        />
      </div>
      <div className='w-1 h-2 bg-white/50 rounded-r-full' />
      <div className='text-lg font-medium flex gap-3 ml-3 [font-variant-numeric:tabular-nums]'>
        <span className='text-right'>{percentage}%</span>
        <span>{voltage.toFixed(1)}V</span>
        <span className='w-9'>{current.toFixed(1)}A</span>
      </div>
    </div>
  )
}
