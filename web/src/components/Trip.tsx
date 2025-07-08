import { GaugeIcon, SplineIcon, TimerIcon, ZapIcon } from 'lucide-react'
import type { ReactNode } from 'react'

export const TripMeter = () => {
  return (
    <div className='flex flex-col items-center text-rose-100/90 text-xl w-80'>
      <TripRow icon={<SplineIcon />} text='Mesafe' value='122m' />
      <TripRow icon={<TimerIcon />} text='Süre' value='00:05:44' />
      <TripRow icon={<GaugeIcon />} text='Ort. Hız' value='12 km/h' />
      <TripRow icon={<ZapIcon />} text='Tüketim' value='832 km/kWh' />
    </div>
  )
}

const TripRow = ({ icon, text, value }: { icon: ReactNode; text: string; value: string }) => {
  return (
    <div className='flex justify-between w-full py-6 border-b last:border-none border-rose-200/20'>
      <div className='flex gap-4 text-rose-100/80'>
        <span className=''>{icon}</span>
        <span>{text}</span>
      </div>
      <span className='text-rose-100/50'>{value}</span>
    </div>
  )
}
