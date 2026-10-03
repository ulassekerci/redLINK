import { AnimatePresence } from 'motion/react'
import { TEKLogo } from './Logo'
import { TripMeter } from './TripMeter'
import { DeviceList } from './DeviceList'
import { StatusLine } from './StatusLine'
import { WaitingScreen } from './WaitingScreen'
import { onDirectLink, useDirectLinkStore } from '../../store/directLink'
import { useHubStore } from '../../store/hub'

const actionClass = 'cursor-pointer text-rose-100/80 hover:text-rose-500/80'

export const MiddleSection = () => {
  const { phase, stop } = useDirectLinkStore()
  const watching = useHubStore((state) => state.watching)
  const waiting = phase === 'off' && !watching

  // As a viewer the middle is the waiting screen until the stream is on the
  // dashboard, then the trip meter. A direct link is started from settings.
  return (
    <div className='flex flex-col relative items-center text-xl w-[360px]'>
      <AnimatePresence>{waiting && <TEKLogo />}</AnimatePresence>
      {waiting && <WaitingScreen />}
      {phase === 'off' && watching && (
        <>
          <StatusLine />
          <TripMeter />
        </>
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
