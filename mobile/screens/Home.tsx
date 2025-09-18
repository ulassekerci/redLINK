import { StatusBar } from 'expo-status-bar'
import { StyleSheet, View } from 'react-native'
import { SafeAreaView } from 'react-native-safe-area-context'
import { Text } from '../components/Text'
import { useVehicleData } from '../hooks/useVehicleData'
import colors from 'tailwindcss/colors'
import { DataRow } from '../components/DataRow'
import { ConnectButton } from '../components/ConnectButton'
import AsyncStorage from '@react-native-async-storage/async-storage'

export const HomeScreen = () => {
  const data = useVehicleData()

  return (
    <SafeAreaView style={styles.safe}>
      <Text style={styles.largeTitle} onPress={() => AsyncStorage.clear()}>
        redLINK
      </Text>
      <View style={styles.dataDisplay}>
        <DataRow name='Hız' data={data.speed} unit='km/h' />
        <DataRow name='Güç' data={data.power} unit='watt' />
        <DataRow name='Voltaj' data={data.voltage.battery} unit='V' precision={1} />
        <DataRow name='Mesafe' data={data.distance} unit='metre' />
        <DataRow name='Tüketim' data={data.wattHours.consumed} unit='Wh' />
        <DataRow name='Sıcaklık' data={data.temp.mosfet} unit='°C' last />
      </View>

      <ConnectButton />

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
    color: colors.rose[600],
  },
  dataDisplay: {
    width: '100%',
  },
})
