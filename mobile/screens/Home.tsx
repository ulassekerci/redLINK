import { StatusBar } from 'expo-status-bar'
import { Button, StyleSheet, View } from 'react-native'
import { socket } from '../services/socket'
import { SafeAreaView } from 'react-native-safe-area-context'
import { Text } from '../components/Text'
import { useNavigation } from '@react-navigation/native'
import { useBLEStore } from '../store/ble'
import { useVehicleData } from '../hooks/useVehicleData'
import { requestPermissions } from '../services/bluetooth/permissions'

export const HomeScreen = () => {
  const navigation = useNavigation()
  const ble = useBLEStore()
  const data = useVehicleData()

  return (
    <SafeAreaView style={styles.container}>
      <Text>redLINK</Text>
      <Button title='send' color='#ec003f' onPress={() => socket.emit('data', 'test')} />
      {!ble.connectedDevice && (
        <Button
          title='connect'
          color='#ec003f'
          onPress={async () => {
            await requestPermissions()
            navigation.navigate('Connect')
          }}
        />
      )}
      {ble.connectedDevice && (
        <View>
          <Text>speed: {Math.round(data.speed)} km/h</Text>
          <Text>power: {Math.round(data.power)} watts</Text>
          <Text>distance: {Math.round(data.distance)} m</Text>
          <Text>temperature: {Math.round(data.temp.mosfet)} °C</Text>
          <Text>consumption: {Math.round(data.wattHours.abs)} Wh</Text>
        </View>
      )}

      <StatusBar hidden={false} />
    </SafeAreaView>
  )
}

const styles = StyleSheet.create({
  container: {
    height: '100%',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 48,
  },
  devicesView: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
})
