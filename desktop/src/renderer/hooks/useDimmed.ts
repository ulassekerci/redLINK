import { onDirectLink, useDirectLinkStore } from '../store/directLink'
import { useHubStore } from '../store/hub'
import { useVehicleStore } from '../store/vehicle'

// Whether the gauges and the bottom section show their last values dimmed:
// on a direct link while the board is not answering, and as a viewer whenever
// values are on show that the stream is no longer feeding.
export const useDimmed = () => {
  const phase = useDirectLinkStore((state) => state.phase)
  const live = useHubStore((state) => state.connection.state === 'live')
  const hasValues = useVehicleStore((state) => state.board !== null || state.adc !== null)

  if (onDirectLink(phase)) return phase === 'not_answering'
  return hasValues && !live
}
