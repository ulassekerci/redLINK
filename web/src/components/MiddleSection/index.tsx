import { AnimatePresence } from 'motion/react'
import { TEKLogo } from './Logo'
import { TripMeter } from './TripMeter'
import { useBLEStore } from '../../store/ble'
import { useSocketStore } from '../../store/socket'
import { Sources } from './Sources'
import { useCallback, useEffect } from 'react'
import { downloadCSV } from '../../utils/csv'

export const MiddleSection = () => {
  const ble = useBLEStore()
  const socket = useSocketStore()
  const connected = ble.connected || !!socket.connectedSourceID

  const handleSave = useCallback((e: KeyboardEvent) => {
    if ((e.ctrlKey || e.metaKey) && e.key === 's') {
      console.log('tsest')
      e.preventDefault()
      downloadCSV()
    }
  }, [])

  useEffect(() => {
    addEventListener('keydown', handleSave)
    return () => {
      window.removeEventListener('keydown', handleSave)
    }
  }, [handleSave])

  return (
    <div className='flex flex-col relative items-center text-xl w-[360px]'>
      <AnimatePresence>{!connected && !socket.sources.length && <TEKLogo />}</AnimatePresence>
      {!connected && !!socket.sources.length && <Sources />}
      {connected && <TripMeter />}
    </div>
  )
}
