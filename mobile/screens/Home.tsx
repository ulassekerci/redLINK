import { StatusBar } from 'expo-status-bar'
import { StyleSheet, View } from 'react-native'
import { SafeAreaView } from 'react-native-safe-area-context'
import { Text } from '../components/Text'
import { useVehicleData } from '../hooks/useVehicleData'
import colors from 'tailwindcss/colors'
import { DataRow } from '../components/DataRow'
import { ConnectButton } from '../components/ConnectButton'
import AsyncStorage from '@react-native-async-storage/async-storage'
import { useEffect } from 'react'
import { useBLEStore } from '../store/ble'
import { useDeviceStore } from '../store/device'
import { gps } from '../services/location'

export const HomeScreen = () => {
  const data = useVehicleData()
  const ble = useBLEStore()
  const { gpsPermissions } = useDeviceStore()

  useEffect(() => {
    const syncGPS = async () => {
      const shouldRunGPS =
        (ble.connection.state === 'connected' || ble.connection.state === 'reconnecting') && gpsPermissions.bg === true
      try {
        if (shouldRunGPS) await gps.start()
        else await gps.stop()
      } catch {
        // ignore errors
      }
    }
    syncGPS()
  }, [ble.connection.state, gpsPermissions.bg])

  useEffect(() => {
    return () => {
      gps.stop().catch(() => {})
    }
  }, [])

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
