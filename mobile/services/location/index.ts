import { startLocationUpdatesAsync, stopLocationUpdatesAsync } from 'expo-location'
import colors from 'tailwindcss/colors'

const startGPS = async () => {
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
