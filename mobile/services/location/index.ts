import { createLocationTask } from './task'
import { startLocationUpdatesAsync, stopLocationUpdatesAsync } from 'expo-location'
import colors from 'tailwindcss/colors'
import { getLocationPermissions } from './permissions'

const startGPS = async () => {
  createLocationTask()
  await getLocationPermissions()
  await startLocationUpdatesAsync('updateGPS', {
    foregroundService: {
      notificationTitle: 'RedLINK',
      notificationBody: 'RedLINK arkaplanda çalışıyor',
      notificationColor: colors.rose[600],
    },
    activityType: 2,
    accuracy: 6,
  })
}

const stopGPS = () => stopLocationUpdatesAsync('updateGPS')

export const gps = {
  start: startGPS,
  stop: stopGPS,
}
