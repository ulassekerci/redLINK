import * as Location from 'expo-location'
import colors from 'tailwindcss/colors'

const startGPS = async () => {
  if (await Location.hasStartedLocationUpdatesAsync('updateGPS')) {
    return
  }

  try {
    await Location.startLocationUpdatesAsync('updateGPS', {
      foregroundService: {
        notificationTitle: 'RedLINK',
        notificationBody: 'RedLINK arkaplanda çalışıyor',
        notificationColor: colors.rose[600],
      },
      activityType: 2,
      accuracy: 6,
    })
  } catch (e) {
    throw e
  }
}

const stopGPS = async () => {
  if (!(await Location.hasStartedLocationUpdatesAsync('updateGPS'))) {
    return
  }

  await Location.stopLocationUpdatesAsync('updateGPS')
}

export const gps = {
  start: startGPS,
  stop: stopGPS,
}
