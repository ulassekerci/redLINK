import { GaugeIcon, SplineIcon, TimerIcon, ZapIcon } from 'lucide-react'
import type { ReactNode } from 'react'
import { motion } from 'motion/react'
import { useTripData } from '../hooks/useTripData'

export const TripMeter = () => {
  const data = useTripData()

  return (
    <div className='flex flex-col items-center text-xl w-[360px]'>
      <TripRow icon={<SplineIcon />} text='Mesafe' value={data.distanceString} order={0} />
      <TripRow icon={<TimerIcon />} text='Süre' value={data.timeString} order={1} />
      <TripRow icon={<GaugeIcon />} text='Ort. Hız' value={data.avgSpeedString} order={2} />
      <TripRow icon={<ZapIcon />} text='Tüketim' value={data.consumptionString} order={3} />
    </div>
  )
}

const TripRow = ({ icon, text, value, order }: { icon: ReactNode; text: string; value: string; order: number }) => {
  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.5 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ delay: order * 0.1 + 1 }}
      className='flex justify-between w-full py-6 border-t first:border-none border-white/20'
    >
      <div className='flex gap-4'>
        {icon}
        <span>{text}</span>
      </div>
      <span className='text-gray-300/60 [font-variant-numeric:tabular-nums]'>{value}</span>
    </motion.div>
  )
}
