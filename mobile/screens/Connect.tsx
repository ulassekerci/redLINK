import { SafeAreaView } from 'react-native-safe-area-context'
import { Text } from '../components/Text'
import { ActivityIndicator, FlatList, StyleSheet, View } from 'react-native'
import { useEffect } from 'react'
import colors from 'tailwindcss/colors'
import { useNavigation } from '@react-navigation/native'
import { useBLEStore } from '../store/ble'
import * as Haptics from 'expo-haptics'
import { Button } from '../components/Button'

export const ConnectScreen = () => {
  const ble = useBLEStore()
  const navigation = useNavigation()

  const connect = (id: string) => {
    ble.scanner.stop()
    ble.connect(id)
    Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success)
    navigation.goBack()
  }

  useEffect(() => {
    ble.scanner.start()
    return () => {
      ble.scanner.stop()
    }
  }, [])

  return (
    <SafeAreaView style={{ paddingTop: 24 }}>
      <View style={styles.scanIndicator}>
        <Text style={{ opacity: 0.5 }}>Cihazlar</Text>
        {ble.scanner.isScanning && <ActivityIndicator />}
      </View>

      <FlatList
        data={[...ble.scanner.devices.values()]}
        keyExtractor={(item) => item.id + Math.random()}
        style={styles.deviceList}
        renderItem={(device) => {
          const isLast = device.index === ble.scanner.devices.size - 1
          return (
            <View style={{ ...styles.deviceItem, borderBottomWidth: isLast ? 0 : 0.5 }}>
              <Text>{device.item.name}</Text>
              <Button title='Bağlan' onPress={() => connect(device.item.id)} />
            </View>
          )
        }}
      />
    </SafeAreaView>
  )
}

const styles = StyleSheet.create({
  scanIndicator: {
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginLeft: 12,
  },
  deviceList: {
    backgroundColor: colors.neutral[900],
    marginTop: 16,
    marginHorizontal: 12,
    borderRadius: 10,
  },
  deviceItem: {
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 12,
    paddingHorizontal: 16,
    justifyContent: 'space-between',
  },
})
