import { motion } from 'motion/react'
import { useVehicleData } from '../hooks/useVehicleData'
import Map, { Marker } from 'react-map-gl/maplibre'
import { vehicleLog } from '../utils/csv'
import type { LocationData } from '../services/location/interfaces'

export const MapScreen = () => {
  const { location } = useVehicleData()

  // deduped array is needed as pastLocations array grows a lot
  const pastPoints: LocationData[] = []
  const logWithValidLocation = vehicleLog.filter((log) => log.data.location)
  const pastLocations = logWithValidLocation.map((log) => log.data.location) as LocationData[]
  pastLocations.forEach((location) => {
    const lastPoint = pastPoints.at(-1)
    if (JSON.stringify(location?.coords) === JSON.stringify(lastPoint?.coords)) return
    pastPoints.push(location!)
  })

  const initialState = {
    latitude: location?.coords.latitude ?? pastLocations.at(-1)?.coords.latitude,
    longitude: location?.coords.longitude ?? pastLocations.at(-1)?.coords.longitude,
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
        className='w-full border-4 border-neutral-800 rounded-4xl'
      >
        {initialState.latitude && (
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
            {pastPoints.map((location) => (
              <Marker latitude={location!.coords.latitude} longitude={location!.coords.longitude}>
                <div className='bg-rose-600 w-4 h-4 rounded-full' />
              </Marker>
            ))}
          </Map>
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
