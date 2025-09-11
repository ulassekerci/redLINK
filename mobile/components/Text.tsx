import { Text as RNText, TextProps as RNTextProps, StyleSheet } from 'react-native'

// Same as react native text but this one is white by default

export const Text: React.FC<RNTextProps> = ({ style, ...props }) => {
  return <RNText style={[styles.default, style]} {...props} />
}

const styles = StyleSheet.create({
  default: {
    color: 'white',
  },
})
