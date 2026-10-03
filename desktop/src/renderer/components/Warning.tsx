import { LucideTriangleAlert } from 'lucide-react'
import { AnimatePresence, motion } from 'motion/react'
import { useVehicleData } from '../hooks/useVehicleData'
import { faultName } from '../utils/faults'

export const Warning = () => {
  const { fault_code } = useVehicleData()

  return (
    <AnimatePresence>
      {fault_code !== 0 && (
        <motion.div
          initial={{ y: -200 }}
          animate={{ y: 0 }}
          exit={{ y: -200 }}
          className='absolute left-1/2 transform -translate-x-1/2 flex gap-4 items-center justify-between p-3 min-w-[360px] h-24 bg-gradient-to-tr from-rose-600/30 to-rose-600/50 rounded-2xl'
        >
          <div className='flex flex-col justify-center gap-2'>
            <span className='text-xl font-medium'>Uyarı</span>
            <span>{faultName(fault_code)}</span>
          </div>
          <LucideTriangleAlert className='text-rose-500/80' size={48} />
        </motion.div>
      )}
    </AnimatePresence>
  )
}

