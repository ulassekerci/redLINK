import { motion } from 'motion/react'
import { Gauge } from './components/Gauge'
import { Battery } from './components/Battery'
import { Mosfet } from './components/Mosfet'
import { useEffect, useRef, useState } from 'react'
import { TripMeter } from './components/Trip'

function App() {
  const [testValue, setTestValue] = useState(0)
  const direction = useRef(1)

  const update = () => {
    setTestValue((old) => {
      if (old > 60) direction.current = -1
      if (old < 10) direction.current = +1
      return old + direction.current
    })
  }

  // useEffect(() => {
  //   const int = setInterval(update, 20)
  //   return () => {
  //     clearInterval(int)
  //   }
  // }, [])

  return (
    <div className='flex flex-col justify-between h-full'>
      <div className=''></div>
      <div className='flex items-center justify-between'>
        <Gauge value={testValue} max={60} unit='km/h' left outerRing={{ value: testValue, max: 75 }}>
          <span className='text-rose-100/80 text-lg font-medium text-center'>{testValue * 6}m</span>
        </Gauge>
        <TripMeter />
        <Gauge value={testValue * 4} max={300} step={50} unit='watt' outerRing={{ value: testValue, max: 75 }}>
          <span className='text-rose-100/80 text-lg font-medium text-center'>{testValue} Wh</span>
        </Gauge>
      </div>
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 0.5 }}
        className='flex justify-between items-center mx-9'
      >
        <Battery voltage={10 + testValue} current={testValue} />
        <Mosfet voltage={10 + testValue} current={testValue} temp={25 + testValue} />
      </motion.div>
    </div>
  )
}

export default App
