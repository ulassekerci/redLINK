import { motion } from 'motion/react'
import { Gauge } from './components/Gauge'
import { Battery } from './components/Battery'
import { Mosfet } from './components/Mosfet'
import { TripMeter } from './components/Trip'
import { useVehicleData } from './hooks/useVehicleData'
import { useEffect } from 'react'
import * as ble from './services/bluetooth'

function App() {
  const data = useVehicleData()

  const handleBLE = () => {
    if (!ble.isConnected()) return
    ble.requestValues()
  }

  useEffect(() => {
    const bleInt = setInterval(handleBLE, 50)
    return () => {
      clearInterval(bleInt)
    }
  }, [])

  return (
    <div className='flex flex-col justify-between h-full'>
      <div className=''></div>
      <div className='flex items-center justify-between'>
        <Gauge value={data.speed} max={60} unit='km/h' left outerRing={{ value: data.dutyCycle, max: 1 }}>
          <span className='opacity-70 text-lg font-medium text-center'>{data.distance}m</span>
        </Gauge>
        <TripMeter />
        {/* TODO: implement ADC reading */}
        <Gauge value={data.power} max={300} step={50} unit='watt' outerRing={{ value: 0, max: 75 }}>
          <span className='opacity-70 text-lg font-medium text-center'>{data.wattHours.consumed} Wh</span>
        </Gauge>
      </div>
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 0.5 }}
        className='flex justify-between items-center mx-9'
        onClick={ble.connectBLE}
      >
        <Battery voltage={data.voltage.battery} current={data.current.battery} />
        <Mosfet voltage={data.voltage.motor} current={data.current.motor} temp={data.temp.mosfet} />
      </motion.div>
    </div>
  )
}

export default App
