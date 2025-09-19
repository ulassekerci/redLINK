import { motion } from 'motion/react'
import { Battery } from './Battery'
import { NavLink, useLocation } from 'react-router'
import { Mosfet } from './Mosfet'
import { useBLEStore } from '../../store/ble'

export const BottomSection = () => {
  const ble = useBLEStore()
  let location = useLocation()
  console.log(location)

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ delay: 0.5 }}
      className='flex justify-between items-center mx-9'
    >
      <Battery />
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

        {ble.connected ? (
          <span
            className='text-center rounded-xl cursor-pointer text-rose-100/80 hover:text-rose-500/80'
            onClick={ble.disconnectBLE}
          >
            Soket
          </span>
        ) : (
          <span
            className='text-center rounded-xl cursor-pointer text-rose-100/80 hover:text-rose-500/80'
            onClick={ble.connectBLE}
          >
            Bluetooth
          </span>
        )}
      </div>
      <Mosfet />
    </motion.div>
  )
}
