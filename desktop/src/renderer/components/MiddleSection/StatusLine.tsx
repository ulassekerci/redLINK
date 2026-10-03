import { directLinkStatus, onDirectLink, useDirectLinkStore } from '../../store/directLink'
import { useHubStore, viewerStatus } from '../../store/hub'

// One line at the top of the middle section; nothing when all is well. Off a
// direct link it is the viewer's state, unless Bluetooth has just failed.
export const StatusLine = () => {
  const directLink = useDirectLinkStore((state) => onDirectLink(state.phase))
  const aroundDirectLink = useDirectLinkStore(directLinkStatus)
  const asViewer = useHubStore(viewerStatus)

  const text = aroundDirectLink ?? (directLink ? null : asViewer)
  if (!text) return null
  return <span className='w-full pb-6 text-center text-balance text-rose-100/80'>{text}</span>
}
