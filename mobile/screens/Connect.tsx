import { SafeAreaView } from 'react-native-safe-area-context'
import { Text } from '../components/Text'

import { Button, FlatList, StyleSheet, View } from 'react-native'
import { useEffect } from 'react'
import colors from 'tailwindcss/colors'
import { useNavigation } from '@react-navigation/native'
import { useBLEStore } from '../store/ble'

export const ConnectScreen = () => {
  const ble = useBLEStore()
  const navigation = useNavigation()

  const connect = (id: string) => {
    ble.stopScan()
    ble.connect(id)
    navigation.goBack()
  }

  useEffect(() => {
    ble.scan()
    return () => {
      ble.stopScan()
    }
  }, [])

  return (
    <SafeAreaView style={{ paddingTop: 24 }}>
      <FlatList
        data={ble.devices}
        keyExtractor={(item) => item.id}
        renderItem={(device) => (
          <View style={styles.deviceItem}>
            <Text>{device.item.name}</Text>
            <Button title='Connect' color={colors.rose[600]} onPress={() => connect(device.item.id)} />
          </View>
        )}
      />
    </SafeAreaView>
  )
}

const styles = StyleSheet.create({
  deviceItem: {
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 8,
    paddingHorizontal: 12,
    marginHorizontal: 24,
    justifyContent: 'space-between',
    backgroundColor: colors.zinc[900],
    borderRadius: 8,
  },
})
