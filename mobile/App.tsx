import { StatusBar } from 'expo-status-bar'
import { Button, StyleSheet, Text, View } from 'react-native'
import { socket } from './services/socket'

export default function App() {
  return (
    <View style={styles.container}>
      <Text style={{ color: 'white' }}>Hello!</Text>
      <Button title='send' onPress={() => socket.emit('data', 'test')} />
      <StatusBar hidden={false} />
    </View>
  )
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#000',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 48,
  },
})
