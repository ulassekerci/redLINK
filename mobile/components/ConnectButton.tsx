import { useNavigation } from '@react-navigation/native'
import { useBLEStore } from '../store/ble'
import { Button } from './Button'
import { requestBLEPermissions } from '../services/bluetooth/permissions'
import { useDeviceStore } from '../store/device'
import { getGPSPermisson } from '../services/location/permissions'
import { askDeviceName } from '../utils/name'

export const ConnectButton = () => {
  const navigation = useNavigation()
  const ble = useBLEStore()
  const { deviceName, gpsPermissions } = useDeviceStore()

  if (ble.connectedDevice) {
    return <Button title='Bağlantıyı Kes' onPress={ble.disconnect} />
  }

  if (!deviceName) {
    return <Button title='Cihaz Adı Gir' onPress={askDeviceName} />
  }
  if (gpsPermissions?.fg === null) {
    return <Button title='Konum izni ver' onPress={() => getGPSPermisson.fg()} />
  }
  if (gpsPermissions.bg === null && gpsPermissions.fg !== false) {
    return <Button title='Arkaplan konum izni ver' onPress={() => getGPSPermisson.bg()} />
  }

  return (
    <Button
      title='Bağlan'
      onPress={async () => {
        await requestBLEPermissions()
        navigation.navigate('Connect')
      }}
    />
  )
}
