import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router'

export const SettingsScreen = () => {
  const navigate = useNavigate()
  const [version, setVersion] = useState<string | null>(null)

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
      {version && <span className='font-light text-rose-100/60'>Sürüm {version}</span>}
    </div>
  )
}
