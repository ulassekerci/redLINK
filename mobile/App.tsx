import { SafeAreaProvider } from 'react-native-safe-area-context'
import { createStaticNavigation, DefaultTheme, StaticParamList, Theme } from '@react-navigation/native'
import { createNativeStackNavigator } from '@react-navigation/native-stack'
import { HomeScreen } from './screens/Home'
import { ConnectScreen } from './screens/Connect'
import colors from 'tailwindcss/colors'

const RootStack = createNativeStackNavigator({
  initialRouteName: 'Home',
  screens: {
    Home: {
      screen: HomeScreen,
      options: { headerShown: false },
    },
    Connect: {
      screen: ConnectScreen,
      options: {
        presentation: 'modal',
        title: 'Choose a device',
      },
    },
  },
})

const Navigation = createStaticNavigation(RootStack)
const redLinkDark: Theme = {
  ...DefaultTheme,
  colors: {
    primary: colors.rose[600],
    background: colors.black,
    card: colors.black,
    text: colors.white,
    border: colors.rose[600],
    notification: colors.rose[600],
  },
  dark: true,
}

export default function App() {
  return (
    <SafeAreaProvider>
      <Navigation theme={redLinkDark} />
    </SafeAreaProvider>
  )
}

type RootStackParamList = StaticParamList<typeof RootStack>

declare global {
  namespace ReactNavigation {
    interface RootParamList extends RootStackParamList {}
  }
}
