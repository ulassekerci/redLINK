import { useTripData } from '../../hooks/useTripData'
import { TripRow } from './TripRow'
import { GaugeIcon, SplineIcon, TimerIcon, ZapIcon } from 'lucide-react'

export const TripMeter = () => {
  const data = useTripData()

  return (
    <>
      <TripRow icon={<SplineIcon />} text='Mesafe' value={data.distanceString} order={0} />
      <TripRow icon={<TimerIcon />} text='Süre' value={data.timeString} order={1} />
      <TripRow icon={<GaugeIcon />} text='Ort. Hız' value={data.avgSpeedString} order={2} />
      <TripRow icon={<ZapIcon />} text='Tüketim' value={data.consumptionString} order={3} />
    </>
  )
}
