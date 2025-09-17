import * as Location from 'expo-location'

export const getLocationPermissions = async () => {
  let { status: statusFG } = await Location.requestForegroundPermissionsAsync()
  let { status: statusBG } = await Location.requestBackgroundPermissionsAsync()

  if (statusFG !== 'granted') return { permitted: false, msg: 'Konum izni alınamadı' }
  if (statusBG !== 'granted') return { permitted: false, msg: 'Arkaplanda konum izni alınamadı' }
  return { permitted: true, msg: null }
}
