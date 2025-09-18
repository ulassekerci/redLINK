import { motion } from 'motion/react'
import { useVehicleData } from '../hooks/useVehicleData'
import { Warning } from '../components/Warning'
import { NavLink } from 'react-router'
import Map from 'react-map-gl/maplibre'

export const MapScreen = () => {
  const data = useVehicleData()

  return (
    <div className='flex flex-col justify-between h-full'>
      <Warning code={data.faultCode} />
      <div className='flex items-center justify-between'>
        <div className='w-96 text-center'>sol kısım</div>
        <Map
          initialViewState={{
            latitude: 39.891497,
            longitude: 32.780574,
            zoom: 17.5,
          }}
          style={{ width: '100%', height: '80vh', borderRadius: 24 }}
          mapStyle={'/mapstyle.json'}
          attributionControl={false}
        />
        <div className='w-96 text-center'>sağ kısım</div>
      </div>
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 0.5 }}
        className='flex justify-between items-center mx-9'
        onClick={() => {}}
      >
        <NavLink to='/'>alttaki çubuk</NavLink>
      </motion.div>
    </div>
  )
}
