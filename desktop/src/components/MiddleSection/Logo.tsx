import { motion } from 'motion/react'
import LogoSVG from '../../assets/tek.svg?react'

export const TEKLogo = () => {
  return (
    <motion.div
      className='absolute top-0 flex items-center justify-center h-full'
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      transition={{ delay: 0.5 }}
    >
      <LogoSVG className='w-40' />
    </motion.div>
  )
}
