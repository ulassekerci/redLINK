import { useDirectLinkStore } from '../../store/directLink'

// The boards a scan has found, added as it finds them. The person picks one;
// nothing is picked automatically, even with one entry.
export const DeviceList = () => {
  const { devices, pick, cancel } = useDirectLinkStore()

  return (
    <div className='flex flex-col w-full'>
      <span className='pb-6 text-rose-100/80'>Araç seçin</span>
      {devices.length === 0 && <span className='py-6 border-t border-white/20 text-gray-300/60'>Araç aranıyor</span>}
      {devices.map((device) => (
        <button
          key={device.id}
          className='py-6 text-left border-t border-white/20 cursor-pointer hover:text-rose-500/80 truncate'
          onClick={() => pick(device.id)}
        >
          {device.name || device.id}
        </button>
      ))}
      <button
        className='py-6 text-left border-t border-white/20 cursor-pointer text-rose-100/80 hover:text-rose-500/80'
        onClick={cancel}
      >
        Vazgeç
      </button>
    </div>
  )
}
