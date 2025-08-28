import { AnimatePresence } from 'motion/react'
// import { useEffect, useState } from 'react'
import { TEKLogo } from './Logo'
import { TripMeter } from './TripMeter'
import { useBLEStore } from '../../store/ble'

export const MiddleSection = () => {
  const ble = useBLEStore()
  // const [showLogo, setShowLogo] = useState(true)

  // useEffect(() => {
  //   const logoTimer = setTimeout(() => setShowLogo(false), 1500)
  //   return () => {
  //     clearTimeout(logoTimer)
  //   }
  // }, [])

  return (
    <div className='flex flex-col relative items-center text-xl w-[360px]'>
      <AnimatePresence>{!ble.connected && <TEKLogo />}</AnimatePresence>
      {ble.connected && <TripMeter />}
    </div>
  )
}
