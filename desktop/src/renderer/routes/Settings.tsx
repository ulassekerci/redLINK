import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router'
import { onDirectLink, useDirectLinkStore } from '../store/directLink'

export const SettingsScreen = () => {
  const navigate = useNavigate()
  const [version, setVersion] = useState<string | null>(null)
  const { phase, start, stop } = useDirectLinkStore()

  // The device list is on the gauges. The scan starts here, in the click,
  // because Web Bluetooth needs a user gesture.
  const connectDirectly = () => {
    start()
    navigate('/')
  }

  // With no history, as after a reload on #/settings, Geri goes to the gauges.
  const goBack = () => (window.history.state?.idx > 0 ? navigate(-1) : navigate('/'))

  useEffect(() => {
    window.redlink.version().then(setVersion)
  }, [])

  return (
    <div className='flex flex-col gap-6 mx-18 text-xl'>
      <div className='flex justify-between items-center py-6 border-b border-rose-200/50'>
        <span className='text-rose-100/80'>Ayarlar</span>
        <span className='cursor-pointer text-rose-100/80 hover:text-rose-500/80' onClick={goBack}>
          Geri
        </span>
      </div>
      {onDirectLink(phase) ? (
        <span className='self-start cursor-pointer text-rose-100/80 hover:text-rose-500/80' onClick={stop}>
          Bağlantıyı kes
        </span>
      ) : (
        <span className='self-start cursor-pointer text-rose-100/80 hover:text-rose-500/80' onClick={connectDirectly}>
          Doğrudan bağlan
        </span>
      )}
      {version && <span className='font-light text-rose-100/60'>Sürüm {version}</span>}
    </div>
  )
}
