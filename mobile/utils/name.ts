import Alert from '@blazejkustra/react-native-alert'
import { useDeviceStore } from '../store/device'

export const askDeviceName = async () => {
  Alert.prompt(
    'Cihaz Adı',
    'Uzaktan bağlanabilmek için bu cihaza bir isim ver',
    [
      {
        text: 'Tamam',
        onPress: (newName?: string) => {
          if (newName) useDeviceStore.getState().updateName(newName)
        },
      },
    ],
    'plain-text',
    `redLINK`,
  )
}
