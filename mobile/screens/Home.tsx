import { StatusBar } from 'expo-status-bar'
import { StyleSheet, View } from 'react-native'
import { SafeAreaView } from 'react-native-safe-area-context'
import { Text } from '../components/Text'
import { useNavigation } from '@react-navigation/native'
import { useBLEStore } from '../store/ble'
import { useVehicleData } from '../hooks/useVehicleData'
import { requestPermissions } from '../services/bluetooth/permissions'
import colors from 'tailwindcss/colors'
import { Button } from '../components/Button'
import { DataRow } from '../components/DataRow'

export const HomeScreen = () => {
  const navigation = useNavigation()
  const ble = useBLEStore()
  const data = useVehicleData()

  return (
    <SafeAreaView style={styles.safe}>
      <Text style={styles.largeTitle}>redLINK</Text>
      <View style={styles.dataDisplay}>
        <DataRow name='Hız' data={data.speed} unit='km/h' />
        <DataRow name='Güç' data={data.power} unit='watt' />
        <DataRow name='Voltaj' data={data.voltage.battery} unit='V' precision={1} />
        <DataRow name='Mesafe' data={data.distance} unit='metre' />
        <DataRow name='Tüketim' data={data.wattHours.consumed} unit='Wh' />
        <DataRow name='Sıcaklık' data={data.temp.mosfet} unit='°C' last />
      </View>

      <View>
        {ble.connectedDevice ? (
          <Button title='Bağlantıyı Kes' onPress={ble.disconnect} />
        ) : (
          <Button
            title='Bluetooth ile bağlan'
            color='#ec003f'
            onPress={async () => {
              await requestPermissions()
              navigation.navigate('Connect')
            }}
          />
        )}
      </View>

      <StatusBar hidden={false} />
    </SafeAreaView>
  )
}

const styles = StyleSheet.create({
  safe: {
    flex: 1,
    alignItems: 'center',
    padding: 24,
    gap: 48,
    display: 'flex',
    justifyContent: 'space-between',
  },
  largeTitle: {
    fontSize: 36,
    fontWeight: 'bold',
    color: colors.rose[700],
  },
  dataDisplay: {
    width: '100%',
  },
})
