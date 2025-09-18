import * as Location from 'expo-location'
import { useDeviceStore } from '../../store/device'
import { Alert } from 'react-native'

export const getGPSPermisson = {
  fg: async () => {
    const { granted } = await Location.requestForegroundPermissionsAsync()
    useDeviceStore.getState().updateGPSPermission.fg(granted)
    if (!granted) Alert.alert('Konum izni alınamadı', 'Ayarlardan izin verebilirsiniz.', [{ text: 'Tamam' }])
  },
  bg: async () => {
    const { granted } = await Location.requestBackgroundPermissionsAsync()
    useDeviceStore.getState().updateGPSPermission.bg(granted)
    if (!granted) Alert.alert('Arkaplanda konum izni alınamadı', 'Ayarlardan izin verebilirsiniz.', [{ text: 'Tamam' }])
  },
}
