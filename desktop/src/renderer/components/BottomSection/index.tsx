import { motion } from 'motion/react'
import { Battery } from './Battery'
import { NavLink, useLocation } from 'react-router'
import { Mosfet } from './Mosfet'
import { twMerge } from 'tailwind-merge'
import { useDimmed } from '../../store/directLink'

export const BottomSection = () => {
  const location = useLocation()
  const dimmed = useDimmed()
  const valuesClass = twMerge('transition-opacity', dimmed && 'opacity-40')

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ delay: 0.5 }}
      className='flex justify-between items-center mx-9'
    >
      <div className={valuesClass}>
        <Battery />
      </div>
      <div className='flex gap-4'>
        {location.pathname === '/map' ? (
          <NavLink to='/' className='flex gap-4'>
            <span className='text-center rounded-xl cursor-pointer text-rose-100/80 hover:text-rose-500/80'>
              Göstergeler
            </span>
          </NavLink>
        ) : (
          <NavLink to='/map' className='flex gap-4'>
            <span className='text-center rounded-xl cursor-pointer text-rose-100/80 hover:text-rose-500/80'>
              Harita
            </span>
          </NavLink>
        )}

        <NavLink to='/settings' className='flex gap-4'>
          <span className='text-center rounded-xl cursor-pointer text-rose-100/80 hover:text-rose-500/80'>Ayarlar</span>
        </NavLink>
      </div>
      <div className={valuesClass}>
        <Mosfet />
      </div>
    </motion.div>
  )
}
