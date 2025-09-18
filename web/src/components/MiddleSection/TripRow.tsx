import type { ReactNode } from 'react'
import { motion } from 'motion/react'

export const TripRow = ({
  icon,
  text,
  value,
  order,
}: {
  icon: ReactNode
  text: string
  value: string
  order: number
}) => {
  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.5 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ delay: order * 0.1 + 0.25 }}
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
