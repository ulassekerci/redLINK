import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router'
import { twMerge } from 'tailwind-merge'
import type { Settings, TypedSettings } from '../../preload/api'
import { formatTeamCode } from '../../protocol'
import { onDirectLink, useDirectLinkStore } from '../store/directLink'
import { saveSettings } from '../store/hub'

const actionClass = 'self-start cursor-pointer text-rose-100/80 hover:text-rose-500/80'
const fieldClass =
  'w-80 py-2 bg-transparent border-b border-white/20 outline-none text-rose-100/90 focus:border-rose-500/80'

// The three fields as the settings screen shows stored settings.
const asTyped = (settings: Settings): TypedSettings => ({
  teamCode: settings.teamCode ? formatTeamCode(settings.teamCode) : '',
  hubHost: settings.hubHost,
  hubPort: String(settings.hubPort),
})

export const SettingsScreen = () => {
  const navigate = useNavigate()
  const [version, setVersion] = useState<string | null>(null)
  const [stored, setStored] = useState<TypedSettings | null>(null)
  const [typed, setTyped] = useState<TypedSettings | null>(null)
  const [rejected, setRejected] = useState(false)
  const { phase, start, stop } = useDirectLinkStore()

  // The device list is on the gauges. The scan starts here, in the click,
  // because Web Bluetooth needs a user gesture.
  const connectDirectly = () => {
    start()
    navigate('/')
  }

  // With no history, as on a first launch or after a reload on #/settings,
  // Geri goes to the gauges.
  const goBack = () => (window.history.state?.idx > 0 ? navigate(-1) : navigate('/'))

  const show = (settings: Settings) => {
    setStored(asTyped(settings))
    setTyped(asTyped(settings))
  }

  useEffect(() => {
    window.redlink.version().then(setVersion)
    window.redlink.readSettings().then(show)
  }, [])

  const setField = (field: keyof TypedSettings, value: string) => {
    setTyped((typed) => typed && { ...typed, [field]: value })
    if (field === 'teamCode') setRejected(false)
  }

  // Kaydet is dimmed while the fields hold what is stored.
  const changed =
    typed !== null &&
    stored !== null &&
    (Object.keys(typed) as (keyof TypedSettings)[]).some((key) => typed[key] !== stored[key])

  const save = async (event: FormEvent) => {
    event.preventDefault()
    if (!typed || !changed) return
    const saved = await saveSettings(typed)
    setRejected(!saved)
    if (saved) show(saved)
  }

  return (
    <div className='flex flex-col gap-6 mx-18 text-xl'>
      <div className='flex justify-between items-center py-6 border-b border-rose-200/50'>
        <span className='text-rose-100/80'>Ayarlar</span>
        <span className='cursor-pointer text-rose-100/80 hover:text-rose-500/80' onClick={goBack}>
          Geri
        </span>
      </div>
      {typed && (
        <form className='flex flex-col gap-6 pb-6 border-b border-white/20' onSubmit={save}>
          <div className='grid grid-cols-[10rem_20rem] items-center gap-y-4 self-start'>
            <label htmlFor='teamCode'>Takım kodu</label>
            <input
              id='teamCode'
              className={twMerge(fieldClass, 'uppercase tracking-widest')}
              value={typed.teamCode}
              onChange={(e) => setField('teamCode', e.target.value)}
              autoFocus={!typed.teamCode}
              autoComplete='off'
              spellCheck={false}
            />
            <label htmlFor='hubHost'>Hub adresi</label>
            <input
              id='hubHost'
              className={fieldClass}
              value={typed.hubHost}
              onChange={(e) => setField('hubHost', e.target.value)}
              autoComplete='off'
              spellCheck={false}
            />
            <label htmlFor='hubPort'>Port</label>
            <input
              id='hubPort'
              className={fieldClass}
              value={typed.hubPort}
              onChange={(e) => setField('hubPort', e.target.value.replace(/\D/g, ''))}
              inputMode='numeric'
              maxLength={5}
              autoComplete='off'
            />
          </div>
          {rejected && <span className='text-rose-500'>Takım kodu hatalı. Telefondaki kodu kontrol edin.</span>}
          <button
            type='submit'
            disabled={!changed}
            className={twMerge(actionClass, 'disabled:cursor-default disabled:text-rose-100/30')}
          >
            Kaydet
          </button>
        </form>
      )}
      {onDirectLink(phase) ? (
        <span className={actionClass} onClick={stop}>
          Bağlantıyı kes
        </span>
      ) : (
        <span className={actionClass} onClick={connectDirectly}>
          Doğrudan bağlan
        </span>
      )}
      {version && <span className='font-light text-rose-100/60'>Sürüm {version}</span>}
    </div>
  )
}
