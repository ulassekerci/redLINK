import prompt from 'react-native-prompt-android'
import { useDeviceStore } from '../store/device'

export const askDeviceName = async () => {
  prompt(
    'Cihaz Adı',
    'Uzaktan bağlanabilmek için bu cihaza bir isim ver',
    [
      { text: 'İptal', style: 'cancel' },
      { text: 'Tamam', onPress: (newName) => useDeviceStore.getState().updateName(newName) },
    ],
    { type: 'plain-text', cancelable: false, defaultValue: `redLINK` }
  )
}
