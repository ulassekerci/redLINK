import { directLinkStatus, useDirectLinkStore } from '../../store/directLink'

// One line at the top of the middle section; nothing when all is well.
export const StatusLine = () => {
  const text = useDirectLinkStore(directLinkStatus)
  if (!text) return null
  return <span className='w-full pb-6 text-center text-rose-100/80'>{text}</span>
}
