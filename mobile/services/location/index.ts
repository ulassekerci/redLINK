import { createLocationTask } from './task'
import { getLocationPermissions } from './permissions'
import { startLocationUpdatesAsync, stopLocationUpdatesAsync } from 'expo-location'
import colors from 'tailwindcss/colors'

const startGPS = async () => {
  const permissionState = await getLocationPermissions()
  if (!permissionState.permitted) return console.log(permissionState.msg)

  createLocationTask()
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

const stopGPS = async () => {
  await stopLocationUpdatesAsync('updateGPS')
}

export const gps = {
  start: startGPS,
  stop: stopGPS,
}
