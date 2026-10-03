import { motion } from 'motion/react'
import { useVehicleData } from '../hooks/useVehicleData'
import Map, { Marker } from 'react-map-gl/maplibre'
import type { StyleSpecification } from 'maplibre-gl'
import mapStyle from '../assets/mapstyle.json'
import { onDirectLink, useDirectLinkStore } from '../store/directLink'

export const MapScreen = () => {
  const data = useVehicleData()
  // A direct link has no GPS; a fix left from before it is not shown.
  const directLink = useDirectLinkStore((state) => onDirectLink(state.phase))
  const gps = directLink ? null : data.gps

  const initialState = {
    latitude: gps?.gps_lat_deg,
    longitude: gps?.gps_lon_deg,
    zoom: 17.5,
  }

  return (
    <div className='flex items-center justify-between'>
      <motion.div
        initial={{ opacity: 0, scale: 0.25 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ type: 'spring', damping: 20, stiffness: 200 }}
        className='w-full mx-18 min-h-[80vh] border-4 border-neutral-800 rounded-4xl bg-neutral-950'
      >
        {gps ? (
          <Map
            initialViewState={initialState}
            style={{ width: '100%', height: '80vh', borderRadius: 32 }}
            mapStyle={mapStyle as StyleSpecification}
            attributionControl={false}
          >
            {gps && (
              <Marker latitude={gps.gps_lat_deg} longitude={gps.gps_lon_deg}>
                <div className='bg-rose-600 w-4 h-4 rounded-full' />
              </Marker>
            )}
          </Map>
        ) : (
          <div className='w-full h-[80vh] flex flex-col items-center justify-center'>Konum verisi yok</div>
        )}
      </motion.div>
    </div>
  )
}
