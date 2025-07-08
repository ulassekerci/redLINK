import { GaugeIcon, SplineIcon, TimerIcon, ZapIcon } from 'lucide-react'
import type { ReactNode } from 'react'
import { motion } from 'motion/react'

export const TripMeter = () => {
  return (
    <div className='flex flex-col items-center text-rose-100/90 text-xl w-80'>
      <TripRow icon={<SplineIcon />} text='Mesafe' value='122m' order={1} />
      <TripRow icon={<TimerIcon />} text='Süre' value='00:05:44' order={2} />
      <TripRow icon={<GaugeIcon />} text='Ort. Hız' value='12 km/h' order={3} />
      <TripRow icon={<ZapIcon />} text='Tüketim' value='832 km/kWh' order={4} />
    </div>
  )
}

const TripRow = ({ icon, text, value, order }: { icon: ReactNode; text: string; value: string; order: number }) => {
  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.5 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ delay: order * 0.1 + 1 }}
      className='flex justify-between w-full py-6 border-t first:border-none border-rose-200/20'
    >
      <div className='flex gap-4 text-rose-100/80'>
        <span className=''>{icon}</span>
        <span>{text}</span>
      </div>
      <span className='text-rose-100/50'>{value}</span>
    </motion.div>
  )
}
