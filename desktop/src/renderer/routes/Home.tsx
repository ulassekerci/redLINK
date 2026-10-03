import { twMerge } from 'tailwind-merge'
import { Gauge } from '../components/Gauge'
import { useVehicleData } from '../hooks/useVehicleData'
import { MiddleSection } from '../components/MiddleSection'
import { useDimmed } from '../hooks/useDimmed'

export const HomeScreen = () => {
  const data = useVehicleData()
  const dimmed = useDimmed()
  const gaugeClass = twMerge('transition-opacity', dimmed && 'opacity-40')

  return (
    <>
      <div></div>
      <div className='flex items-center justify-between'>
        <div className={gaugeClass}>
          <Gauge value={data.speed_kmh} max={60} unit='km/h' left outerRing={{ value: data.duty_cycle, max: 1 }}>
            <span className='opacity-70 text-lg font-medium text-center'>{Math.round(data.distance_abs_m)} m</span>
          </Gauge>
        </div>
        <MiddleSection />
        <div className={gaugeClass}>
          <Gauge value={data.power_w} max={300} step={50} unit='watt' outerRing={{ value: data.adc_level1, max: 1 }}>
            <span className='opacity-70 text-lg font-medium text-center'>{Math.round(data.energy_used_wh)} Wh</span>
          </Gauge>
        </div>
      </div>
    </>
  )
}
