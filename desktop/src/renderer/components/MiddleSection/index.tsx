import { AnimatePresence } from 'motion/react'
import { TEKLogo } from './Logo'
import { TripMeter } from './TripMeter'
import { DeviceList } from './DeviceList'
import { StatusLine } from './StatusLine'
import { onDirectLink, useDirectLinkStore } from '../../store/directLink'
import { useCallback, useEffect } from 'react'
import { downloadCSV } from '../../utils/csv'

const actionClass = 'cursor-pointer text-rose-100/80 hover:text-rose-500/80'

export const MiddleSection = () => {
  const { phase, start, stop } = useDirectLinkStore()

  const handleSave = useCallback((e: KeyboardEvent) => {
    if ((e.ctrlKey || e.metaKey) && e.key === 's') {
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

  // Until the waiting screen (ticket 30), the app off a direct link shows the
  // logo and the action that starts one.
  return (
    <div className='flex flex-col relative items-center text-xl w-[360px]'>
      <AnimatePresence>{phase === 'off' && <TEKLogo />}</AnimatePresence>
      {phase === 'off' && (
        <div className='absolute top-24 flex flex-col items-center gap-4'>
          <StatusLine />
          <button className={actionClass} onClick={start}>
            Doğrudan bağlan
          </button>
        </div>
      )}
      {phase === 'choosing' && (
        <>
          <StatusLine />
          <DeviceList />
        </>
      )}
      {onDirectLink(phase) && (
        <>
          <StatusLine />
          <TripMeter />
          <div className='flex justify-between w-full py-6 border-t border-white/20'>
            <span>Doğrudan bağlantı</span>
            <button className={actionClass} onClick={stop}>
              Bağlantıyı kes
            </button>
          </div>
        </>
      )}
    </div>
  )
}
