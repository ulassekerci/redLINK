import { Gauge } from '../components/Gauge'
import { useVehicleData } from '../hooks/useVehicleData'
import { MiddleSection } from '../components/MiddleSection'

export const HomeScreen = () => {
  const data = useVehicleData()

  return (
    <div className='flex items-center justify-between'>
      <Gauge value={data.speed} max={60} unit='km/h' left outerRing={{ value: data.dutyCycle, max: 1 }}>
        <span className='opacity-70 text-lg font-medium text-center'>{Math.round(data.distance)} m</span>
      </Gauge>
      <MiddleSection />
      <Gauge value={data.power} max={300} step={50} unit='watt' outerRing={{ value: data.adc.level1, max: 1 }}>
        <span className='opacity-70 text-lg font-medium text-center'>{Math.round(data.wattHours.consumed)} Wh</span>
      </Gauge>
    </div>
  )
}
