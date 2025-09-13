import { AnimatePresence } from 'motion/react'
import { TEKLogo } from './Logo'
import { TripMeter } from './TripMeter'
import { useBLEStore } from '../../store/ble'
import { useSocketStore } from '../../store/socket'

export const MiddleSection = () => {
  const ble = useBLEStore()
  const socket = useSocketStore()
  const connected = ble.connected || !!socket.connectedSource

  return (
    <div className='flex flex-col relative items-center text-xl w-[360px]'>
      <AnimatePresence>{!connected && <TEKLogo />}</AnimatePresence>
      {connected && <TripMeter />}
    </div>
  )
}
