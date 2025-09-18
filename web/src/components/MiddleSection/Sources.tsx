import { useSocketStore } from '../../store/socket'
import { LucideSmartphoneNfc } from 'lucide-react'
import { motion } from 'motion/react'

export const Sources = () => {
  const { sources, connect } = useSocketStore()

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0, transition: { delay: 0 } }}
      transition={{ delay: 0.5 }}
      className='w-full'
    >
      <div className='flex justify-between w-full py-6 border-b border-rose-200/50'>
        <div className='flex items-center justify-between w-full text-rose-100/80'>
          <span>Veri Kaynakları</span>
          <LucideSmartphoneNfc opacity={0.8} />
        </div>
      </div>

      {sources.map((source) => {
        return (
          <div className='flex justify-between w-full py-6 border-b last:border-none border-rose-200/20'>
            <div className='flex items-center justify-between w-full gap-4 text-rose-100/60'>
              <span className='font-light'>{source.name}</span>
              <span className='text-rose-800 cursor-pointer hover:text-rose-900/80' onClick={() => connect(source.id)}>
                Bağlan
              </span>
            </div>
          </div>
        )
      })}
    </motion.div>
  )
}
