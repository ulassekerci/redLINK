import { motion } from 'motion/react'
import { useVehicleData } from '../hooks/useVehicleData'
import Map, { Marker } from 'react-map-gl/maplibre'

export const MapScreen = () => {
  const { location } = useVehicleData()

  const initialState = {
    latitude: location?.coords.latitude,
    longitude: location?.coords.longitude,
    zoom: 17.5,
  }

  return (
    <div className='flex items-center justify-between'>
      <motion.div
        initial={{ opacity: 0, x: -200 }}
        animate={{ opacity: 1, x: 0 }}
        transition={{ type: 'spring', damping: 20, stiffness: 200 }}
        className='w-96 text-center'
      >
        sol kısım
      </motion.div>
      <motion.div
        initial={{ opacity: 0, scale: 0.25 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ type: 'spring', damping: 20, stiffness: 200 }}
        className='w-full min-h-[80vh] border-4 border-neutral-800 rounded-4xl bg-neutral-950'
      >
        {initialState.latitude ? (
          <Map
            initialViewState={initialState}
            style={{ width: '100%', height: '80vh', borderRadius: 32 }}
            mapStyle={'/mapstyle.json'}
            attributionControl={false}
          >
            {location && (
              <Marker latitude={location?.coords.latitude} longitude={location.coords.longitude}>
                <div className='bg-rose-600 w-4 h-4 rounded-full' />
              </Marker>
            )}
          </Map>
        ) : (
          <div className='w-full h-[80vh] flex flex-col items-center justify-center'>Konum verisi yok</div>
        )}
      </motion.div>
      <motion.div
        initial={{ opacity: 0, x: 200 }}
        animate={{ opacity: 1, x: 0 }}
        transition={{ type: 'spring', damping: 20, stiffness: 200 }}
        className='w-96 text-center'
      >
        sağ kısım
      </motion.div>
    </div>
  )
}
